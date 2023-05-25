package org.sandag.cvm.common.model;

public class ChoiceModelOverflowException extends RuntimeException {

	public ChoiceModelOverflowException() {
	}

	public ChoiceModelOverflowException(String message) {
		super(message);
	}

	public ChoiceModelOverflowException(Throwable cause) {
		super(cause);
	}

	public ChoiceModelOverflowException(String message, Throwable cause) {
		super(message, cause);
	}

}
