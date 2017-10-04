package priceserver;

import priceserver.domain.Price;

import java.time.ZonedDateTime;
import java.util.function.LongFunction;

class PriceFactory implements LongFunction<Price> {
	private ZonedDateTime time;

	PriceFactory(ZonedDateTime time) {
		this.time = time;
	}

	@Override
	public Price apply(long id) {
		return new Price(id, time, time.toString());
	}
}
