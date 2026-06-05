package portalgun.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * Цветная овальная обводка портала в стиле Portal 2 + сплошная заливка тыла (§9).
 * Рисуется поверх штатного рендера сквозной поверхности Immersive Portals.
 */
public final class PortalFrameRenderer {
	private static final int SEGMENTS = 48;       // гладкость овала
	private static final double RING_SCALE = 1.0; // 1.0 = овал касается краёв портала

	private PortalFrameRenderer() {}

	// §9: сплошная цветная заливка ТЫЛЬНОЙ стороны портала.
	// С лица там рабочая поверхность телепорта, поэтому заливаем только когда камера сзади.
	public static void drawBackFill(WorldRenderContext ctx, Portal portal, int rgb) {
		VertexConsumerProvider consumers = ctx.consumers();
		if (consumers == null) return;

		Camera cam = ctx.camera();
		Vec3d camPos = cam.getPos();
		if (portal.isInFrontOfPortal(camPos)) return; // камера спереди — не закрываем рабочую поверхность

		Vec3d w = portal.axisW.multiply(portal.width / 2.0 * RING_SCALE);
		Vec3d h = portal.axisH.multiply(portal.height / 2.0 * RING_SCALE);
		// центр чуть смещён к тылу по нормали, чтобы не z-файтить с плоскостью портала
		Vec3d center = portal.getOriginPos().add(portal.getNormal().multiply(-0.01));

		MatrixStack ms = ctx.matrixStack();
		ms.push();
		ms.translate(-camPos.x, -camPos.y, -camPos.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getDebugFilledBox());

		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;

		// триангл-стрип «веером»: center, rim0, center, rim1, ... — сплошной эллипс.
		// два обхода (по и против часовой), чтобы было видно при любом отсечении граней.
		appendFan(vc, m, center, w, h, r, g, b, false);
		appendFan(vc, m, center, w, h, r, g, b, true);

		ms.pop();
	}

	private static void appendFan(VertexConsumer vc, Matrix4f m, Vec3d center, Vec3d w, Vec3d h,
								  float r, float g, float b, boolean reversed) {
		for (int i = 0; i <= SEGMENTS; i++) {
			double a = 2.0 * Math.PI * i / SEGMENTS;
			if (reversed) a = -a;
			Vec3d rim = center.add(w.multiply(Math.cos(a))).add(h.multiply(Math.sin(a)));
			vc.vertex(m, (float) center.x, (float) center.y, (float) center.z).color(r, g, b, 1f).next();
			vc.vertex(m, (float) rim.x, (float) rim.y, (float) rim.z).color(r, g, b, 1f).next();
		}
	}

	public static void drawOutline(WorldRenderContext ctx, Portal portal, int rgb) {
		VertexConsumerProvider consumers = ctx.consumers();
		if (consumers == null) return;

		Camera cam = ctx.camera();
		Vec3d camPos = cam.getPos();

		Vec3d o = portal.getOriginPos();
		// полуоси эллипса вдоль ширины и высоты портала
		Vec3d w = portal.axisW.multiply(portal.width / 2.0 * RING_SCALE);
		Vec3d h = portal.axisH.multiply(portal.height / 2.0 * RING_SCALE);

		MatrixStack ms = ctx.matrixStack();
		ms.push();
		ms.translate(-camPos.x, -camPos.y, -camPos.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());

		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;

		Vec3d prev = ellipsePoint(o, w, h, 0);
		for (int i = 1; i <= SEGMENTS; i++) {
			Vec3d cur = ellipsePoint(o, w, h, 2.0 * Math.PI * i / SEGMENTS);
			Vec3d dir = cur.subtract(prev).normalize();
			vc.vertex(m, (float) prev.x, (float) prev.y, (float) prev.z)
				.color(r, g, b, 1f)
				.normal((float) dir.x, (float) dir.y, (float) dir.z).next();
			vc.vertex(m, (float) cur.x, (float) cur.y, (float) cur.z)
				.color(r, g, b, 1f)
				.normal((float) dir.x, (float) dir.y, (float) dir.z).next();
			prev = cur;
		}
		ms.pop();
	}

	private static Vec3d ellipsePoint(Vec3d o, Vec3d w, Vec3d h, double angle) {
		return o.add(w.multiply(Math.cos(angle))).add(h.multiply(Math.sin(angle)));
	}
}
