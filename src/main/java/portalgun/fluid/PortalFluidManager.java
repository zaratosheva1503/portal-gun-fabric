package portalgun.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.portal.PortalColorAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fluids through portals (section 10).
 *
 * If there is a STILL fluid source at a portal's entrance, we keep a stable SOURCE block
 * in the air block in front of the partner portal.
 *
 * Anti-flicker design:
 *  - Idempotent placement: if the exit block already holds our source of the right type,
 *    we do NOT call setBlockState again (no visual re-trigger every cycle).
 *  - Grace period: an exit source is removed only after its entrance has been empty for
 *    GRACE_TICKS in a row, so a single missed detection on one tick can't blink the water.
 *  - We ignore our own placed exits during detection, so there is no feedback loop.
 */
public final class PortalFluidManager {
	private static final int PERIOD = 5;
	/** How long (ticks) an exit keeps its fluid after the entrance stops feeding it. */
	private static final long GRACE_TICKS = 30;

	// exit pos -> last game time it was fed by an entrance source, per world.
	private static final Map<ServerWorld, Map<BlockPos, Long>> FED = new HashMap<>();

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;
		long now = world.getTime();
		Map<BlockPos, Long> fed = FED.computeIfAbsent(world, w -> new HashMap<>());
		Set<BlockPos> placed = fed.keySet();

		List<Portal> portals = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof Portal p && PortalColorAccess.isPortalGun(p) && p.destination != null) {
				portals.add(p);
			}
		}

		for (Portal portal : portals) {
			boolean lava = hasSourceAtEntry(world, portal, placed, true);
			boolean water = lava ? false : hasSourceAtEntry(world, portal, placed, false);
			if (!lava && !water) continue;

			Portal partner = findPartner(portals, portal);
			if (partner == null) continue;

			BlockPos exit = exitBlock(partner);
			if (exit == null || !canPlace(world, exit, placed)) continue;

			// Idempotent: only set the block if it is NOT already our still source of this type.
			FluidState fs = world.getFluidState(exit);
			boolean alreadyOk = fs.isStill() && fs.isIn(lava ? FluidTags.LAVA : FluidTags.WATER);
			if (!alreadyOk) {
				world.setBlockState(exit, (lava ? Blocks.LAVA : Blocks.WATER).getDefaultState(), 3);
			}
			fed.put(exit, now); // mark as fed this tick
		}

		// Cleanup with grace: remove an exit only if it has not been fed for GRACE_TICKS.
		if (!fed.isEmpty()) {
			List<BlockPos> toRemove = new ArrayList<>();
			for (Map.Entry<BlockPos, Long> en : fed.entrySet()) {
				if (now - en.getValue() <= GRACE_TICKS) continue; // recently fed -> keep, no flicker
				BlockPos pos = en.getKey();
				if (world.getBlockState(pos).getBlock() instanceof FluidBlock
						&& world.getFluidState(pos).isStill()) {
					world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
				}
				toRemove.add(pos);
			}
			for (BlockPos pos : toRemove) fed.remove(pos);
		}
	}

	/** Is there a STILL source of the wanted type at the portal entrance (ignoring our own placed sources)? */
	private static boolean hasSourceAtEntry(ServerWorld world, Portal portal, Set<BlockPos> placed, boolean wantLava) {
		Vec3d normal = portal.getNormal();
		Vec3d origin = portal.getOriginPos();
		double halfH = portal.height / 2.0;

		for (double v = -halfH + 0.5; v < halfH; v += 1.0) {
			for (double nOff = -0.3; nOff <= 1.0; nOff += 0.45) {
				BlockPos bp = BlockPos.ofFloored(
					origin.add(portal.axisH.multiply(v)).add(normal.multiply(nOff)));
				if (placed.contains(bp)) continue; // our own placed source -> skip (no loop)
				FluidState fs = world.getFluidState(bp);
				if (fs.isEmpty() || !fs.isStill()) continue;
				boolean isLava = fs.isIn(FluidTags.LAVA);
				if (isLava == wantLava) return true;
			}
		}
		return false;
	}

	/** The AIR block right in FRONT of the partner's mouth, where the fluid pours out. */
	private static BlockPos exitBlock(Portal partner) {
		return BlockPos.ofFloored(partner.getOriginPos().add(partner.getNormal().multiply(0.5)));
	}

	private static Portal findPartner(List<Portal> portals, Portal portal) {
		Vec3d dest = portal.destination;
		if (dest == null) return null;
		Portal best = null;
		double bestD = 1.0;
		for (Portal p : portals) {
			if (p == portal) continue;
			double d = p.getOriginPos().squaredDistanceTo(dest);
			if (d < bestD) { bestD = d; best = p; }
		}
		return best;
	}

	/** Placeable if air or already a fluid / our own placed pos; never overwrite solid blocks. */
	private static boolean canPlace(ServerWorld world, BlockPos pos, Set<BlockPos> placed) {
		if (placed.contains(pos)) return true;
		if (world.getBlockState(pos).isAir()) return true;
		return !world.getFluidState(pos).isEmpty();
	}
}
