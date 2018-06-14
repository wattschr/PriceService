package priceserver.service.impl;

import priceserver.domain.Batch;
import priceserver.domain.Price;
import priceserver.service.BatchManager;
import priceserver.exceptions.BatchNotFoundException;
import net.jcip.annotations.ThreadSafe;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This implementation uses an EHCache as a source of batches.  This is because we will likely want to tune how this
 * works.  For example, there is no requirement for the user to close any batches he has open, so we will want to
 * keep batches around for some times, but tune when they get evicted.  In a real production system I would put
 * this configuration in an XML file somewhere, but for this exercise I have just used code to create it.
 *
 * The implementation is thread safe, and any locking is only dictated by the cache provider.  There is locking
 * going on at the batch level.
 * @see Batch
 */
@Component
@ThreadSafe
public class BatchManagerEhCacheImpl implements BatchManager {
	private static final Logger log = LoggerFactory.getLogger(BatchManagerEhCacheImpl.class);

	private final Cache<Long, Batch> ongoingBatches;

	private final AtomicLong nextIdHolder = new AtomicLong(1);


	public BatchManagerEhCacheImpl() {
		final CacheConfiguration<Long, Batch> cacheConfiguration =
				CacheConfigurationBuilder.newCacheConfigurationBuilder(Long.class,
				                                                       Batch.class,
				                                                       ResourcePoolsBuilder.heap(10000))
				                         .build();

		final CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
		                                                .withCache("batches", cacheConfiguration).build();

		cacheManager.init();

		ongoingBatches = cacheManager.getCache("batches", Long.class, Batch.class);
	}


	@Override
	public Batch create() {
		final Batch batch = new Batch(nextIdHolder.getAndIncrement());
		//Batch is completely new and we guarantee that the id is unique, so we can just put the batch in the cache
		ongoingBatches.put(batch.getId(), batch);

		log.info("New batch created {}", batch.getId());
		return batch;
	}

	@Override
	public void upload(long batchId, List<Price> prices){
		//Cache is thread-safe so we can just look up the batch without locks
		final Batch batch = safeGetBatch(batchId);
		log.debug("Updating batch {}", batch.getId());
		batch.upload(prices);
	}

	@NotNull
	private Batch safeGetBatch(long batchId) {
		final Batch batch = ongoingBatches.get(batchId);
		if (batch == null) {
			throw new BatchNotFoundException(batchId);
		}
		return batch;
	}

	public Collection<Price> closeBatch(long batchId) {
		final Batch batch = safeGetBatch(batchId);
		final Collection<Price> prices = batch.closeAndGetPrices();
		//The above call ensures that the batch is closed and can no longer be modified
		//So we can remove it now with out fear of other threads being involved
		//Does not matter if remove is called multiple times.
		ongoingBatches.remove(batchId);
		return prices;
	}


}
