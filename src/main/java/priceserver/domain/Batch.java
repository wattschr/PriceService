package priceserver.domain;

import priceserver.exceptions.BatchAlreadyCommitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Batch keeps the state of the prices that are currently being uploaded.  We only want to update the latest bid,
 * and there is a chance that the users could update multiple prices for the same id, so we store those prices
 * in a map and keep only the latest.  Because we're using a ConcurrentHashMap there are no locks involved here
 * at all during the update, but there are locks on this class.  This is because if a commit happens we want to
 * block all other access to the Batch (so no-one tries to update at the same time or afterwards).
 */
public class Batch {
	private static final Logger log = LoggerFactory.getLogger(Batch.class);
	private final long id;
	private final ConcurrentMap<Long, Price> pricesToUpload = new ConcurrentHashMap<>(1000);
	private boolean open = true;

	//Multiple updaters may update the batch, but if we are commiting we want to make su
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	public Batch(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void upload(List<Price> prices) {
		log.trace("About to enter critical update region in Batch {}", id);
		readWriteLock.readLock().lock();
		try {
			log.debug("About to update prices for batch {}", id);
			if (!open) {
				throw new BatchAlreadyCommitted(id);
			}

			prices.forEach(price -> pricesToUpload.compute(price.getId(), new LatestPriceFinder(price)));
		} finally {
			readWriteLock.readLock().unlock();
		}
		log.info("Batch {} updated with {} prices", id, prices.size());
	}

	/**
	 * This returns all the prices that have been collected in the batch, and closes this batch so that it
	 * is no longer usable.
	 * @return All the latest prices from the current batch
	 */
	public Collection<Price> closeAndGetPrices() {
		log.info("About to close batch {}", id);
		readWriteLock.writeLock().lock();
		try {
			open = false;
			return pricesToUpload.values();
		} finally {
			readWriteLock.writeLock().unlock();
			log.info("Batch {} closed", id);
		}
	}


}
