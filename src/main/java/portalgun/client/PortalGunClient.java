package portalgun.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalGunMod;
import portalgun.portal.PortalColorAccess;
import portalgun.portal.PortalIndex;

public class PortalGunClient implements ClientModInitializer {
	private static KeyBinding swapColorsKey;

	@Override
	public void onInitializeClient() {
		swapColorsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.portalgun.swap_colors",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"key.categories.portalgun"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (swapColorsKey.wasPressed()) {
				if (client.player != null) {
					ClientPlayNetworking.send(PortalGunMod.SWAP_COLORS, PacketByteBufs.empty());
				}
			}
		});

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
			for (Portal portal : PortalIndex.all()) {
				if (!PortalColorAccess.isPortalGun(portal)) continue;
				PortalFrameRenderer.drawOutline(ctx, portal, PortalColorAccess.getRgb(portal));
			}
		});
	}
}
