package com.campus.trade.vo;

import lombok.Data;

import java.util.List;

@Data
public class MetaVO {
    private List<CategoryVO> categories;
    private List<OptionVO> conditions;
    private List<String> areas;
    private List<OptionVO> reportCategories;

    @Data
    public static class OptionVO {
        private String value;
        private String label;

        public OptionVO() {
        }

        public OptionVO(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
