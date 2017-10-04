package priceserver.rest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import priceserver.domain.Price;
import priceserver.service.PricerService;

@RestController
class PricesRestController {
	private final PricerService pricerService;

	public PricesRestController(PricerService pricerService) {
		this.pricerService = pricerService;
	}

	@RequestMapping(value = "/prices/{id}", method = RequestMethod.GET)
	Price getLatest(@PathVariable long id) {
		return pricerService.latestPrice(id);
	}

}
