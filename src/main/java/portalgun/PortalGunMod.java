package portalgun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import portalgun.fluid.PortalFluidManager;
import portalgun.item.PortalGunItem;

public class PortalGunMod implements ModInitializer {
	public static final String MOD_ID = "portalgun";
	public static PortalGunItem PORTAL_GUN;

	@Override
	public void onInitialize() {
		PORTAL_GUN = Registry.register(
			Registries.ITEM,
			new Identifier(MOD_ID, "portal_gun"),
			new PortalGunItem(new Item.Settings().maxCount(1))
		);

		// ЛКМ -> синий портал. SUCCESS, чтобы не ломать блок.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (!world.isClient
					&& player.getMainHandStack().getItem() instanceof PortalGunItem) {
				PortalGunItem.onLeftClick((ServerPlayerEntity) player);
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		// Серверный тик: перекачка жидкости между порталами.
		ServerTickEvents.END_WORLD_TICK.register(PortalFluidManager::onWorldTick);
	}
}
