package portalgun.portal;

import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalColor;

/**
 * Цвет портала хранится в публичном поле portalTag (String) сущности Portal,
 * которое Immersive Portals сохраняет в NBT и синхронизирует на клиент.
 * (Раньше использовалось несуществующее поле extraData — это вызывало ошибку компиляции.)
 */
public final class PortalColorAccess {
	private static final String PREFIX = "portalgun:";

	private PortalColorAccess() {}

	public static void set(Portal portal, PortalColor color) {
		portal.portalTag = PREFIX + color.name();
	}

	public static PortalColor get(Portal portal) {
		String tag = portal.portalTag;
		if (tag != null && tag.startsWith(PREFIX)) {
			try {
				return PortalColor.valueOf(tag.substring(PREFIX.length()));
			} catch (IllegalArgumentException ignored) {}
		}
		return PortalColor.BLUE;
	}
}
