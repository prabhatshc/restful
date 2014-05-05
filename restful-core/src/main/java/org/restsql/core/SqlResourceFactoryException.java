package org.restsql.core;

public class SqlResourceFactoryException extends SqlResourceException {
	
	private static final long serialVersionUID = 1L;

	public SqlResourceFactoryException(final String message) {
		super(message);
	}

	public SqlResourceFactoryException(final Throwable cause) {
		super(cause);
	}
}
