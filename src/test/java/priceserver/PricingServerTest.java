package priceserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import priceserver.domain.Price;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PricingServerTest {
	private final ExecutorService exec = Executors.newFixedThreadPool(4);

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private PriceServerConnection priceServerConnection;

	@BeforeEach
	void setUp() {
		priceServerConnection =  new PriceServerConnection(port, testRestTemplate);
	}

	@Test
	void getPriceReturnsNullIfNoPriceForId() {
		final ResponseEntity<Price> shouldBeNullPrice = priceServerConnection.getPrice(20L);
		assertEquals(HttpStatus.NOT_FOUND, shouldBeNullPrice.getStatusCode());
	}

	@Test
	void uploadToBatchThatDoesNotExistResultsIn404() {
		final ResponseEntity<Void> voidResponseEntity =
				priceServerConnection.uploadBatch(
						12214242L,
						Collections.singletonList(price(995945949L)));

		assertEquals(HttpStatus.NOT_FOUND, voidResponseEntity.getStatusCode());
	}

	@Test
	void uploadToBatchFollowedByACancelMeansThePricesUpdatedAreLost() {
		Long batchId = createAndValidateBatch();

		final long id = 32423423L;
		priceServerConnection.uploadBatch(batchId, Collections.singletonList(price(id)));

		priceServerConnection.cancelBatch(id);

		final ResponseEntity<Price> expectedEmptyPrice = priceServerConnection.getPrice(id);
		assertEquals(HttpStatus.NOT_FOUND, expectedEmptyPrice.getStatusCode());
	}

	@Test
	void createBatchReturnsBatchIdAndUploadingAndCommittingThatBatchResultsInPricesBeingAccessibleButFurtherUploadsToThatBatchFails() {
		Long batchId = createAndValidateBatch();

		final Price originalPrice = price(31212323L);

		priceServerConnection.uploadBatch(batchId, Collections.singletonList(originalPrice));

		//Price should not be visible before it is committed.
		final ResponseEntity<Price> expectedEmptyPrice = priceServerConnection.getPrice(originalPrice.getId());
		assertEquals(HttpStatus.NOT_FOUND, expectedEmptyPrice.getStatusCode());

		priceServerConnection.commitBatch(batchId);

		final ResponseEntity<Price> requestedPrice = priceServerConnection.getPrice(originalPrice.getId());
		assertEquals(HttpStatus.OK, requestedPrice.getStatusCode());

		assertTrue(requestedPrice.hasBody());

		assertEquals(originalPrice, requestedPrice.getBody());

		final ResponseEntity<Void> failedUploadBatch =
				priceServerConnection.uploadBatch(batchId, Collections.singletonList(price(12131232L)));

		assertEquals(HttpStatus.GONE, failedUploadBatch.getStatusCode());
	}

	@Test
	void createManyBatchesAndPricesRunConcurrentlyAndEnsureOnlyTheLatestPricesAreSaved() {
		final BatchUploader batchThatShouldWin = new BatchUploader(7);
		final List<Future<?>> futures = Arrays.asList(
				exec.submit(new BatchUploader(0)),
				exec.submit(new BatchUploader(3)),
				exec.submit(batchThatShouldWin),
				exec.submit(new BatchUploader(5))
		                                             );

		futures.forEach(f -> {
			try {
				f.get(10, TimeUnit.SECONDS);
			} catch (RuntimeException e) {
				throw e;
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		LongStream.range(1, 901).forEach(id -> validateBatch(id, batchThatShouldWin.timeFirstBatch));
		LongStream.range(901, 1901).forEach(id -> validateBatch(id, batchThatShouldWin.timeSecondBatch));
		LongStream.range(1901, 2800).forEach(id -> validateBatch(id, batchThatShouldWin.timeThirdBatch));
	}

	private void validateBatch(long id, ZonedDateTime expectedTime) {
		final ResponseEntity<Price> priceResponse = priceServerConnection.getPrice(id);
		assertEquals(HttpStatus.OK, priceResponse.getStatusCode());
		assertEquals(expectedTime.getNano(), priceResponse.getBody().getDateTime().getNano());
	}

	private Long createAndValidateBatch() {
		final ResponseEntity<Long> batchCreatedResponse = priceServerConnection.createBatch();

		assertEquals(HttpStatus.OK, batchCreatedResponse.getStatusCode());
		assertTrue(batchCreatedResponse.hasBody());

		assertNotNull(batchCreatedResponse.getBody());

		return batchCreatedResponse.getBody();
	}


	private Price price(long id) {
		return new Price(id, ZonedDateTime.now(ZoneOffset.UTC), "dsds");
	}


	private class BatchUploader implements Runnable {

		private final ZonedDateTime timeFirstBatch;
		private final ZonedDateTime timeSecondBatch;
		private final ZonedDateTime timeThirdBatch;

		BatchUploader(int hoursOffset) {
			timeFirstBatch = ZonedDateTime.now(ZoneOffset.UTC).plusHours(hoursOffset);
			timeSecondBatch = ZonedDateTime.now(ZoneOffset.UTC).plusHours(hoursOffset + 1);
			timeThirdBatch = ZonedDateTime.now(ZoneOffset.UTC).plusHours(hoursOffset - 1);
		}

		@Override
		public void run() {
			List<List<Price>> pricesBatch = Arrays.asList(
					LongStream.range(1, 1000)
					          .mapToObj(new PriceFactory(timeFirstBatch)).collect(Collectors.toList()),
					LongStream.range(901, 1900)
					          .mapToObj(new PriceFactory(timeSecondBatch)).collect(Collectors.toList()),
					LongStream.range(1801, 2800)
					          .mapToObj(new PriceFactory(timeThirdBatch)).collect(Collectors.toList()));
			Long batchId = createAndValidateBatch();

			pricesBatch.forEach(batch -> priceServerConnection.uploadBatch(batchId, batch));

			priceServerConnection.commitBatch(batchId);
		}
	}
}
