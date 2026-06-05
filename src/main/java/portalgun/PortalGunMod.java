package portalgun;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import portalgun.fluid.PortalFluidManager;
import portalgun.item.PortalGunItem;

public class PortalGunMod implements ModInitializer {
	public static final String MOD_ID = "portalgun";
	public static final Identifier SWAP_COLORS = new Identifier(MOD_ID, "swap_colors");
	public static PortalGunItem PORTAL_GUN;

	@Override
	public void onInitialize() {
		// §13: загрузить/создать конфиг (config/portalgun.json).
		PortalGunConfig.get();

		PORTAL_GUN = Registry.register(
			Registries.ITEM,
			new Identifier(MOD_ID, "portal_gun"),
			new PortalGunItem(new Item.Settings().maxCount(1))
		);

		// Виден в креативной вкладке «Инструменты».
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
			.register(entries -> entries.add(PORTAL_GUN));

		// §11 ЛКМ -> сломать порталы (или перекрас, если в левой руке краситель).
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (!world.isClient
					&& player.getMainHandStack().getItem() instanceof PortalGunItem) {
				PortalGunItem.onLeftClick((ServerPlayerEntity) player);
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		// Бинд из настроек: поменять местами цвета слотов.
		ServerPlayNetworking.registerGlobalReceiver(SWAP_COLORS,
			(server, player, netHandler, buf, sender) -> server.execute(() -> {
				ItemStack stack = player.getMainHandStack();
				if (stack.getItem() instanceof PortalGunItem) {
					PortalGunItem.swapColors(stack);
					player.sendMessage(Text.literal("Portal Gun: цвета слотов поменяны местами"), true);
				}
			}));

		// Серверный тик: перекачка жидкости между порталами.
		ServerTickEvents.END_WORLD_TICK.register(PortalFluidManager::onWorldTick);
	}
}
