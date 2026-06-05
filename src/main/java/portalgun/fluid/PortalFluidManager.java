package portalgun.fluid;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.portal.PortalIndex;

import java.util.HashMap;
import java.util.Map;

/**
 * Жидкость сквозь портал: жидкость, дошедшая до плоскости одного портала,
 * продолжается из парного. Серверный «насос» с лимитом уровня и кулдауном.
 */
public final class PortalFluidManager {
	private static final int PERIOD = 5;          // насос не каждый тик
	private static final int EXIT_COOLDOWN = 10;  // кулдаун на позицию выхода

	private static final Map<BlockPos, Long> exitCooldownUntil = new HashMap<>();

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;

		for (Portal portal : PortalIndex.all()) {
			if (portal.getDestination() == null) continue;

			Vec3d normal = portal.getNormal();
			BlockPos entryFront = BlockPos.ofFloored(
				portal.getOriginPos().add(normal.multiply(0.5)));

			FluidState fs = world.getFluidState(entryFront);
			if (fs.isEmpty()) continue;

			int level = fs.getLevel(); // 8 = source, 1..7 = течёт
			if (level <= 1) continue;

			Vec3d exitWorld = portal.transformPoint(Vec3d.ofCenter(entryFront));
			BlockPos exitFront = BlockPos.ofFloored(exitWorld);

			long now = world.getTime();
			Long until = exitCooldownUntil.get(exitFront);
			if (until != null && now < until) continue;

			FluidState exitFs = world.getFluidState(exitFront);
			if (!world.getBlockState(exitFront).isAir() && exitFs.isEmpty()) continue;
			if (!exitFs.isEmpty() && exitFs.getLevel() >= level) continue;

			placeFlowing(world, exitFront, fs, level - 1);
			exitCooldownUntil.put(exitFront, now + EXIT_COOLDOWN);
		}
	}

	private static void placeFlowing(ServerWorld world, BlockPos pos, FluidState src, int level) {
		boolean lava = src.isIn(FluidTags.LAVA);
		FluidState flowing = (lava ? Fluids.FLOWING_LAVA : Fluids.FLOWING_WATER)
				.getFlowing(Math.max(1, level), false);
		world.setBlockState(pos, flowing.getBlockState(), 3);
		world.scheduleFluidTick(pos, flowing.getFluid(),
				flowing.getFluid().getTickRate(world));
	}
}
