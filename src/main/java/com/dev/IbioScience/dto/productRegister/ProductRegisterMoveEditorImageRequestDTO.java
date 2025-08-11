package com.dev.IbioScience.dto.productRegister;

import java.util.List;

import lombok.Data;

@Data
public class ProductRegisterMoveEditorImageRequestDTO {
    private String type;
    private String key;
    private String html;
    private List<String> tempImgList;
}