package priceserver.service.impl;

import priceserver.domain.Batch;
import priceserver.domain.LatestPriceFinder;
import priceserver.domain.Price;
import priceserver.exceptions.PriceNotFoundException;
import priceserver.service.PricerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PricerServiceImpl implements PricerService {
	private static final Logger log = LoggerFactory.getLogger(PricerServiceImpl.class);
	private final BatchManagerEhCacheImpl batchManager;

	private final ConcurrentHashMap<Long, Price> currentPrices = new ConcurrentHashMap<>(10000);

	public PricerServiceImpl(@NotNull BatchManagerEhCacheImpl batchManager) {
		this.batchManager = batchManager;
	}

	@Override
	public Batch createBatch() {
		return batchManager.create();
	}

	@Override
	public void upload(long batchId, List<Price> prices)  {
		batchManager.upload(batchId, prices);
	}

	@Override
	public void commit(long batchId) {
		log.info("Committing {}", batchId);
		final Collection<Price> batchPrices = batchManager.closeBatch(batchId);

		batchPrices.forEach(price -> currentPrices.compute(price.getId(), new LatestPriceFinder(price)));
		log.info("Batch {} is now committed", batchId);
	}

	@Override
	public void cancel(long batchId) {
		log.info("Cancelling batch {}", batchId);
		batchManager.closeBatch(batchId);
		log.info("Batch {} cancelled", batchId);
	}

	@Override
	public Price latestPrice(long id) {
		final Price price = currentPrices.get(id);
		log.trace("Latest price for {} is {}", id, price);
		if (price == null) {
			throw new PriceNotFoundException(id);
		}
		return price;
	}
}
