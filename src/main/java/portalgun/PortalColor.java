package portalgun;

public enum PortalColor {
	BLUE(0x3FA9F5),
	ORANGE(0xFF7A18);

	public final int rgb;
	PortalColor(int rgb) { this.rgb = rgb; }

	// Палитра цветов для перебора биндом (R). Каждый цвет = независимая пара порталов.
	public static final int[] PALETTE = {
		0x3FA9F5, // синий
		0xFF7A18, // оранжевый
		0x35E03A, // зелёный
		0xB14CFF, // фиолетовый
		0xFF3B30, // красный
		0xFFE03B, // жёлтый
		0x2EE6D6, // бирюзовый
		0xFF5CC8  // розовый
	};

	// Следующий цвет палитры после текущего (по кругу). Если цвета нет — первый.
	public static int nextColor(int rgb) {
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == rgb) {
				return PALETTE[(i + 1) % PALETTE.length];
			}
		}
		return PALETTE[0];
	}

	// Человекочитаемое имя цвета для подсказки в action bar.
	public static String nameOf(int rgb) {
		switch (rgb) {
			case 0x3FA9F5: return "синий";
			case 0xFF7A18: return "оранжевый";
			case 0x35E03A: return "зелёный";
			case 0xB14CFF: return "фиолетовый";
			case 0xFF3B30: return "красный";
			case 0xFFE03B: return "жёлтый";
			case 0x2EE6D6: return "бирюзовый";
			case 0xFF5CC8: return "розовый";
			default: return String.format("#%06X", rgb & 0xFFFFFF);
		}
	}
}
