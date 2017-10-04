package priceserver.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class LatestPriceFinder implements BiFunction<Long, Price, Price> {
	private static final Logger log = LoggerFactory.getLogger(LatestPriceFinder.class);

	private final Price newPrice;

	public LatestPriceFinder(Price newPrice) {
		this.newPrice = newPrice;
	}

	@Override
	public Price apply(@SuppressWarnings("unused") Long key, Price oldPrice) {

		final Price priceToSet = shouldUseNewPrice(oldPrice) ? newPrice : oldPrice;

		log.trace("For price id {}, old price was {}, new price is {}, we keep {}", key, oldPrice, newPrice, priceToSet );

		return priceToSet;
	}

	private boolean shouldUseNewPrice(Price oldPrice) {
		return oldPrice == null || oldPrice.getDateTime().isBefore(newPrice.getDateTime());
	}

}
