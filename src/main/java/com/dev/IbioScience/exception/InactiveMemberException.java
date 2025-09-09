package com.dev.IbioScience.exception;

import org.springframework.security.core.AuthenticationException;

public class InactiveMemberException extends AuthenticationException {
	private static final long serialVersionUID = 1L;

	public InactiveMemberException(String msg) {
		super(msg);
	}
}