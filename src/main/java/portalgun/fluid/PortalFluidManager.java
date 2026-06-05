package portalgun.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.portal.PortalColorAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Жидкость сквозь портал: жидкость, дошедшая до плоскости одного портала,
 * продолжается из парного. Проверяем все клетки, которые накрывает портал (1×2).
 *
 * Берём только ЖИВЫЕ порталы прямо из мира (а не кэш PortalIndex, который не
 * чистился) — удалённые/заменённые порталы больше не качают воду «в пустоту».
 * Уровень при переносе уменьшаем на 1 — это и анти-зацикливание между парой,
 * и естественное затухание после исчезновения источника.
 */
public final class PortalFluidManager {
	private static final int PERIOD = 5; // насос не каждый тик

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;

		for (Entity entity : world.iterateEntities()) {
			if (!(entity instanceof Portal portal)) continue;
			if (!PortalColorAccess.isPortalGun(portal)) continue;
			if (portal.getDestPos() == null) continue;

			Vec3d normal = portal.getNormal();
			for (BlockPos entry : entryCells(portal, normal)) {
				FluidState fs = world.getFluidState(entry);
				if (fs.isEmpty()) continue;

				int level = fs.getLevel(); // 8 = источник, 1..7 = течёт
				if (level <= 1) continue;

				Vec3d exitWorld = portal.transformPoint(Vec3d.ofCenter(entry));
				BlockPos exitPos = BlockPos.ofFloored(exitWorld);

				FluidState exitFs = world.getFluidState(exitPos);
				// твёрдый блок на выходе — пропускаем
				if (!world.getBlockState(exitPos).isAir() && exitFs.isEmpty()) continue;
				// на выходе уже не меньше — не перетираем (анти-зацикливание пары)
				if (!exitFs.isEmpty() && exitFs.getLevel() >= level) continue;

				placeFlowing(world, exitPos, fs, level - 1);
			}
		}
	}

	// Все клетки воздуха перед плоскостью портала (1 шир. × 2 выс.).
	private static List<BlockPos> entryCells(Portal portal, Vec3d normal) {
		List<BlockPos> cells = new ArrayList<>();
		Vec3d origin = portal.getOriginPos();
		double half = portal.height / 2.0;
		for (double v = -half + 0.5; v < half; v += 1.0) {
			Vec3d p = origin
				.add(portal.axisH.multiply(v))
				.add(normal.multiply(0.5));
			cells.add(BlockPos.ofFloored(p));
		}
		return cells;
	}

	private static void placeFlowing(ServerWorld world, BlockPos pos, FluidState src, int level) {
		boolean lava = src.isIn(FluidTags.LAVA);
		FluidState flowing = (lava ? Fluids.FLOWING_LAVA : Fluids.FLOWING_WATER)
				.getFlowing(Math.min(8, Math.max(1, level)), false);
		world.setBlockState(pos, flowing.getBlockState(), 3);
		world.scheduleFluidTick(pos, flowing.getFluid(),
				flowing.getFluid().getTickRate(world));
	}
}
