package com.campus.trade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FavoriteStateVO {
    private boolean favorited;
    private long count;
}
