package pt.codered.sky.automata.client.hypixel;

import java.util.List;
import java.util.Map;

/**
 * Which Hypixel Skyblock mob nameplate names ({@link MobInfo#name()}) can appear at a given
 * {@link SkyblockState#getLocation()} value — filters Combat mode's target-mob picker down to
 * mobs that can actually spawn where the player currently is.
 *
 * <p>"Crypts" is confirmed exact via a live {@code location=...} reading while standing in the
 * crypt (see {@link ScoreboardTracker}).
 */
public final class LocationMobs {
	private static final Map<String, List<String>> MOBS_BY_LOCATION = Map.of(
			"Graveyard", List.of("Graveyard Zombie", "Zombie Villager"),
			"Crypts", List.of("Crypt Ghoul"));

	private LocationMobs() {
	}

	public static List<String> forLocation(String location) {
		if (location == null) {
			return List.of();
		}
		return MOBS_BY_LOCATION.getOrDefault(location, List.of());
	}
}
