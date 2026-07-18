package pt.codered.sky.automata.client.bot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lookup of registered {@link Mode}s by id, so a future command/keybind/GUI can switch
 * {@link ModeManager}'s active mode by name without needing a compile-time reference to
 * the concrete {@link Mode} class.
 */
public class ModeRegistry {
	private static final Map<String, Mode> MODES = new LinkedHashMap<>();

	private ModeRegistry() {
	}

	public static void register(String id, Mode mode) {
		MODES.put(id, mode);
	}

	public static Mode get(String id) {
		return MODES.get(id);
	}

	public static String idOf(Mode mode) {
		for (Map.Entry<String, Mode> entry : MODES.entrySet()) {
			if (entry.getValue() == mode) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static Collection<String> ids() {
		return MODES.keySet();
	}
}
