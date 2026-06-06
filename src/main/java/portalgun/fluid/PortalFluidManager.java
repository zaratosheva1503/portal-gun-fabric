package portalgun.fluid;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fluids through portals (section 10).
 *
 * Strategy: if there is a STILL fluid source at a portal's entrance, place a stable
 * SOURCE block in the AIR block in front of the partner portal. A source persists,
 * is clearly visible and spreads like real water. We track the positions we placed so
 * we can (a) remove them when the entrance source is gone, and (b) ignore our own placed
 * sources during detection so there is no feedback loop.
 */
public final class PortalFluidManager {
	private static final int PERIOD = 5;

	// Source blocks WE placed at portal exits, per world.
	private static final Map<ServerWorld, Set<BlockPos>> PLACED = new HashMap<>();

	private PortalFluidManager() {}

	public static void onWorldTick(ServerWorld world) {
		if (world.getTime() % PERIOD != 0) return;

		Set<BlockPos> placed = PLACED.computeIfAbsent(world, w -> new HashSet<>());

		List<Portal> portals = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof Portal p && PortalColorAccess.isPortalGun(p) && p.destination != null) {
				portals.add(p);
			}
		}

		Set<BlockPos> activeNow = new HashSet<>();

		for (Portal portal : portals) {
			boolean lava = hasSourceAtEntry(world, portal, placed, true);
			boolean water = lava ? false : hasSourceAtEntry(world, portal, placed, false);
			if (!lava && !water) continue;

			Portal partner = findPartner(portals, portal);
			if (partner == null) continue;

			BlockPos exit = exitBlock(partner);
			if (exit == null || !canPlace(world, exit, placed)) continue;

			world.setBlockState(exit, (lava ? Blocks.LAVA : Blocks.WATER).getDefaultState(), 3);
			placed.add(exit);
			activeNow.add(exit);
		}

		// Cleanup: sources we placed but that are no longer fed by an entrance -> revert to air.
		if (!placed.isEmpty()) {
			List<BlockPos> toRemove = new ArrayList<>();
			for (BlockPos pos : placed) {
				if (activeNow.contains(pos)) continue;
				if (world.getBlockState(pos).getBlock() instanceof FluidBlock
						&& world.getFluidState(pos).isStill()) {
					world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
				}
				toRemove.add(pos);
			}
			placed.removeAll(toRemove);
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
