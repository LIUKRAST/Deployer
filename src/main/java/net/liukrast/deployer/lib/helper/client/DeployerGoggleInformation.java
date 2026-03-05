package net.liukrast.deployer.lib.helper.client;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Expands the concept of goggle information to blocks and entities, rather than block entities only.<br>
 * Note: On blocks, you should use {@link DeployerGoggleInformation#addToGoogleTooltip(Level, BlockPos, BlockState, List, boolean)} rather than {@link IHaveGoggleInformation#addToGoggleTooltip(List, boolean)},
 * so that you can access world, position and state data.<br>
 * <pre>{@code
 *     public class MyBlock extends Block implements DeployerGoggleInformation {
 *         public boolean addToGoogleTooltip(Level level, BlockPos pos, BlockState state, List<Component> tooltip, boolean isPlayerSneaking) {
 *             //add components to the tooltip here
 *             return true;
 *         }
 *     }
 * }</pre>
 *
 *
 * On the other side, you can add IHaveGoggleInformation to entities, but you should not use this method below, but the original one from {@link IHaveGoggleInformation}
 * */
public interface DeployerGoggleInformation extends IHaveGoggleInformation {

    default boolean addToGoogleTooltip(Level level, BlockPos pos, BlockState state, List<Component> tooltip, boolean isPlayerSneaking) {
        return addToGoggleTooltip(tooltip, isPlayerSneaking);
    }
}
