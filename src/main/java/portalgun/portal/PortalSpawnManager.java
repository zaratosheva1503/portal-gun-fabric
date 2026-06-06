package portalgun.portal;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import portalgun.PortalGunConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalSpawnManager {
	// Portal 2 пропорции: 1 блок в ширину, 2 в высоту.
	private static final double PORTAL_W = 1.0;
	private static final double PORTAL_H = 2.0;
	private static final double SURFACE_OFFSET = 0.01; // чуть «над» поверхностью
	private static final int ELLIPSE_SEGMENTS = 48;   // гладкость овала

	// Состояние пары порталов одного цвета: два конца + чей черёд ставить.
	private static final class Pair {
		UUID endA;
		UUID endB;
		boolean nextIsB; // false -> следующий выстрел ставит конец A, true -> конец B
	}

	// playerUUID -> (rgb цвета -> пара порталов этого цвета).
	// У каждого игрока (мультиплеер) свои порталы; каждый цвет = отдельная пара,
	// поэтому можно держать сразу несколько разноцветных пар одновременно.
	private static final Map<UUID, Map<Integer, Pair>> OWNED = new HashMap<>();

	private static Pair pairFor(ServerPlayerEntity p, int rgb) {
		return OWNED.computeIfAbsent(p.getUuid(), k -> new HashMap<>())
			.computeIfAbsent(rgb, k -> new Pair());
	}

	// ---- спавн с привязкой к сетке ----
	public static void placePortal(ServerWorld world, ServerPlayerEntity player,
								   int rgb, BlockHitResult hit) {
		Direction face = hit.getSide();
		Vec3d normal = Vec3d.of(face.getVector());
		BlockPos blockPos = hit.getBlockPos();

		Vec3d origin;
		Vec3d axisW;
		Vec3d axisH;

		if (face.getAxis().isVertical()) {
			// --- ПОЛ / ПОТОЛОК: портал лежит горизонтально, СТРОГО по осям мира ---
			Direction horiz = player.getHorizontalFacing();
			axisH = Vec3d.of(horiz.getVector());             // длинная ось (2) вдоль стороны игрока
			axisW = axisH.crossProduct(normal).normalize();  // ширина (1) поперёк
			double cy = (face == Direction.UP)
				? blockPos.getY() + 1.0 + SURFACE_OFFSET
				: blockPos.getY() - SURFACE_OFFSET;
			origin = new Vec3d(blockPos.getX() + 0.5, cy, blockPos.getZ() + 0.5);
			origin = origin.add(axisH.multiply(0.5));
		} else {
			// --- СТЕНА: высота строго вверх, центр привязан к сетке блока ---
			axisH = new Vec3d(0.0, 1.0, 0.0);
			axisW = axisH.crossProduct(normal).normalize();
			double centerY = blockPos.getY() + 1.0;
			double cx = blockPos.getX() + 0.5 + normal.x * (0.5 + SURFACE_OFFSET);
			double cz = blockPos.getZ() + 0.5 + normal.z * (0.5 + SURFACE_OFFSET);
			origin = new Vec3d(cx, centerY, cz);
		}

		Portal portal = Portal.entityType.create(world);
		portal.setOriginPos(origin);
		portal.setOrientationAndSize(axisW.normalize(), axisH.normalize(), PORTAL_W, PORTAL_H);
		portal.setDestinationDimension(world.getRegistryKey());
		portal.setDestination(origin); // временно, до линковки пары
		PortalManipulation.makePortalRound(portal, ELLIPSE_SEGMENTS);
		portal.hasCrossPortalCollision = true;
		portal.teleportable = true;
		portal.setTeleportChangesGravity(false);
		PortalColorAccess.set(portal, rgb);

		Pair pair = pairFor(player, rgb);
		boolean placeB = pair.nextIsB;

		// удалить старый портал того же конца у этого цвета
		UUID old = placeB ? pair.endB : pair.endA;
		if (old != null) {
			Entity e = world.getEntity(old);
			if (e instanceof Portal p) p.remove(Entity.RemovalReason.DISCARDED);
		}

		McHelper.spawnServerEntity(portal);
		portal.reloadAndSyncToClientNextTick();

		if (placeB) pair.endB = portal.getUuid();
		else pair.endA = portal.getUuid();
		pair.nextIsB = !placeB;

		linkPair(world, pair);
	}

	// §11: ЛКМ ломает порталы — убираем ВСЕ порталы этого игрока (всех цветов).
	public static void removeOwnedPortals(ServerWorld world, ServerPlayerEntity player) {
		Map<Integer, Pair> byColor = OWNED.get(player.getUuid());
		if (byColor == null) return;
		for (Pair pair : new ArrayList<>(byColor.values())) {
			removeEnd(world, pair.endA);
			removeEnd(world, pair.endB);
		}
		byColor.clear();
	}

	private static void removeEnd(ServerWorld world, UUID id) {
		if (id == null) return;
		Entity e = world.getEntity(id);
		if (e instanceof Portal p) p.remove(Entity.RemovalReason.DISCARDED);
	}

	// ---- линковка пары одного цвета ----
	private static void linkPair(ServerWorld world, Pair pair) {
		if (pair.endA == null || pair.endB == null) return;

		Entity ae = world.getEntity(pair.endA);
		Entity be = world.getEntity(pair.endB);
		if (!(ae instanceof Portal a) || !(be instanceof Portal b)) return;

		// Двусторонняя связь по позиции/измерению.
		a.setDestinationDimension(b.getOriginDim());
		a.setDestination(b.getOriginPos());
		b.setDestinationDimension(a.getOriginDim());
		b.setDestination(a.getOriginPos());

		// §13: разворот вида включается/выключается конфигом config/portalgun.json.
		if (PortalGunConfig.get().rotateCameraOnTeleport) {
			PortalManipulation.adjustRotationToConnect(a, b);
		} else {
			a.setRotation(null);
			b.setRotation(null);
		}

		a.reloadAndSyncToClientNextTick();
		b.reloadAndSyncToClientNextTick();
	}
}
