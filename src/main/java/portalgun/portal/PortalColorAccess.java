package portalgun.portal;

import qouteall.imm_ptl.core.portal.Portal;
import portalgun.PortalColor;

/**
 * Цвет портала хранится в публичном поле portalTag (String) сущности Portal,
 * которое Immersive Portals сохраняет в NBT и синхронизирует на клиент.
 * Формат: "portalgun:gun:<rgbInt>", напр. "portalgun:gun:4172277".
 */
public final class PortalColorAccess {
	private static final String PREFIX = "portalgun:";

	private PortalColorAccess() {}

	public static void set(Portal portal, int rgb) {
		portal.portalTag = PREFIX + "gun:" + rgb;
	}

	public static boolean isPortalGun(Portal portal) {
		return portal.portalTag != null && portal.portalTag.startsWith(PREFIX);
	}

	public static int getRgb(Portal portal) {
		String tag = portal.portalTag;
		if (tag != null && tag.startsWith(PREFIX)) {
			int i = tag.lastIndexOf(':');
			if (i >= 0) {
				try {
					return Integer.parseInt(tag.substring(i + 1));
				} catch (NumberFormatException ignored) {}
			}
		}
		return PortalColor.BLUE.rgb;
	}
}
