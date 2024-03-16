package telran.students.exceptions;

import telran.students.service.ServiceErrorMessages;

public class MarkIllegalStateException extends IllegalStateException {
	public MarkIllegalStateException() {
		super(ServiceErrorMessages.MARK_ALREADY_EXISTS);
	}

}
