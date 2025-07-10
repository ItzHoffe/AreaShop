package me.wiefferink.areashop.tools;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;

/**
 * Sign Utilities class
 */
public final class SignUtils {

    private SignUtils() {

    }

    /**
     * Get the {@link BlockFace} of a sign block
     * If the passed {@link Block} is not an instance of either
     * a {@link Sign} or {@link WallSign}, this method will return <code>null</code>
     *
     * @param block The sign
     * @return Returns the {@link BlockFace} of the sign or <code>null</code>
     */
    public static BlockFace getSignFacing(Block block) {
        if (block == null) {
            return null;
        }

        BlockState blockState = block.getState();
        BlockData blockData = blockState.getBlockData();

        return switch (blockData) {
            case WallSign wallSign -> wallSign.getFacing();
            case Sign sign -> sign.getRotation();
            case HangingSign hangingSign -> hangingSign.getRotation();
            case WallHangingSign wallHangingSign -> wallHangingSign.getFacing();
            default -> null;
        };
    }

    /**
     * Set the {@link BlockFace} in which the sign will face
     * If the passed {@link Block} is not an instance of either
     * a {@link Sign} or {@link WallSign}, this method will return <code>null</code>.
     *
     * @param block  The sign block
     * @param facing The new {@link BlockFace} the sign should face
     * @return Returns <code>true</code> if the sign was successfully placed, <code>false</code> otherwise
     */
    public static boolean setSignFacing(Block block, BlockFace facing) {
        if (block == null || facing == null) {
            return false;
        }

        BlockState blockState = block.getState();
        BlockData blockData = blockState.getBlockData();

        if (blockData instanceof WallSign wallSign) {
            wallSign.setFacing(facing);
        } else if (blockData instanceof org.bukkit.block.data.type.Sign sign) {
            sign.setRotation(facing);
        } else {
            return false;
        }
        block.setBlockData(blockData);
        return true;
    }

    /**
     * Get the {@link Block} which a given sign is attached to.
     * This method will return <code>null</code> if the passed {@link Block}
     * is not an instance of either a {@link Sign} or {@link WallSign}.
     *
     * @param block The sign {@link Block}
     * @return Returns the block which the sign is attached to or null
     */
    public static Block getSignAttachedTo(Block block) {
        if (block == null) {
            return null;
        }

        BlockState blockState = block.getState();

        BlockData blockData = blockState.getBlockData();

        if (blockData instanceof WallSign wallSign) {
            return block.getRelative(wallSign.getFacing().getOppositeFace());
        } else if (blockData instanceof Sign) {
            return block.getRelative(BlockFace.DOWN);
        }

        return null;
    }

}
