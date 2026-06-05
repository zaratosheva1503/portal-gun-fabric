package portalgun.portal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.ArrayList;
import java.util.List;

/**
 * §9: физически непроходимый ТЫЛ портала.
 *
 * Immersive Portals прорезает дырку в блоке насквозь, поэтому без этого барьера
 * сквозь портал можно пройти с тыла (телепорта там нет — он только «перёд→зад»).
 * Этот серверный тик держит живые сущности позади плоскости, не пуская их «зад→перёд».
 * Лицевой вход (и §7-проваливание сквозь пол/потолок) не трогается.
 */
public final class PortalBackWall {
	/** На какой глубине за плоскостью стоит «стенка» (и с какой дистанции ловим). */
	private static final double HOLD = 0.5;

	private PortalBackWall() {}

	public static void onWorldTick(ServerWorld world) {
		List<Portal> portals = new ArrayList<>();
		for (Entity e : world.iterateEntities()) {
			if (e instanceof Portal p && PortalColorAccess.isPortalGun(p)) {
				portals.add(p);
			}
		}
		if (portals.isEmpty()) return;

		for (Entity e : world.iterateEntities()) {
			if (!(e instanceof LivingEntity)) continue;
			if (e instanceof PlayerEntity pl && pl.isSpectator()) continue;

			for (Portal portal : portals) {
				applyBarrier(e, portal);
			}
		}
	}

	private static void applyBarrier(Entity e, Portal portal) {
		Vec3d normal = portal.getNormal();
		Vec3d origin = portal.getOriginPos();
		Vec3d center = e.getBoundingBox().getCenter();
		Vec3d rel = center.subtract(origin);

		double d = rel.dotProduct(normal); // >0 — перед порталом, <0 — позади
		if (d > 0.0) return;               // с лица — рабочая зона телепорта, не трогаем
		if (d < -HOLD) return;             // далеко позади — не мешаем

		// в пределах овала портала?
		double hw = portal.width / 2.0;
		double hh = portal.height / 2.0;
		double pw = rel.dotProduct(portal.axisW);
		double ph = rel.dotProduct(portal.axisH);
		if ((pw * pw) / (hw * hw) + (ph * ph) / (hh * hh) > 1.0) return;

		// держим центр на глубине -HOLD (сдвиг вдоль -normal)
		double delta = -HOLD - d; // d в [-HOLD,0] -> delta в [-HOLD,0], т.е. только назад
		Vec3d feet = e.getPos();
		Vec3d newFeet = feet.add(normal.multiply(delta));
		e.setPosition(newFeet.x, newFeet.y, newFeet.z);

		// гасим компоненту скорости, направленную «внутрь» (вдоль +normal)
		Vec3d vel = e.getVelocity();
		double vN = vel.dotProduct(normal);
		if (vN > 0.0) {
			e.setVelocity(vel.subtract(normal.multiply(vN)));
			e.velocityModified = true;
		}
	}
}
