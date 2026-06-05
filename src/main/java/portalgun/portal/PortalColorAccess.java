package portalgun.portal;

import net.minecraft.nbt.NbtCompound;
import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalColor;

/**
 * Хранит цвет портала в его extraData (NbtCompound), который IP
 * синхронизирует на клиент автоматически.
 * NB: имя поля (extraData / additionalData) может отличаться между версиями IP.
 */
public final class PortalColorAccess {
	private static final String KEY = "portalgun:color";

	private PortalColorAccess() {}

	public static void set(Portal portal, PortalColor color) {
		if (portal.extraData == null) {
			portal.extraData = new NbtCompound();
		}
		portal.extraData.putString(KEY, color.name());
	}

	public static PortalColor get(Portal portal) {
		if (portal.extraData != null && portal.extraData.contains(KEY)) {
			return PortalColor.valueOf(portal.extraData.getString(KEY));
		}
		return PortalColor.BLUE;
	}
}
