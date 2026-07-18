package pt.codered.sky.automata.client.bot.modes;

/**
 * The default mode: performs no primitive actions. {@link pt.codered.sky.automata.client.bot.ModeManager}
 * starts here so the FSM always has a defined active mode instead of {@code null}.
 */
public class IdleMode extends AbstractMode {
	public IdleMode() {
		super("Idle");
	}
}
