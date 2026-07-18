package pt.codered.sky.automata.client.bot.modes;

import pt.codered.sky.automata.SkyAutomata;
import pt.codered.sky.automata.client.bot.Mode;
import pt.codered.sky.automata.client.bot.TaskQueue;

/**
 * Base for a named {@link Mode} that has no primitive-task behavior of its own yet — logs
 * on enter/exit so mode switches are visible, and leaves {@link #tick(TaskQueue)} a no-op
 * until real task-pushing logic is added.
 */
public abstract class AbstractMode implements Mode {
	private final String name;

	protected AbstractMode(String name) {
		this.name = name;
	}

	@Override
	public void onEnter() {
		SkyAutomata.LOGGER.info("Entering {} mode", name);
	}

	@Override
	public void onExit() {
		SkyAutomata.LOGGER.info("Exiting {} mode", name);
	}

	@Override
	public void tick(TaskQueue taskQueue) {
	}
}
