package me.wiefferink.areashop.events.notify;

import me.wiefferink.areashop.events.NotifyRegionEvent;
import me.wiefferink.areashop.regions.RentRegion;

/**
 * Broadcasted when a region has been rented.
 */
public class RentedRegionEvent extends NotifyRegionEvent<RentRegion> {

	private final boolean extended;
	private final long before;

	/**
	 * Constructor.
	 * @param region   The region that has been rented
	 * @param extended true if the region has been extended, false if this is the first time buying the region
	 */
	public RentedRegionEvent(RentRegion region, boolean extended) {
		super(region);
		this.region = region;
		this.extended = extended;
		this.before = 0;
	}

	public RentedRegionEvent(RentRegion region, boolean extended, long before) {
		super(region);
		this.region = region;
		this.extended = extended;
		this.before = before;
	}

	/**
	 * Check if the region was extended or rented for the first time.
	 * @return true if the region was extended, false when rented for the first time
	 */
	public boolean hasExtended() {
		return extended;
	}

	public long getBefore() {
		return before;
	}
}
