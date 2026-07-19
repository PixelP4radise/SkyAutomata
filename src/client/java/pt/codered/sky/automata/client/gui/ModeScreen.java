package pt.codered.sky.automata.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.client.bot.Mode;
import pt.codered.sky.automata.client.bot.ModeRegistry;
import pt.codered.sky.automata.client.bot.ModeSetting;
import pt.codered.sky.automata.client.bot.MultiChoiceSetting;
import pt.codered.sky.automata.client.bot.viewmodel.ModeUiViewModel;

/**
 * Master-detail screen opened by {@code /automata ui}: a mode list on the left (clicking
 * only selects/browses via {@link ModeUiViewModel#select(String)} — it never activates),
 * and the selected mode's settings on the right with a single explicit "Activate" button.
 */
public class ModeScreen extends Screen {
	private static final int LIST_WIDTH = 100;
	private static final int ROW_HEIGHT = 22;
	private static final int DETAIL_X_OFFSET = LIST_WIDTH + 30;
	private static final int OPTION_WIDTH = 150;
	private static final int SETTINGS_TOP_PADDING = 20;
	private static final int SETTING_LABEL_GAP = 12;

	private final ModeUiViewModel viewModel;

	public ModeScreen(ModeUiViewModel viewModel) {
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
				this.rebuildWidgets();
			}).pos(10, y).size(LIST_WIDTH, 20).build());
			y += ROW_HEIGHT;
		}

		String selectedId = viewModel.getSelectedId();
		if (selectedId != null) {
			addSettingWidgets(ModeRegistry.get(selectedId));
			addRenderableWidget(Button.builder(Component.literal("Activate"), button -> {
				viewModel.activate(selectedId);
				this.rebuildWidgets();
			}).pos(10 + DETAIL_X_OFFSET, this.height - 40).size(OPTION_WIDTH, 20).build());
		}
	}

	private void addSettingWidgets(Mode selectedMode) {
		int detailX = 10 + DETAIL_X_OFFSET;
		int y = detailSettingsStartY();
		for (ModeSetting<?> setting : selectedMode.getSettings()) {
			if (!(setting instanceof MultiChoiceSetting<?> multi) || multi.getOptions().isEmpty()) {
				continue;
			}
			y += SETTING_LABEL_GAP;
			for (Object option : multi.getOptions()) {
				boolean selected = multi.getValue().contains(option);
				String label = selected ? "[" + option + "]" : String.valueOf(option);
				addRenderableWidget(Button.builder(Component.literal(label), button -> {
					toggleUnchecked(multi, option);
					this.rebuildWidgets();
				}).pos(detailX, y).size(OPTION_WIDTH, 20).build());
				y += ROW_HEIGHT;
			}
		}
	}

	private int detailSettingsStartY() {
		return this.height / 2 - 40 + SETTINGS_TOP_PADDING;
	}

	@SuppressWarnings("unchecked")
	private static <T> void toggleUnchecked(MultiChoiceSetting<T> multi, Object option) {
		multi.toggle((T) option);
	}

	private Component rowLabel(String id, Mode mode) {
		String label = mode.getName();
		if (id.equals(viewModel.getActiveId())) {
			label = "● " + label;
		}
		if (id.equals(viewModel.getSelectedId())) {
			label = "[" + label + "]";
		}
		return Component.literal(label);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		String selectedId = viewModel.getSelectedId();
		if (selectedId == null) {
			return;
		}
		Mode selectedMode = ModeRegistry.get(selectedId);
		int detailX = 10 + DETAIL_X_OFFSET;
		int detailY = this.height / 2 - 40;
		guiGraphics.drawString(this.font, selectedMode.getName() + " settings", detailX, detailY, 0xFFFFFF);

		if (selectedMode.getSettings().isEmpty()) {
			guiGraphics.drawString(this.font, "No settings yet for " + selectedMode.getName() + ".", detailX,
					detailY + 14, 0xA0A0A0);
			return;
		}

		int y = detailSettingsStartY();
		for (ModeSetting<?> setting : selectedMode.getSettings()) {
			if (!(setting instanceof MultiChoiceSetting<?> multi)) {
				continue;
			}
			if (multi.getOptions().isEmpty()) {
				guiGraphics.drawString(this.font, "No " + setting.getLabel().toLowerCase() + " known for this location.",
						detailX, y, 0xA0A0A0);
				continue;
			}
			guiGraphics.drawString(this.font, setting.getLabel() + ":", detailX, y, 0xFFFFFF);
			y += SETTING_LABEL_GAP + multi.getOptions().size() * ROW_HEIGHT;
		}
	}
}
