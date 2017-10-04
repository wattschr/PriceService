package priceserver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.GONE)
public class BatchAlreadyCommitted extends RuntimeException {
	public BatchAlreadyCommitted(long id) {
		super("Batch [" + id + "] has already been committed");
	}
}
