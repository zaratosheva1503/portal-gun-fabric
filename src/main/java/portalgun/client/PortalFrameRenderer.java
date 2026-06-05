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
 * Декоративная цветная обводка портала (4 ребра прямоугольника).
 * Рисуется поверх штатного рендера Immersive Portals.
 */
public final class PortalFrameRenderer {
	private PortalFrameRenderer() {}

	public static void drawOutline(WorldRenderContext ctx, Portal portal, int rgb) {
		VertexConsumerProvider consumers = ctx.consumers();
		if (consumers == null) return;

		Camera cam = ctx.camera();
		Vec3d camPos = cam.getPos();

		Vec3d o = portal.getOriginPos();
		Vec3d w = portal.axisW.multiply(portal.width / 2.0);
		Vec3d h = portal.axisH.multiply(portal.height / 2.0);

		Vec3d[] c = new Vec3d[] {
			o.add(w).add(h),
			o.subtract(w).add(h),
			o.subtract(w).subtract(h),
			o.add(w).subtract(h)
		};

		MatrixStack ms = ctx.matrixStack();
		ms.push();
		ms.translate(-camPos.x, -camPos.y, -camPos.z);
		Matrix4f m = ms.peek().getPositionMatrix();
		VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());

		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;

		for (int i = 0; i < 4; i++) {
			Vec3d a = c[i];
			Vec3d d = c[(i + 1) % 4];
			Vec3d dir = d.subtract(a).normalize();
			vc.vertex(m, (float) a.x, (float) a.y, (float) a.z)
				.color(r, g, b, 1f)
				.normal((float) dir.x, (float) dir.y, (float) dir.z).next();
			vc.vertex(m, (float) d.x, (float) d.y, (float) d.z)
				.color(r, g, b, 1f)
				.normal((float) dir.x, (float) dir.y, (float) dir.z).next();
		}
		ms.pop();
	}
}
