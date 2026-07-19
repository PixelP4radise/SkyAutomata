package pt.codered.sky.automata.client.bot;

import java.util.List;

/**
 * A {@link ModeSetting} whose value must be one of a fixed, enumerable set of options — e.g.
 * picking a single target mob name from a location-filtered list. {@link #getOptions()} is
 * queried live, not cached, so an option list that depends on external state (like the player's
 * current Hypixel location) stays current across UI rebuilds.
 */
public interface ChoiceSetting<T> extends ModeSetting<T> {
	List<T> getOptions();
}
