package portalgun.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.lwjgl.glfw.GLFW;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalGunMod;
import portalgun.item.PortalGunItem;
import portalgun.portal.PortalColorAccess;

public class PortalGunClient implements ClientModInitializer {
	private static KeyBinding cycleColorKey;

	@Override
	public void onInitializeClient() {
		cycleColorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.portalgun.swap_colors",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"key.categories.portalgun"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (cycleColorKey.wasPressed()) {
				if (client.player != null) {
					ClientPlayNetworking.send(PortalGunMod.SWAP_COLORS, PacketByteBufs.empty());
				}
			}
		});

		// Подкраска слоя свечения текстуры (layer1) выбранным цветом пушки.
		// layer0 (база) не тонируется (-1 = белый множитель).
		ColorProviderRegistry.ITEM.register(
			(stack, tintIndex) -> tintIndex == 1
				? (0xFF000000 | PortalGunItem.getSelectedColor(stack))
				: -1,
			PortalGunMod.PORTAL_GUN
		);

		// Цветную обводку рисуем по всем портал-пушка клиентского мира.
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
			ClientWorld world = ctx.world();
			if (world == null) return;
			for (Entity entity : world.getEntities()) {
				if (entity instanceof Portal portal && PortalColorAccess.isPortalGun(portal)) {
					PortalFrameRenderer.drawOutline(ctx, portal, PortalColorAccess.getRgb(portal));
				}
			}
		});
	}
}
