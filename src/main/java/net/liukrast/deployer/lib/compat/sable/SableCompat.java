package net.liukrast.deployer.lib.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public class SableCompat {
    public static boolean SABLE_INSTALLED = false;

    public static @Nullable Pair<Vec3, Vec3> setRayFromSubLevel(BlockEntity be, Vec3 eyePos, Vec3 rayEnd, Vec3 lookVec, double reach) {
        if (!SABLE_INSTALLED) return null;

        SubLevel subLevel = Sable.HELPER.getContainingClient(be);
        if (subLevel != null) {
            Optional<Vec3> subLevelHit = subLevel.boundingBox().toMojang().clip(eyePos, rayEnd);
            if (subLevelHit.isPresent()) {
                eyePos = subLevel.logicalPose().transformPositionInverse(subLevelHit.get());
                rayEnd = eyePos.add(lookVec.scale(reach));
            }
        }

        return Pair.of(eyePos, rayEnd);
    }
}
