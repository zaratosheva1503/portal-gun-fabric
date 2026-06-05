package portalgun.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

	public PortalGunItem(Settings settings) { super(settings); }

	// ПКМ -> оранжевый
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (!world.isClient) {
			firePortal((ServerWorld) world, (ServerPlayerEntity) player, PortalColor.ORANGE);
		}
		return TypedActionResult.success(player.getStackInHand(hand));
	}

	// ЛКМ -> синий (из AttackBlockCallback)
	public static void onLeftClick(ServerPlayerEntity player) {
		firePortal(player.getServerWorld(), player, PortalColor.BLUE);
	}

	private static void firePortal(ServerWorld world, ServerPlayerEntity player, PortalColor color) {
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

		PortalSpawnManager.placePortal(world, player, color, hit);
	}
}
