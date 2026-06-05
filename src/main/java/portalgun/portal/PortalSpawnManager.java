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
import portalgun.PortalColor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortalSpawnManager {
	// Portal 2 пропорции: 1 блок в ширину, 2 в высоту.
	private static final double PORTAL_W = 1.0;
	private static final double PORTAL_H = 2.0;
	private static final double SURFACE_OFFSET = 0.01; // чуть «над» поверхностью
	private static final int ELLIPSE_SEGMENTS = 48;   // гладкость овала

	// playerUUID -> (слот -> UUID портала)
	private static final Map<UUID, EnumMap<PortalColor, UUID>> OWNED = new HashMap<>();

	private static UUID getExisting(ServerPlayerEntity p, PortalColor c) {
		return OWNED.getOrDefault(p.getUuid(), new EnumMap<>(PortalColor.class)).get(c);
	}
	private static void remember(ServerPlayerEntity p, PortalColor c, UUID id) {
		OWNED.computeIfAbsent(p.getUuid(), k -> new EnumMap<>(PortalColor.class)).put(c, id);
	}

	// ---- спавн с привязкой к сетке ----
	public static void placePortal(ServerWorld world, ServerPlayerEntity player,
								   PortalColor channel, int rgb, BlockHitResult hit) {
		Direction face = hit.getSide();
		Vec3d normal = Vec3d.of(face.getVector());
		BlockPos blockPos = hit.getBlockPos();
		Vec3d hitPos = hit.getPos();

		Vec3d origin;
		Vec3d axisW;
		Vec3d axisH;

		if (face.getAxis().isVertical()) {
			// --- ПОЛ / ПОТОЛОК: портал лежит горизонтально ---
			Vec3d look = Vec3d.fromPolar(0.0F, player.getYaw()).normalize();
			axisH = look;                                    // длинная ось (2) вдоль взгляда
			axisW = axisH.crossProduct(normal).normalize();  // ширина (1) поперёк
			double cy = (face == Direction.UP)
				? blockPos.getY() + 1.0 + SURFACE_OFFSET
				: blockPos.getY() - SURFACE_OFFSET;
			origin = new Vec3d(blockPos.getX() + 0.5, cy, blockPos.getZ() + 0.5);
		} else {
			// --- СТЕНА: высота строго вверх, низ прибит к сетке блоков ---
			axisH = new Vec3d(0.0, 1.0, 0.0);
			axisW = axisH.crossProduct(normal).normalize();
			double centerY = Math.floor(hitPos.y) + 1.0;     // центр ровно 2-блочного портала
			double cx = blockPos.getX() + 0.5 + normal.x * (0.5 + SURFACE_OFFSET);
			double cz = blockPos.getZ() + 0.5 + normal.z * (0.5 + SURFACE_OFFSET);
			origin = new Vec3d(cx, centerY, cz);
		}

		Portal portal = Portal.entityType.create(world);
		portal.setOriginPos(origin);
		portal.setOrientationAndSize(axisW.normalize(), axisH.normalize(), PORTAL_W, PORTAL_H);
		portal.setDestinationDimension(world.getRegistryKey());
		portal.setDestination(origin); // временно, до линковки пары
		// Овальная дыра как в Portal 2 — штатным хелпером IP (корректно и для рендера, и для коллизий).
		PortalManipulation.makePortalRound(portal, ELLIPSE_SEGMENTS);
		PortalColorAccess.set(portal, channel, rgb);

		// удалить старый портал того же слота у этого игрока
		UUID old = getExisting(player, channel);
		if (old != null) {
			Entity e = world.getEntity(old);
			if (e instanceof Portal p) p.remove(Entity.RemovalReason.DISCARDED);
		}

		McHelper.spawnServerEntity(portal);
		portal.reloadAndSyncToClientNextTick(); // пересчёт bounding box / коллизии + формы
		remember(player, channel, portal.getUuid());

		linkPair(world, player);
	}

	// ---- линковка пары ----
	private static void linkPair(ServerWorld world, ServerPlayerEntity player) {
		UUID blueId = getExisting(player, PortalColor.BLUE);
		UUID orangeId = getExisting(player, PortalColor.ORANGE);
		if (blueId == null || orangeId == null) return;

		Entity be = world.getEntity(blueId);
		Entity oe = world.getEntity(orangeId);
		if (!(be instanceof Portal blue) || !(oe instanceof Portal orange)) return;

		// Двусторонняя связь по позиции/измерению.
		blue.setDestinationDimension(orange.getOriginDim());
		blue.setDestination(orange.getOriginPos());
		orange.setDestinationDimension(blue.getOriginDim());
		orange.setDestination(blue.getOriginPos());

		// Штатный расчёт разворота IP (с флипом вокруг axisH): выставляет rotation
		// обоим порталам так, что вход смотрит В портал, а выход — ИЗ него.
		// Благодаря этому при любом угле захода (в том числе боком) выкидывает
		// строго на правильный выход, а не в стену/в другую сторону.
		PortalManipulation.adjustRotationToConnect(blue, orange);

		blue.reloadAndSyncToClientNextTick();
		orange.reloadAndSyncToClientNextTick();
	}
}
