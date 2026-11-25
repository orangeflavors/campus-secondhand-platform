package com.campus.trade.entity;

import lombok.Data;

@Data
public class Category {
    private Long id;
    private String name;
    private String code;
    private Integer sortNo;
    private Integer enabled;
}
