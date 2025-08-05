package com.dev.IbioScience.dto;

import com.dev.IbioScience.model.product.Promotion;

import lombok.Data;

@Data
public class PromotionSearchDTO {
    private Long id;
    private String name;
    private String type;
    private String typeLabel;
    private String term;
    private String termLabel;
    private boolean active;
    public static PromotionSearchDTO fromEntity(Promotion p) {
        PromotionSearchDTO dto = new PromotionSearchDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setType(p.getType().name());
        dto.setTypeLabel(p.getType().getLabel());
        dto.setTerm(p.getTerm().name());
        dto.setTermLabel(p.getTerm().getLabel());
        dto.setActive(p.getActive());
        return dto;
    }
}