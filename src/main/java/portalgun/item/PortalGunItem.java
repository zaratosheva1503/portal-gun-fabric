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
	private static final String KEY_PRIMARY = "PrimaryColor";
	private static final String KEY_SECONDARY = "SecondaryColor";
	private static final String KEY_NEXT_ORANGE = "NextOrange"; // §11: какой портал ставим следующим выстрелом ПКМ

	public PortalGunItem(Settings settings) { super(settings); }

	// ---- цвета слотов в NBT предмета ----
	public static int getColor(ItemStack stack, boolean primary) {
		NbtCompound nbt = stack.getNbt();
		String key = primary ? KEY_PRIMARY : KEY_SECONDARY;
		if (nbt != null && nbt.contains(key)) return nbt.getInt(key);
		return primary ? PortalColor.BLUE.rgb : PortalColor.ORANGE.rgb;
	}
	public static void setColor(ItemStack stack, boolean primary, int rgb) {
		stack.getOrCreateNbt().putInt(primary ? KEY_PRIMARY : KEY_SECONDARY, rgb);
	}
	public static void swapColors(ItemStack stack) {
		int p = getColor(stack, true);
		int s = getColor(stack, false);
		setColor(stack, true, s);
		setColor(stack, false, p);
	}

	// §11: чередование цвета при постановке через ПКМ.
	public static boolean isNextOrange(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null && nbt.getBoolean(KEY_NEXT_ORANGE);
	}
	public static void setNextOrange(ItemStack stack, boolean v) {
		stack.getOrCreateNbt().putBoolean(KEY_NEXT_ORANGE, v);
	}

	private static int rgbOf(DyeColor color) {
		float[] c = color.getColorComponents();
		int r = (int) (c[0] * 255.0F);
		int g = (int) (c[1] * 255.0F);
		int b = (int) (c[2] * 255.0F);
		return (r << 16) | (g << 8) | b;
	}

	// §11 ПКМ -> ставит порталы ПООЧЕРЁДНО (синий, затем оранжевый, по кругу).
	// Краситель в левой руке вместо постановки красит цвет, который пойдёт следующим.
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (hand == Hand.OFF_HAND) return TypedActionResult.pass(stack);

		if (!world.isClient) {
			ServerPlayerEntity sp = (ServerPlayerEntity) player;
			ItemStack off = player.getOffHandStack();
			boolean nextOrange = isNextOrange(stack);
			if (off.getItem() instanceof DyeItem dye) {
				// !nextOrange == слот primary (синий)
				setColor(stack, !nextOrange, rgbOf(dye.getColor()));
				sp.sendMessage(Text.literal("Portal Gun: цвет изменён"), true);
			} else {
				PortalColor channel = nextOrange ? PortalColor.ORANGE : PortalColor.BLUE;
				int rgb = getColor(stack, !nextOrange);
				firePortal(sp.getServerWorld(), sp, channel, rgb);
				setNextOrange(stack, !nextOrange); // следующий выстрел — другой цвет
			}
		}
		return TypedActionResult.success(stack);
	}

	// §11 ЛКМ -> ломает порталы (из AttackBlockCallback). Краситель в левой руке красит текущий слот.
	public static void onLeftClick(ServerPlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		if (off.getItem() instanceof DyeItem dye) {
			boolean nextOrange = isNextOrange(stack);
			setColor(stack, !nextOrange, rgbOf(dye.getColor()));
			player.sendMessage(Text.literal("Portal Gun: цвет изменён"), true);
			return;
		}
		PortalSpawnManager.removeOwnedPortals(player.getServerWorld(), player);
		player.sendMessage(Text.literal("Portal Gun: порталы убраны"), true);
	}

	private static void firePortal(ServerWorld world, ServerPlayerEntity player,
								   PortalColor channel, int rgb) {
		Vec3d eye = player.getCameraPosVec(1.0F);
		Vec3d dir = player.getRotationVec(1.0F);
		Vec3d end = eye.add(dir.multiply(MAX_DISTANCE));

		BlockHitResult hit = world.raycast(new RaycastContext(
			eye, end,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			player
		));
		if (hit.getType() != HitResult.Type.BLOCK) return;

		PortalSpawnManager.placePortal(world, player, channel, rgb, hit);
	}
}
