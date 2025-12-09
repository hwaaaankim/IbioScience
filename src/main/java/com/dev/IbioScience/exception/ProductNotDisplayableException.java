package com.dev.IbioScience.exception;

public class ProductNotDisplayableException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2025L;

	private final Long productId;

    public ProductNotDisplayableException(Long productId) {
        this(productId, "진열 불가 상품입니다.");
    }

    public ProductNotDisplayableException(Long productId, String message) {
        super(message);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}