package priceserver.service;

import priceserver.domain.Batch;
import priceserver.domain.Price;

import java.util.List;

public interface PricerService {
	Batch createBatch();

	void upload(long batchId, List<Price> prices);

	void commit(long batchId);

	void cancel(long batchId);

	Price latestPrice(long id);

}
