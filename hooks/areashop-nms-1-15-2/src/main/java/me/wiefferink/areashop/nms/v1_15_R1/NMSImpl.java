package me.wiefferink.areashop.nms.v1_15_R1;

import me.wiefferink.areashop.interfaces.BlockBehaviourHelper;
import me.wiefferink.areashop.interfaces.NMS;

public final class NMSImpl implements NMS {

    private BlockBehaviourHelper signHelper = new BlockBehaviourHelperImpl();

    @Override
    public BlockBehaviourHelper behaviourHelper() {
        return this.signHelper;
    }

}
