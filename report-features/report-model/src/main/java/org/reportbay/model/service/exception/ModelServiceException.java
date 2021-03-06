package org.reportbay.model.service.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback=true)
public class ModelServiceException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 * @param message
	 */
	public ModelServiceException(String message) {
		super(message);
	}
	/**
	 * 
	 * @param cause
	 */
	public ModelServiceException(Throwable cause) {
		super(cause);
	}
	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public ModelServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
