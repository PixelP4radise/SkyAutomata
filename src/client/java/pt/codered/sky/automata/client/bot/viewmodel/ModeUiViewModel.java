package pt.codered.sky.automata.client.bot.viewmodel;

import java.util.Collection;

import pt.codered.sky.automata.client.bot.ModeManager;
import pt.codered.sky.automata.client.bot.ModeRegistry;

/**
 * Backs the mode settings screen: tracks which mode is being <em>browsed</em>
 * ({@link #getSelectedId()}) separately from which mode is <em>active</em>
 * ({@link #getActiveId()}), so selecting a mode in the UI never activates it — only
 * {@link #activate(String)} does.
 */
public class ModeUiViewModel {
	private final ModeManager modeManager;
	private String selectedId;

	public ModeUiViewModel(ModeManager modeManager) {
		this.modeManager = modeManager;
		String activeId = ModeRegistry.idOf(modeManager.getActiveMode());
		this.selectedId = activeId != null ? activeId : ModeRegistry.ids().stream().findFirst().orElse(null);
	}

	public Collection<String> getModeIds() {
		return ModeRegistry.ids();
	}

	public String getSelectedId() {
		return selectedId;
	}

	public void select(String id) {
		this.selectedId = id;
	}

	public String getActiveId() {
		return ModeRegistry.idOf(modeManager.getActiveMode());
	}

	public void activate(String id) {
		modeManager.setMode(ModeRegistry.get(id));
	}
}
