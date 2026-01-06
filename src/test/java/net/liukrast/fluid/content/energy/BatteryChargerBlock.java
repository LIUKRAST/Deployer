package net.liukrast.fluid.content.energy;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.liukrast.deployer.lib.logistics.packager.AbstractPackagerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BatteryChargerBlock extends AbstractPackagerBlock {
    /**
     * @param properties The block properties
     *
     */
    public BatteryChargerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
        return null;
    }

    @Override
    public boolean isSideValid(BlockEntity be) {
        return false;
    }
}
