package pt.codered.sky.automata.client.bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link ModeSetting} whose value is an ordered, currently-selected subset of a fixed,
 * enumerable set of options — e.g. any number of target mob names picked from a
 * location-filtered list, in the order they were selected (first-selected = earliest in the
 * list; selection order is preserved for future priority-between-targets use). {@link
 * #getOptions()} is queried live, not cached, so an option list that depends on external state
 * (like the player's current Hypixel location) stays current across UI rebuilds.
 *
 * <p>{@link #getValue()} must never return {@code null} — an empty {@link List} means "nothing
 * selected". Implementations should treat both the list returned by {@link #getValue()} and any
 * list passed to {@link ModeSetting#setValue(Object)} as immutable snapshots; {@link
 * #toggle(Object)}, {@link #moveUp(Object)} and {@link #moveDown(Object)} never mutate either in
 * place.
 */
public interface MultiChoiceSetting<T> extends ModeSetting<List<T>> {
	List<T> getOptions();

	/**
	 * Removes {@code option} from the current selection if present, otherwise appends it to the
	 * end — preserving the relative order of the other, already-selected options.
	 */
	default void toggle(T option) {
		List<T> updated = new ArrayList<>(getValue());
		if (!updated.remove(option)) {
			updated.add(option);
		}
		setValue(List.copyOf(updated));
	}

	/**
	 * Swaps {@code option} with the option immediately before it in selection/priority order.
	 * No-op if {@code option} isn't currently selected or is already first.
	 */
	default void moveUp(T option) {
		List<T> current = new ArrayList<>(getValue());
		int index = current.indexOf(option);
		if (index > 0) {
			Collections.swap(current, index, index - 1);
			setValue(List.copyOf(current));
		}
	}

	/**
	 * Swaps {@code option} with the option immediately after it in selection/priority order.
	 * No-op if {@code option} isn't currently selected or is already last.
	 */
	default void moveDown(T option) {
		List<T> current = new ArrayList<>(getValue());
		int index = current.indexOf(option);
		if (index >= 0 && index < current.size() - 1) {
			Collections.swap(current, index, index + 1);
			setValue(List.copyOf(current));
		}
	}
}
