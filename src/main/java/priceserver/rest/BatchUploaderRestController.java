package priceserver.rest;

import priceserver.domain.Batch;
import priceserver.domain.Price;
import priceserver.service.PricerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class BatchUploaderRestController {
	private static final Logger log = LoggerFactory.getLogger(BatchUploaderRestController.class);

	private final PricerService pricerService;

	BatchUploaderRestController(PricerService pricerService) {
		this.pricerService = pricerService;
	}

	@RequestMapping(value = "/batches/create", method = RequestMethod.POST, produces = "application/json")
	Long create() {
		final Batch batch = pricerService.createBatch();
		log.info("Batch {} has been created", batch);
		return batch.getId();
	}

	@RequestMapping(value = "/batches/{id}/upload", method = RequestMethod.POST)
	void upload(@PathVariable long id, @RequestBody List<Price> prices) {
		log.info("Received update for batch {} with {} prices", id, prices.size());
		log.trace("Updating following prices {}", prices);
		pricerService.upload(id, prices);
		log.info("Batch {} now updated", id);
	}

	@RequestMapping(value = "/batches/{id}/commit", method = RequestMethod.POST)
	void commit(@PathVariable long id) {
		log.info("About to commit batch {}", id);
		pricerService.commit(id);
		log.info("Batch {} now commited", id);
	}


	@RequestMapping(value = "/batches/{id}/cancel", method = RequestMethod.POST)
	void cancel(@PathVariable long id) {
		log.info("About to commit batch {}", id);
		pricerService.cancel(id);
		log.info("Batch {} now commited", id);
	}

}
