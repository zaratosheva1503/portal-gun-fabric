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
 * продолжается из парного.
 *
 * §10: дозируем перенос — увеличен период и ограничен уровень выходной жидкости.
 * Детекция проверяет несколько клеток перед порталом, чтобы не пропустить точную точку входа.
 */
public final class PortalFluidManager {
	/** Период тиков между проходами. */
	private static final int PERIOD = 4;
	/** Максимальный уровень жидкости на выходе. */
	private static final int MAX_EXIT_LEVEL = 6;

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;

		for (Entity entity : world.iterateEntities()) {
			if (!(entity instanceof Portal portal)) continue;
			if (!PortalColorAccess.isPortalGun(portal)) continue;
			// Используем публичное поле destination (подтверждёно в IP API),
			// чтобы не зависеть от возможных проблем с getDestPos() в рантайме.
			if (portal.destination == null) continue;

			Vec3d normal = portal.getNormal();
			for (BlockPos entry : entryCells(portal, normal)) {
				FluidState fs = world.getFluidState(entry);
				if (fs.isEmpty()) continue;

				int level = fs.getLevel(); // 8 = источник, 1..7 = течёт
				// Принимаем любой непустой поток, включая уровень 1 (слабая струя).
				int outLevel = Math.min(Math.max(level, 1), MAX_EXIT_LEVEL);

				Vec3d exitWorld = portal.transformPoint(Vec3d.ofCenter(entry));
				BlockPos exitPos = BlockPos.ofFloored(exitWorld);

				// Сам портал-источник — не записываем в самое себя.
				if (exitPos.equals(entry)) continue;
				FluidState exitFs = world.getFluidState(exitPos);
				// Твёрдый блок на выходе без жидкости — пропускаем.
				if (!world.getBlockState(exitPos).isAir() && exitFs.isEmpty()) continue;
				// Уровень уже достаточный — не перезаписываем (анти-цикл).
				if (!exitFs.isEmpty() && exitFs.getLevel() >= outLevel) continue;

				placeFlowing(world, exitPos, fs, outLevel);
			}
		}
	}

	/**
	 * Клетки перед плоскостью портала, где ищем жидкость.
	 * Проверяем на 0, 0.5 и 1.0 блока вдоль нормали, чтобы не пропустить
	 * жидкость, вплотную прилегающую к входу портала.
	 */
	private static List<BlockPos> entryCells(Portal portal, Vec3d normal) {
		List<BlockPos> cells = new ArrayList<>();
		Vec3d origin = portal.getOriginPos();
		double halfH = portal.height / 2.0;

		for (double nOff = 0.0; nOff <= 1.0; nOff += 0.5) {
			for (double v = -halfH + 0.5; v < halfH; v += 1.0) {
				Vec3d p = origin
					.add(portal.axisH.multiply(v))
					.add(normal.multiply(nOff));
				BlockPos bp = BlockPos.ofFloored(p);
				if (!cells.contains(bp)) cells.add(bp);
			}
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
