package me.wiefferink.areashop.events.ask;

import me.wiefferink.areashop.events.CancellableRegionEvent;
import me.wiefferink.areashop.regions.GeneralRegion;

public class MoneyBackRegionEvent extends CancellableRegionEvent<GeneralRegion> {
    private double price;

    public MoneyBackRegionEvent(GeneralRegion region) {
        super(region);
        this.price = 0.0;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }
}
