package priceserver;

import org.springframework.http.HttpStatus;
import priceserver.domain.Price;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

class PriceServerConnection {
	private final String urlLocalPricingservice;
	private final TestRestTemplate template;

	PriceServerConnection(int port, TestRestTemplate testRestTemplate) {
		urlLocalPricingservice =  "http://localhost:" + port + "/";
		template = testRestTemplate;
	}

	ResponseEntity<Price> getPrice(long priceId) {
		return template.getForEntity(urlLocalPricingservice + "prices/" + priceId, Price.class);
	}


	ResponseEntity<Void> uploadBatch(Long batchId, List<Price> prices) {
		return template.exchange(urlLocalPricingservice + "batches/" + batchId + "/upload",
                         HttpMethod.POST,
                         new HttpEntity<>(prices),
                         Void.class
                        );
	}

	void commitBatch(Long batchId) {
		final ResponseEntity<Void> commitResponse =
				template.exchange(urlLocalPricingservice + "batches/" + batchId + "/commit",
				                  HttpMethod.POST,
				                  null,
				                  Void.class
				                 );
		if (commitResponse.getStatusCode() != HttpStatus.OK) {
			throw new IllegalStateException("Commit failed " + commitResponse);
		}
	}

	ResponseEntity<Long> createBatch() {
		return template.exchange(urlLocalPricingservice + "/batches/create",
		                         HttpMethod.POST,
		                         null,
		                         Long.class
		                                                       );
	}

	public void cancelBatch(long batchId) {
		template.exchange(urlLocalPricingservice + "batches/" + batchId + "/cancel",
		                  HttpMethod.POST,
		                  null,
		                  Void.class
		                 );

	}
}
