package portalgun.portal;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.ArrayList;
import java.util.List;

/** Лёгкий кэш активных порталов, чтобы не перебирать все сущности каждый тик. */
public final class PortalIndex {
	private static final List<Portal> CACHE = new ArrayList<>();

	private PortalIndex() {}

	public static void rebuild(ServerWorld world) {
		CACHE.clear();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof Portal p) CACHE.add(p);
		}
	}

	public static List<Portal> all() { return CACHE; }

	public static Portal portalAt(World world, BlockPos pos) {
		Vec3d c = Vec3d.ofCenter(pos);
		for (Portal p : CACHE) {
			if (p.getOriginPos().squaredDistanceTo(c) < 1.5) return p;
		}
		return null;
	}

	public static Portal raycastPortalPlane(World world, Vec3d from, Vec3d to) {
		for (Portal p : CACHE) {
			if (p.getOriginPos().squaredDistanceTo(to) < 1.0) {
				return p;
			}
		}
		return null;
	}
}
