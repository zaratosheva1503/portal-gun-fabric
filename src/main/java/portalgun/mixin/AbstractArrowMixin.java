package portalgun.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.portal.PortalIndex;

/** Стрелы пролетают портал насквозь, сохраняя траекторию. */
@Mixin(PersistentProjectileEntity.class)
public abstract class AbstractArrowMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void portalgun$passThroughPortal(CallbackInfo ci) {
		ProjectileEntity self = (ProjectileEntity) (Object) this;
		if (self.getWorld().isClient) return;

		Vec3d from = self.getPos();
		Vec3d to = from.add(self.getVelocity());
		Portal portal = PortalIndex.raycastPortalPlane(self.getWorld(), from, to);
		if (portal == null) return;

		Vec3d newPos = portal.transformPoint(to);
		Vec3d newVel = portal.transformLocalVecNonScale(self.getVelocity());
		self.setPos(newPos.x, newPos.y, newPos.z);
		self.setVelocity(newVel);
		self.velocityDirty = true;
	}
}
