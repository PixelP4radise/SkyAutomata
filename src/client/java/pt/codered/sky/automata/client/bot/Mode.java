package pt.codered.sky.automata.client.bot;

/**
 * A single state in the bot's Hierarchical Finite State Machine (e.g. Lumbering, Hunting,
 * Farming). A mode drives behavior by pushing primitive {@link Task}s onto the
 * {@link TaskQueue} — it should check {@link TaskQueue#isIdle()} before pushing its next
 * action rather than pushing on every tick.
 */
public interface Mode {
	default void onEnter() {
	}

	default void onExit() {
	}

	void tick(TaskQueue taskQueue);
}
