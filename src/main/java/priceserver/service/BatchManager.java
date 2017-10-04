package priceserver.service;

import priceserver.domain.Batch;
import priceserver.domain.Price;

import java.util.List;

public interface BatchManager {
	Batch create();

	void upload(long batchId, List<Price> prices);
}
