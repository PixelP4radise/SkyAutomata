package pt.codered.sky.automata.client.bot;

/**
 * A single configurable value a {@link Mode} exposes through {@link Mode#getSettings()}.
 * No concrete implementations exist yet — modes have no real settings until they have
 * real task-pushing behavior to configure.
 */
public interface ModeSetting<T> {
	String getLabel();

	T getValue();

	void setValue(T value);
}
