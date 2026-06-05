package portalgun.portal;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.DQuaternion;
import portalgun.PortalColor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalSpawnManager {
	private static final double PORTAL_W = 1.0;
	private static final double PORTAL_H = 2.0;

	// playerUUID -> (цвет -> UUID портала)
	private static final Map<UUID, EnumMap<PortalColor, UUID>> OWNED = new HashMap<>();

	private static UUID getExisting(ServerPlayerEntity p, PortalColor c) {
		return OWNED.getOrDefault(p.getUuid(), new EnumMap<>(PortalColor.class)).get(c);
	}
	private static void remember(ServerPlayerEntity p, PortalColor c, UUID id) {
		OWNED.computeIfAbsent(p.getUuid(), k -> new EnumMap<>(PortalColor.class)).put(c, id);
	}

	// ---- (б) расчёт ориентации ----
	private static Vec3d[] buildAxes(Direction face, float playerYaw) {
		Vec3d normal = Vec3d.of(face.getVector());
		if (face.getAxis().isVertical()) {
			// пол/потолок: верх портала вдоль взгляда игрока
			Vec3d look = Vec3d.fromPolar(0, playerYaw).normalize();
			Vec3d axisH = look;
			Vec3d axisW = axisH.crossProduct(normal);
			return new Vec3d[]{ axisW.normalize(), axisH.normalize() };
		} else {
			// стена: высота строго вверх
			Vec3d axisH = new Vec3d(0, 1, 0);
			Vec3d axisW = axisH.crossProduct(normal).normalize();
			return new Vec3d[]{ axisW, axisH };
		}
	}

	// ---- спавн ----
	public static void placePortal(ServerWorld world, ServerPlayerEntity player,
								   PortalColor color, BlockHitResult hit) {
		Direction face = hit.getSide();
		Vec3d normal = Vec3d.of(face.getVector());
		Vec3d origin = Vec3d.ofCenter(hit.getBlockPos()).add(normal.multiply(0.5 + 0.01));

		Vec3d[] axes = buildAxes(face, player.getYaw());

		Portal portal = Portal.entityType.create(world);
		portal.setOriginPos(origin);
		portal.setOrientationAndSize(axes[0], axes[1], PORTAL_W, PORTAL_H);
		portal.setDestinationDimension(world.getRegistryKey());
		portal.setDestination(origin); // временно
		PortalColorAccess.set(portal, color);

		// удалить старый портал того же цвета
		UUID old = getExisting(player, color);
		if (old != null) {
			Entity e = world.getEntity(old);
			if (e instanceof Portal p) p.remove(Entity.RemovalReason.DISCARDED);
		}

		McHelper.spawnServerEntity(portal);
		remember(player, color, portal.getUuid());
		PortalIndex.rebuild(world);

		linkPair(world, player);
	}

	// ---- (а) линковка ----
	private static void linkPair(ServerWorld world, ServerPlayerEntity player) {
		UUID blueId = getExisting(player, PortalColor.BLUE);
		UUID orangeId = getExisting(player, PortalColor.ORANGE);
		if (blueId == null || orangeId == null) return;

		Entity be = world.getEntity(blueId);
		Entity oe = world.getEntity(orangeId);
		if (!(be instanceof Portal blue) || !(oe instanceof Portal orange)) return;

		connect(blue, orange);
		connect(orange, blue);
	}

	private static void connect(Portal src, Portal dst) {
		src.setDestinationDimension(dst.getOriginDim());
		src.setDestination(dst.getOriginPos());
		src.setRotationTransformationD(computeRotation(src, dst));
		src.reloadAndSyncToClient();
	}

	// ---- (в) расчёт rotation через кватернионы ----
	private static DQuaternion computeRotation(Portal src, Portal dst) {
		Vec3d sW = src.axisW, sH = src.axisH, sN = src.getNormal();
		Vec3d dW = dst.axisW, dH = dst.axisH, dN = dst.getNormal().multiply(-1);

		DQuaternion qSrc = DQuaternion.matrixToQuaternion(sW, sH, sN);
		DQuaternion qDst = DQuaternion.matrixToQuaternion(dW, dH, dN);
		return qDst.hamiltonProduct(qSrc.getConjugated());
	}
}
