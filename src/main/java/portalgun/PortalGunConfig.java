package portalgun;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * §13: конфиг мода в config/portalgun.json.
 * Пока хранит флаг отключения поворота камеры при переходе через портал.
 * Файл создаётся автоматически при первом запуске; правится вручную либо
 * (в следующем раунде) через экран настроек мода.
 */
public final class PortalGunConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static PortalGunConfig INSTANCE;

	// true (по умолчанию) — камера разворачивается как обычно; false — взгляд не крутит.
	public boolean rotateCameraOnTeleport = true;

	public static PortalGunConfig get() {
		if (INSTANCE == null) INSTANCE = load();
		return INSTANCE;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("portalgun.json");
	}

	private static PortalGunConfig load() {
		Path p = path();
		try {
			if (Files.exists(p)) {
				PortalGunConfig c = GSON.fromJson(Files.readString(p), PortalGunConfig.class);
				if (c != null) return c;
			}
		} catch (Exception ignored) {}
		PortalGunConfig c = new PortalGunConfig();
		c.save();
		return c;
	}

	public void save() {
		try {
			Files.writeString(path(), GSON.toJson(this));
		} catch (Exception ignored) {}
	}
}
