package priceserver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class BatchNotFoundException extends RuntimeException {
	public BatchNotFoundException(long id) {
		super("Cannot find batch with id [" + id + "]");
	}
}
