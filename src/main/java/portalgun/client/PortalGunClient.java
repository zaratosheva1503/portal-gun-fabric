package portalgun.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalColor;
import portalgun.portal.PortalColorAccess;
import portalgun.portal.PortalIndex;

public class PortalGunClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
			for (Portal portal : PortalIndex.all()) {
				PortalColor color = PortalColorAccess.get(portal);
				PortalFrameRenderer.drawOutline(ctx, portal, color.rgb);
			}
		});
	}
}
