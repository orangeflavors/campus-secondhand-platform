package com.campus.trade.service;

import com.campus.trade.common.TradeConst;
import com.campus.trade.entity.Category;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.vo.CategoryVO;
import com.campus.trade.vo.MetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端所需的字典数据（分类、成色、区域、举报类别）。
 */
@Service
@RequiredArgsConstructor
public class MetaService {

    private final CategoryMapper categoryMapper;

    public MetaVO meta() {
        MetaVO meta = new MetaVO();
        meta.setCategories(categories());
        List<MetaVO.OptionVO> conditions = new ArrayList<>();
        for (String value : List.of(TradeConst.CONDITION_NEW, TradeConst.CONDITION_LIKE_NEW,
                TradeConst.CONDITION_GOOD, TradeConst.CONDITION_FAIR)) {
            conditions.add(new MetaVO.OptionVO(value, TradeConst.conditionText(value)));
        }
        meta.setConditions(conditions);
        meta.setAreas(List.of("东区宿舍", "西区宿舍", "南区宿舍", "北区宿舍", "图书馆",
                "第一食堂", "第二食堂", "体育馆", "教学楼", "校门口"));
        List<MetaVO.OptionVO> reportCategories = new ArrayList<>();
        reportCategories.add(new MetaVO.OptionVO("PROHIBITED", "违禁物品"));
        reportCategories.add(new MetaVO.OptionVO("FAKE", "虚假信息"));
        reportCategories.add(new MetaVO.OptionVO("FRAUD", "欺诈行为"));
        reportCategories.add(new MetaVO.OptionVO("ABUSE", "辱骂骚扰"));
        reportCategories.add(new MetaVO.OptionVO("SPAM", "广告刷屏"));
        reportCategories.add(new MetaVO.OptionVO("OTHER", "其他违规"));
        meta.setReportCategories(reportCategories);
        return meta;
    }

    public List<CategoryVO> categories() {
        List<Category> categories = categoryMapper.findAll();
        List<CategoryVO> result = new ArrayList<>();
        for (Category category : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setCode(category.getCode());
            result.add(vo);
        }
        return result;
    }
}
