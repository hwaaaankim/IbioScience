package com.dev.IbioScience.exception;

public class ProductNotFoundException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2025L;

	public ProductNotFoundException(Long id) {
        super("존재하지 않는 상품입니다. id=" + id);
    }
}