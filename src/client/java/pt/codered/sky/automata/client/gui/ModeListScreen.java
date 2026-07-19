package pt.codered.sky.automata.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.client.bot.Mode;
import pt.codered.sky.automata.client.bot.ModeRegistry;
import pt.codered.sky.automata.client.bot.viewmodel.ModeUiViewModel;

/**
 * Mode list opened by {@code /automata ui}: one row per {@link ModeRegistry} entry, marked with
 * a {@code ●} when it's the active mode. Clicking a row browses to {@link ModeSettingsScreen}
 * for that mode via {@link ModeUiViewModel#select(String)} — it never activates a mode
 * directly, only {@link ModeSettingsScreen}'s explicit "Activate" button does.
 */
public class ModeListScreen extends Screen {
	private static final int LIST_X = 10;
	private static final int LIST_WIDTH = 100;
	private static final int ROW_HEIGHT = 22;

	private final ModeUiViewModel viewModel;

	public ModeListScreen(ModeUiViewModel viewModel) {
		super(Component.literal("Sky Automata"));
		this.viewModel = viewModel;
	}

	@Override
	protected void init() {
		int y = this.height / 2 - (viewModel.getModeIds().size() * ROW_HEIGHT) / 2;
		for (String id : viewModel.getModeIds()) {
			Mode mode = ModeRegistry.get(id);
			addRenderableWidget(Button.builder(rowLabel(id, mode), button -> {
				viewModel.select(id);
				Minecraft.getInstance().setScreen(new ModeSettingsScreen(viewModel, this));
			}).pos(LIST_X, y).size(LIST_WIDTH, 20).build());
			y += ROW_HEIGHT;
		}
	}

	private Component rowLabel(String id, Mode mode) {
		String label = mode.getName();
		if (id.equals(viewModel.getActiveId())) {
			label = "● " + label;
		}
		return Component.literal(label);
	}
}
