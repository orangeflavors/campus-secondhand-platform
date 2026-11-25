package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.PageUtils;
import com.campus.trade.common.TextUtils;
import com.campus.trade.common.TradeConst;
import com.campus.trade.dto.ItemDtos;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Item;
import com.campus.trade.entity.ItemImage;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ItemImageMapper;
import com.campus.trade.mapper.ItemMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.AdminItemVO;
import com.campus.trade.vo.FavoriteStateVO;
import com.campus.trade.vo.ItemCardVO;
import com.campus.trade.vo.ItemDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 商品发布、审核、搜索与下架（FR-005 ~ FR-013 / ST-I01 ~ ST-I10）。
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final String RISK_NOTICE =
            "平台不代管资金。请在校内公共区域当面验货、当面付款并完成实物交付，不要向陌生人提前转账。";
    private static final Set<String> SORTS = Set.of("latest", "price_asc", "price_desc");
    private static final Set<String> EDITABLE = Set.of(
            TradeConst.ITEM_DRAFT, TradeConst.ITEM_REJECTED, TradeConst.ITEM_OFF_SHELF, TradeConst.ITEM_ON_SALE);

    private final ItemMapper itemMapper;
    private final ItemImageMapper itemImageMapper;
    private final FavoriteMapper favoriteMapper;
    private final CategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final OrderService orderService;

    @Value("${app.order.max-pending-per-item:10}")
    private int maxPendingPerItem;

    /* ------------------------------ 搜索与详情 ------------------------------ */

    public PageResult<ItemCardVO> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                         String condition, String area, String sort, Integer page, Integer size) {
        String normalizedKeyword = TextUtils.isBlank(keyword) ? null : TextUtils.escapeLike(keyword.trim());
        String normalizedSort = TextUtils.isBlank(sort) ? "latest" : sort;
        if (!SORTS.contains(normalizedSort)) {
            throw BizException.badRequest("排序方式不合法");
        }
        if (!TextUtils.isBlank(condition) && !TradeConst.ITEM_CONDITIONS.contains(condition)) {
            throw BizException.badRequest("成色筛选值不合法");
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw BizException.badRequest("价格区间不能为负数");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw BizException.badRequest("最低价不能高于最高价");
        }
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 12);
        List<ItemCardVO> records = itemMapper.search(normalizedKeyword, categoryId, minPrice, maxPrice,
                TextUtils.isBlank(condition) ? null : condition,
                TextUtils.isBlank(area) ? null : area.trim(),
                normalizedSort, PageUtils.offset(p, s), s);
        records.forEach(this::fillTexts);
        long total = itemMapper.countSearch(normalizedKeyword, categoryId, minPrice, maxPrice,
                TextUtils.isBlank(condition) ? null : condition,
                TextUtils.isBlank(area) ? null : area.trim());
        return PageResult.of(records, total, p, s);
    }

    public ItemDetailVO detail(Long itemId, Long viewerId, boolean admin) {
        Item item = requireItem(itemId);
        boolean owner = item.getSellerId().equals(viewerId);
        if (!isPublicStatus(item.getStatus()) && !owner && !admin) {
            throw BizException.notFound("商品不存在或尚未公开");
        }
        itemMapper.increaseViewCount(itemId);
        return buildDetail(item, viewerId, admin);
    }

    public ItemDetailVO buildDetail(Item item, Long viewerId, boolean admin) {
        ItemCardVO card = itemMapper.findCardById(item.getId());
        if (card == null) {
            throw BizException.notFound("商品不存在");
        }
        ItemDetailVO vo = new ItemDetailVO();
        BeanUtils.copyProperties(card, vo);
        vo.setConditionText(TradeConst.conditionText(item.getItemCondition()));
        vo.setStatusText(TradeConst.itemStatusText(item.getStatus()));
        vo.setDescription(item.getDescription());

        List<ItemImage> images = itemImageMapper.findByItemId(item.getId());
        vo.setImages(images.stream().map(ItemImage::getUrl).toList());
        vo.setSeller(userService.publicProfile(item.getSellerId(), viewerId));

        boolean owner = viewerId != null && item.getSellerId().equals(viewerId);
        vo.setIsOwner(owner);
        vo.setIsFavorite(viewerId != null && favoriteMapper.exists(viewerId, item.getId()) > 0);
        if (owner || admin) {
            vo.setRejectReason(item.getRejectReason());
            TradeOrder active = orderMapper.findActiveByItem(item.getId());
            vo.setActiveOrderId(active == null ? null : active.getId());
        }
        vo.setRiskNotice(RISK_NOTICE);

        String reason = evaluateOrderability(item, viewerId, owner);
        vo.setCanOrder(reason == null);
        vo.setCannotOrderReason(reason);
        return vo;
    }

    private String evaluateOrderability(Item item, Long viewerId, boolean owner) {
        if (!TradeConst.ITEM_ON_SALE.equals(item.getStatus())) {
            return "商品当前不在售（" + TradeConst.itemStatusText(item.getStatus()) + "）";
        }
        if (viewerId == null) {
            return "请先登录";
        }
        if (owner) {
            return "不能购买自己发布的商品";
        }
        User viewer = userMapper.findById(viewerId);
        if (viewer == null) {
            return "登录状态异常，请重新登录";
        }
        if (TradeConst.ACCOUNT_BANNED.equals(viewer.getAccountStatus())) {
            return "账户已被封禁，无法创建待确认请求";
        }
        if (TradeConst.ACCOUNT_LIMITED.equals(viewer.getAccountStatus())) {
            return "账户处于限权状态，暂时无法创建订单";
        }
        if (!TradeConst.AUTH_VERIFIED.equals(viewer.getAuthStatus())) {
            return "请先完成校园身份认证";
        }
        if (orderMapper.findPendingByItemAndBuyer(item.getId(), viewerId) != null) {
            return "你已提交过一条待确认请求，请等待卖家处理";
        }
        if (orderMapper.countPendingByItem(item.getId()) >= maxPendingPerItem) {
            return "该商品的待确认请求已达上限（" + maxPendingPerItem + " 条），请稍后再试";
        }
        return null;
    }

    /* ------------------------------ 卖家操作 ------------------------------ */

    @Transactional
    public ItemDetailVO create(Long sellerId, ItemDtos.ItemSaveRequest request) {
        validateCondition(request.condition());
        Category category = categoryMapper.findById(request.categoryId());
        if (category == null) {
            throw BizException.badRequest("所选商品分类不存在");
        }
        Item item = new Item();
        item.setSellerId(sellerId);
        item.setTitle(request.title().trim());
        item.setCategoryId(request.categoryId());
        item.setItemCondition(request.condition());
        item.setDescription(request.description().trim());
        item.setPrice(request.price());
        item.setHandoverArea(request.handoverArea().trim());
        item.setStatus(TradeConst.ITEM_DRAFT);
        itemMapper.insert(item);
        saveImages(item.getId(), request.images());
        auditService.log("ITEM_CREATE", "ITEM", item.getId(), "卖家创建商品草稿", null, item.getTitle());
        return buildDetail(itemMapper.findById(item.getId()), sellerId, false);
    }

    @Transactional
    public ItemDetailVO update(Long itemId, Long sellerId, ItemDtos.ItemSaveRequest request) {
        Item item = requireOwnedItem(itemId, sellerId);
        if (!EDITABLE.contains(item.getStatus())) {
            throw BizException.conflict("当前商品状态不允许编辑");
        }
        validateCondition(request.condition());
        Category category = categoryMapper.findById(request.categoryId());
        if (category == null) {
            throw BizException.badRequest("所选商品分类不存在");
        }

        boolean keyChanged = !Objects.equals(item.getTitle(), request.title().trim())
                || !Objects.equals(item.getCategoryId(), request.categoryId())
                || !Objects.equals(item.getItemCondition(), request.condition())
                || !Objects.equals(item.getDescription(), request.description().trim())
                || item.getPrice().compareTo(request.price()) != 0;

        String nextStatus = item.getStatus();
        LocalDateTime listedAt = item.getListedAt();
        if (TradeConst.ITEM_ON_SALE.equals(item.getStatus()) && keyChanged) {
            nextStatus = TradeConst.ITEM_PENDING;
            listedAt = null;
        } else if (TradeConst.ITEM_REJECTED.equals(item.getStatus())) {
            nextStatus = TradeConst.ITEM_DRAFT;
        }

        item.setTitle(request.title().trim());
        item.setCategoryId(request.categoryId());
        item.setItemCondition(request.condition());
        item.setDescription(request.description().trim());
        item.setPrice(request.price());
        item.setHandoverArea(request.handoverArea().trim());
        item.setStatus(nextStatus);
        item.setRejectReason(null);
        item.setListedAt(listedAt);
        itemMapper.update(item);
        saveImages(item.getId(), request.images());

        if (TradeConst.ITEM_PENDING.equals(nextStatus)) {
            notificationService.send(sellerId, TradeConst.NOTIFY_ITEM, "商品需要重新审核",
                    "你修改了商品《" + item.getTitle() + "》的关键信息，商品已重新进入待审核状态。",
                    "ITEM", item.getId());
        }
        auditService.log("ITEM_UPDATE", "ITEM", item.getId(), "卖家编辑商品", null, nextStatus);
        return buildDetail(itemMapper.findById(item.getId()), sellerId, false);
    }

    @Transactional
    public void delete(Long itemId, Long sellerId) {
        Item item = requireOwnedItem(itemId, sellerId);
        if (!Set.of(TradeConst.ITEM_DRAFT, TradeConst.ITEM_REJECTED, TradeConst.ITEM_OFF_SHELF).contains(item.getStatus())) {
            throw BizException.conflict("只有草稿、审核驳回或已下架的商品可以删除");
        }
        if (orderMapper.countByItem(itemId) > 0) {
            throw BizException.conflict("该商品已产生过交易记录，不能删除");
        }
        itemImageMapper.deleteByItemId(itemId);
        itemMapper.deleteById(itemId);
        auditService.log("ITEM_DELETE", "ITEM", itemId, "卖家删除商品", item.getTitle(), null);
    }

    @Transactional
    public ItemDetailVO submit(Long itemId, Long sellerId) {
        Item item = requireOwnedItem(itemId, sellerId);
        if (!Set.of(TradeConst.ITEM_DRAFT, TradeConst.ITEM_REJECTED, TradeConst.ITEM_OFF_SHELF).contains(item.getStatus())) {
            throw BizException.conflict("当前商品状态不允许提交审核");
        }
        validateComplete(item);
        itemMapper.updateStatus(itemId, TradeConst.ITEM_PENDING, null, null, null, null);
        auditService.log("ITEM_SUBMIT", "ITEM", itemId, "卖家提交审核", item.getStatus(), TradeConst.ITEM_PENDING);
        return buildDetail(itemMapper.findById(itemId), sellerId, false);
    }

    @Transactional
    public ItemDetailVO offShelf(Long itemId, Long sellerId) {
        Item item = requireOwnedItem(itemId, sellerId);
        if (!TradeConst.ITEM_ON_SALE.equals(item.getStatus())) {
            throw BizException.conflict("只有在售商品可以主动下架");
        }
        if (orderMapper.findActiveByItem(itemId) != null) {
            throw BizException.conflict("该商品存在已接受且未终结的订单，不能直接下架");
        }
        itemMapper.updateStatus(itemId, TradeConst.ITEM_OFF_SHELF, null, null, null, null);
        auditService.log("ITEM_OFF_SHELF", "ITEM", itemId, "卖家主动下架", TradeConst.ITEM_ON_SALE, TradeConst.ITEM_OFF_SHELF);
        return buildDetail(itemMapper.findById(itemId), sellerId, false);
    }

    /* ------------------------------ 管理端审核 ------------------------------ */

    @Transactional
    public AdminItemVO approve(Long itemId, Long adminId) {
        Item item = requireItem(itemId);
        if (!TradeConst.ITEM_PENDING.equals(item.getStatus())) {
            throw BizException.conflict("只有待审核商品可以批准");
        }
        LocalDateTime now = LocalDateTime.now();
        itemMapper.updateStatus(itemId, TradeConst.ITEM_ON_SALE, null, adminId, now, now);
        notificationService.send(item.getSellerId(), TradeConst.NOTIFY_ITEM, "商品审核通过",
                "你的商品《" + item.getTitle() + "》已通过审核并上架，可被校内用户搜索到。", "ITEM", itemId);
        auditService.log("ITEM_APPROVE", "ITEM", itemId, "管理员审核通过",
                TradeConst.ITEM_PENDING, TradeConst.ITEM_ON_SALE);
        return adminItem(itemId);
    }

    @Transactional
    public AdminItemVO reject(Long itemId, String reason, Long adminId) {
        Item item = requireItem(itemId);
        if (!TradeConst.ITEM_PENDING.equals(item.getStatus())) {
            throw BizException.conflict("只有待审核商品可以驳回");
        }
        if (TextUtils.isBlank(reason)) {
            throw BizException.badRequest("驳回必须填写原因");
        }
        itemMapper.updateStatus(itemId, TradeConst.ITEM_REJECTED, reason.trim(), adminId, LocalDateTime.now(), null);
        notificationService.send(item.getSellerId(), TradeConst.NOTIFY_ITEM, "商品审核被驳回",
                "你的商品《" + item.getTitle() + "》未通过审核，原因：" + reason.trim() + "。修改后可重新提交。",
                "ITEM", itemId);
        auditService.log("ITEM_REJECT", "ITEM", itemId, reason.trim(),
                TradeConst.ITEM_PENDING, TradeConst.ITEM_REJECTED);
        return adminItem(itemId);
    }

    @Transactional
    public AdminItemVO takedown(Long itemId, String reason, String ruleBasis, Long adminId) {
        Item item = requireItem(itemId);
        if (TradeConst.ITEM_VIOLATED.equals(item.getStatus())) {
            throw BizException.conflict("该商品已被违规下架");
        }
        if (TradeConst.ITEM_SOLD.equals(item.getStatus())) {
            throw BizException.conflict("已售出商品不能违规下架");
        }
        String before = item.getStatus();
        itemMapper.updateStatus(itemId, TradeConst.ITEM_VIOLATED, reason.trim(), adminId, LocalDateTime.now(), null);
        orderService.onItemTakedown(item, reason.trim(), adminId);
        notificationService.send(item.getSellerId(), TradeConst.NOTIFY_ITEM, "商品被违规下架",
                "你的商品《" + item.getTitle() + "》因违规被管理员下架。理由：" + reason.trim(), "ITEM", itemId);
        auditService.log("ITEM_TAKEDOWN", "ITEM", itemId,
                reason.trim() + (TextUtils.isBlank(ruleBasis) ? "" : "（依据：" + ruleBasis.trim() + "）"),
                before, TradeConst.ITEM_VIOLATED);
        return adminItem(itemId);
    }

    /* ------------------------------ 列表 ------------------------------ */

    public PageResult<ItemCardVO> myItems(Long sellerId, String status, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        String normalizedStatus = TextUtils.isBlank(status) ? null : status;
        List<ItemCardVO> records = itemMapper.findBySeller(sellerId, normalizedStatus, PageUtils.offset(p, s), s);
        records.forEach(this::fillTexts);
        return PageResult.of(records, itemMapper.countBySeller(sellerId, normalizedStatus), p, s);
    }

    public PageResult<ItemCardVO> favorites(Long userId, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 12);
        List<ItemCardVO> records = itemMapper.findFavorites(userId, PageUtils.offset(p, s), s);
        records.forEach(this::fillTexts);
        return PageResult.of(records, itemMapper.countFavorites(userId), p, s);
    }

    public PageResult<AdminItemVO> adminSearch(String status, String keyword, Integer page, Integer size) {
        int p = PageUtils.page(page);
        int s = PageUtils.size(size, 10);
        String normalizedKeyword = TextUtils.isBlank(keyword) ? null : TextUtils.escapeLike(keyword.trim());
        String normalizedStatus = TextUtils.isBlank(status) ? null : status;
        List<AdminItemVO> records = itemMapper.adminSearch(normalizedStatus, normalizedKeyword, PageUtils.offset(p, s), s);
        records.forEach(vo -> vo.setStatusText(TradeConst.itemStatusText(vo.getStatus())));
        long total = itemMapper.adminCount(normalizedStatus, normalizedKeyword);
        return PageResult.of(records, total, p, s);
    }

    /* ------------------------------ 收藏 ------------------------------ */

    @Transactional
    public FavoriteStateVO favorite(Long itemId, Long userId) {
        Item item = requireItem(itemId);
        if (item.getSellerId().equals(userId)) {
            throw BizException.conflict("不能收藏自己发布的商品");
        }
        int inserted = favoriteMapper.insert(userId, itemId);
        if (inserted > 0) {
            itemMapper.updateFavoriteCount(itemId, 1);
        }
        return favoriteState(itemId, userId);
    }

    @Transactional
    public FavoriteStateVO unfavorite(Long itemId, Long userId) {
        int deleted = favoriteMapper.delete(userId, itemId);
        if (deleted > 0) {
            itemMapper.updateFavoriteCount(itemId, -1);
        }
        return favoriteState(itemId, userId);
    }

    public FavoriteStateVO favoriteState(Long itemId, Long userId) {
        Item item = requireItem(itemId);
        boolean favorited = favoriteMapper.exists(userId, itemId) > 0;
        return new FavoriteStateVO(favorited, item.getFavoriteCount() == null ? 0 : item.getFavoriteCount());
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    public Item requireItem(Long itemId) {
        Item item = itemMapper.findById(itemId);
        if (item == null) {
            throw BizException.notFound("商品不存在");
        }
        return item;
    }

    private Item requireOwnedItem(Long itemId, Long sellerId) {
        Item item = requireItem(itemId);
        if (!item.getSellerId().equals(sellerId)) {
            throw BizException.forbidden("只能操作自己发布的商品");
        }
        return item;
    }

    private AdminItemVO adminItem(Long itemId) {
        AdminItemVO vo = itemMapper.findAdminById(itemId);
        if (vo == null) {
            throw BizException.notFound("商品不存在");
        }
        vo.setStatusText(TradeConst.itemStatusText(vo.getStatus()));
        return vo;
    }

    private void validateCondition(String condition) {
        if (!TradeConst.ITEM_CONDITIONS.contains(condition)) {
            throw BizException.badRequest("成色取值不合法");
        }
    }

    private void validateComplete(Item item) {
        if (TextUtils.isBlank(item.getTitle()) || item.getTitle().length() < 2) {
            throw BizException.badRequest("请填写 2-100 个字符的商品标题");
        }
        if (item.getCategoryId() == null) {
            throw BizException.badRequest("请选择商品分类");
        }
        if (TextUtils.isBlank(item.getItemCondition())) {
            throw BizException.badRequest("请选择成色");
        }
        if (TextUtils.isBlank(item.getDescription()) || item.getDescription().length() < 10) {
            throw BizException.badRequest("商品描述至少 10 个字符");
        }
        if (item.getPrice() == null) {
            throw BizException.badRequest("请填写价格");
        }
        if (TextUtils.isBlank(item.getHandoverArea())) {
            throw BizException.badRequest("请选择交付区域");
        }
        List<ItemImage> images = itemImageMapper.findByItemId(item.getId());
        if (images.isEmpty()) {
            throw BizException.badRequest("请至少上传 1 张商品图片");
        }
    }

    private void saveImages(Long itemId, List<String> urls) {
        itemImageMapper.deleteByItemId(itemId);
        if (urls == null || urls.isEmpty()) {
            return;
        }
        List<String> cleaned = new ArrayList<>();
        for (String url : urls) {
            if (!TextUtils.isBlank(url) && !cleaned.contains(url.trim())) {
                cleaned.add(url.trim());
            }
        }
        if (cleaned.size() > 6) {
            cleaned = cleaned.subList(0, 6);
        }
        if (cleaned.isEmpty()) {
            return;
        }
        List<ItemImage> images = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            ItemImage image = new ItemImage();
            image.setItemId(itemId);
            image.setUrl(cleaned.get(i));
            image.setSortNo(i);
            images.add(image);
        }
        itemImageMapper.insertBatch(images);
    }

    private void fillTexts(ItemCardVO vo) {
        vo.setConditionText(TradeConst.conditionText(vo.getCondition()));
        vo.setStatusText(TradeConst.itemStatusText(vo.getStatus()));
    }

    private boolean isPublicStatus(String status) {
        return Set.of(TradeConst.ITEM_ON_SALE, TradeConst.ITEM_LOCKED, TradeConst.ITEM_SOLD,
                TradeConst.ITEM_OFF_SHELF, TradeConst.ITEM_VIOLATED).contains(status);
    }
}
