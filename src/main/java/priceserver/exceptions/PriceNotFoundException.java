package priceserver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PriceNotFoundException extends RuntimeException {
	public PriceNotFoundException(long priceId) {
		super("Cannot find price for id [" + priceId + "]");
	}
}
