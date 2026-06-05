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

	private static int rgbOf(DyeColor color) {
		float[] c = color.getColorComponents();
		int r = (int) (c[0] * 255.0F);
		int g = (int) (c[1] * 255.0F);
		int b = (int) (c[2] * 255.0F);
		return (r << 16) | (g << 8) | b;
	}

	// ПКМ -> оранжевый слот (или перекрас слота 2, если в левой руке краситель)
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (hand == Hand.OFF_HAND) return TypedActionResult.pass(stack);

		if (!world.isClient) {
			ServerPlayerEntity sp = (ServerPlayerEntity) player;
			ItemStack off = player.getOffHandStack();
			if (off.getItem() instanceof DyeItem dye) {
				setColor(stack, false, rgbOf(dye.getColor()));
				sp.sendMessage(Text.literal("Portal Gun: цвет слота 2 изменён"), true);
			} else {
				firePortal((ServerWorld) world, sp, PortalColor.ORANGE, getColor(stack, false));
			}
		}
		return TypedActionResult.success(stack);
	}

	// ЛКМ -> синий слот (из AttackBlockCallback); краситель в левой руке красит слот 1
	public static void onLeftClick(ServerPlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		if (off.getItem() instanceof DyeItem dye) {
			setColor(stack, true, rgbOf(dye.getColor()));
			player.sendMessage(Text.literal("Portal Gun: цвет слота 1 изменён"), true);
			return;
		}
		firePortal(player.getServerWorld(), player, PortalColor.BLUE, getColor(stack, true));
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
