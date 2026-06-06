package portalgun.fluid;

import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
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
 * Жидкость сквозь портал (§10).
 *
 * Надёжная, само-очищающаяся схема без бесконечной петли:
 *  1. У устья портала ищем ИСТОЧНИК жидкости (still): озеро, вылитое ведро и т.п.
 *  2. Если источник есть — льём ТЕКУЧУЮ жидкость в открытый блок у устья ПАРНОГО портала
 *     (в воздух перед порталом, куда вода реально может встать).
 *  3. Детектируем только ИСТОЧНИКИ, а льём только ТЕКУЧУЮ жидкость → парный портал
 *     никогда не примет наш поток за «вход» → петли нет.
 *  4. Пропал источник на входе — текучая жидкость на выходе исчезает сама за ~секунду.
 */
public final class PortalFluidManager {
	/** Период тиков между проходами (мало — чтобы струя не мерцала). */
	private static final int PERIOD = 2;

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;

		// Собираем все порталы-пушки этого мира.
		List<Portal> portals = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof Portal p && PortalColorAccess.isPortalGun(p) && p.destination != null) {
				portals.add(p);
			}
		}

		for (Portal portal : portals) {
			FluidState src = findSourceAtEntry(world, portal);
			if (src == null) continue;

			Portal partner = findPartner(portals, portal);
			if (partner == null) continue;

			BlockPos exit = mouthBlock(partner);
			if (exit == null || !canPlaceFluid(world, exit)) continue;

			placeFlowing(world, exit, src);
		}
	}

	/**
	 * Ищем ИСТОЧНИК жидкости у входа портала — по всей высоте,
	 * чуть перед плоскостью, на плоскости и сразу за ней.
	 */
	private static FluidState findSourceAtEntry(ServerWorld world, Portal portal) {
		Vec3d normal = portal.getNormal();
		Vec3d origin = portal.getOriginPos();
		double halfH = portal.height / 2.0;

		for (double v = -halfH + 0.5; v < halfH; v += 1.0) {
			for (double nOff = -0.3; nOff <= 1.0; nOff += 0.65) {
				Vec3d p = origin
					.add(portal.axisH.multiply(v))
					.add(normal.multiply(nOff));
				FluidState fs = world.getFluidState(BlockPos.ofFloored(p));
				if (!fs.isEmpty() && fs.isStill()) return fs;
			}
		}
		return null;
	}

	/** Открытый блок у устья портала, куда выливаем жидкость. */
	private static BlockPos mouthBlock(Portal partner) {
		return BlockPos.ofFloored(partner.getOriginPos());
	}

	/** Парный портал — ближайший к точке назначения. */
	private static Portal findPartner(List<Portal> portals, Portal portal) {
		Vec3d dest = portal.destination;
		if (dest == null) return null;
		Portal best = null;
		double bestD = 1.0; // не дальше ~1 блока от точки назначения
		for (Portal p : portals) {
			if (p == portal) continue;
			double d = p.getOriginPos().squaredDistanceTo(dest);
			if (d < bestD) { bestD = d; best = p; }
		}
		return best;
	}

	/** Можно ли поставить жидкость: воздух или уже жидкость (твёрдое не трогаем). */
	private static boolean canPlaceFluid(ServerWorld world, BlockPos pos) {
		if (world.getBlockState(pos).isAir()) return true;
		return !world.getFluidState(pos).isEmpty();
	}

	private static void placeFlowing(ServerWorld world, BlockPos pos, FluidState src) {
		boolean lava = src.isIn(FluidTags.LAVA);
		Fluid fluid = lava ? Fluids.FLOWING_LAVA : Fluids.FLOWING_WATER;
		// Полный «падающий» поток (level 8, falling) — чтобы вода уверенно лилась и растекалась.
		FluidState flowing = ((net.minecraft.fluid.FlowableFluid) fluid).getFlowing(8, true);

		FluidState cur = world.getFluidState(pos);
		if (cur.getFluid() == fluid && cur.getLevel() >= 8) return; // уже льётся — не дёргаем

		world.setBlockState(pos, flowing.getBlockState(), 3);
		world.scheduleFluidTick(pos, fluid, fluid.getTickRate(world));
	}
}
