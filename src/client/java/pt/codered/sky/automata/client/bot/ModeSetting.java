package pt.codered.sky.automata.client.bot;

/**
 * A single configurable value a {@link Mode} exposes through {@link Mode#getSettings()}.
 * See {@link MultiChoiceSetting} for a setting whose value is a subset of a fixed set of options.
 */
public interface ModeSetting<T> {
	String getLabel();

	T getValue();

	void setValue(T value);
}
