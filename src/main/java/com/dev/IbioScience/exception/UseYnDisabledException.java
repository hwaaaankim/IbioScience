package com.dev.IbioScience.exception;

import org.springframework.security.core.AuthenticationException;

public class UseYnDisabledException extends AuthenticationException {
	private static final long serialVersionUID = 1L;

	public UseYnDisabledException(String msg) {
		super(msg);
	}
}