package portalgun.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import portalgun.PortalColor;
import portalgun.portal.PortalSpawnManager;

public class PortalGunItem extends Item {
	private static final double MAX_DISTANCE = 96.0;
	private static final String KEY_SELECTED = "SelectedColor"; // выбранный цвет пушки (rgb)

	public PortalGunItem(Settings settings) { super(settings); }

	// ---- выбранный цвет (rgb) в NBT предмета ----
	public static int getSelectedColor(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		if (nbt != null && nbt.contains(KEY_SELECTED)) return nbt.getInt(KEY_SELECTED);
		return PortalColor.PALETTE[0];
	}
	public static void setSelectedColor(ItemStack stack, int rgb) {
		stack.getOrCreateNbt().putInt(KEY_SELECTED, rgb);
	}
	// Перебор цвета по палитре (бинд R). Возвращает новый цвет.
	public static int cycleColor(ItemStack stack) {
		int next = PortalColor.nextColor(getSelectedColor(stack));
		setSelectedColor(stack, next);
		return next;
	}

	private static int rgbOf(DyeColor color) {
		float[] c = color.getColorComponents();
		int r = (int) (c[0] * 255.0F);
		int g = (int) (c[1] * 255.0F);
		int b = (int) (c[2] * 255.0F);
		return (r << 16) | (g << 8) | b;
	}

	// §11 ПКМ -> ставит порталы ТЕКУЩЕГО цвета ПООЧЕРЁДНО (конец A, затем B, по кругу).
	// Краситель в левой руке вместо постановки красит текущий цвет.
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (hand == Hand.OFF_HAND) return TypedActionResult.pass(stack);

		if (!world.isClient) {
			ServerPlayerEntity sp = (ServerPlayerEntity) player;
			ItemStack off = player.getOffHandStack();
			if (off.getItem() instanceof DyeItem dye) {
				int rgb = rgbOf(dye.getColor());
				setSelectedColor(stack, rgb);
				sp.sendMessage(Text.literal("Portal Gun: цвет — " + PortalColor.nameOf(rgb)), true);
			} else {
				firePortal(sp.getServerWorld(), sp, getSelectedColor(stack));
			}
		}
		return TypedActionResult.success(stack);
	}

	// §11 ЛКМ -> убирает порталы (из AttackBlockCallback). Краситель в левой руке красит текущий цвет.
	public static void onLeftClick(ServerPlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		if (off.getItem() instanceof DyeItem dye) {
			int rgb = rgbOf(dye.getColor());
			setSelectedColor(stack, rgb);
			player.sendMessage(Text.literal("Portal Gun: цвет — " + PortalColor.nameOf(rgb)), true);
			return;
		}
		PortalSpawnManager.removeOwnedPortals(player.getServerWorld(), player);
		player.sendMessage(Text.literal("Portal Gun: порталы убраны"), true);
	}

	private static void firePortal(ServerWorld world, ServerPlayerEntity player, int rgb) {
		Vec3d eye = player.getCameraPosVec(1.0F);
		Vec3d dir = player.getRotationVec(1.0F);
		Vec3d end = eye.add(dir.multiply(MAX_DISTANCE));

		BlockHitResult hit = world.raycast(new RaycastContext(
			eye, end,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			player
		));

		// §12: если перед прицелом нет блока — роняем луч вниз и кладём на пол.
		if (hit.getType() != HitResult.Type.BLOCK) {
			Vec3d downStart = end;
			Vec3d downEnd = end.add(0.0, -MAX_DISTANCE, 0.0);
			hit = world.raycast(new RaycastContext(
				downStart, downEnd,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				player
			));
			if (hit.getType() != HitResult.Type.BLOCK) return;
		}

		PortalSpawnManager.placePortal(world, player, rgb, hit);
	}
}
