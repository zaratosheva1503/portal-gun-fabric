package portalgun.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.Portal;

/**
 * Сохранение модуля скорости при переходе через портал (Portal 2 momentum).
 * Имя метода transformVelocity может отличаться по версиям IP — сверить по декомпиляции.
 */
@Mixin(Portal.class)
public abstract class PortalVelocityMixin {

	@Inject(method = "transformVelocity", at = @At("RETURN"), cancellable = true)
	private void portalgun$preserveMomentum(Entity entity, CallbackInfoReturnable<Vec3d> cir) {
		Portal self = (Portal) (Object) this;

		Vec3d inVel = entity.getVelocity();
		double inSpeed = inVel.length();
		if (inSpeed < 1.0e-4) return;

		Vec3d rotated = cir.getReturnValue();
		if (rotated == null || rotated.lengthSquared() < 1.0e-8) {
			rotated = self.transformLocalVecNonScale(inVel);
		}
		double boost = 1.0; // 1.0 = чистое сохранение; >1.0 = разгон
		cir.setReturnValue(rotated.normalize().multiply(inSpeed * boost));
	}
}
