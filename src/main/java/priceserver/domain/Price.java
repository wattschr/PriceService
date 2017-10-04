package priceserver.domain;

import jdk.nashorn.internal.ir.annotations.Immutable;

import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * This represents the price.  The flexible data structure can be stored as JSON or XML in the payload.
 * The class is 'effectively' immutable.  The only case where it is not is during creation from XML or JSON
 * where the private default constructor is used and the fields are created.  After that you cannot change it.
 */
@Immutable
public class Price {

	@NotNull
	private long id;

	@NotNull
	private ZonedDateTime dateTime;

	@NotNull
	private String payload;

	public Price(long id, ZonedDateTime dateTime, String payload) {
		this.id = id;
		this.dateTime = dateTime;
		this.payload = payload;
	}

	@SuppressWarnings("all")
	private Price() {
		//For use by JSON
	}

	public long getId() {
		return id;
	}

	public ZonedDateTime getDateTime() {
		return dateTime;
	}

	public String getPayload() {
		return payload;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Price price = (Price) o;
		return id == price.id &&
				Objects.equals(dateTime.getNano(), price.dateTime.getNano()) &&
				Objects.equals(payload, price.payload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, dateTime, payload);
	}

	@Override
	public String toString() {
		return "Price{" +
				"id=" + id +
				", dateTime=" + dateTime +
				", payload='" + payload + '\'' +
				'}';
	}
}
