package pt.codered.sky.automata.client.gui;

import java.util.List;

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
 *
 * <p>{@link MultiChoiceSetting} options are rendered as two side-by-side columns —
 * "Available" and "Selected", mirroring vanilla's resource-pack screen — where clicking an
 * entry moves it between columns, and rows in "Selected" carry ▲/▼ buttons to reorder
 * priority.
 */
public class ModeScreen extends Screen {
	private static final int LIST_WIDTH = 100;
	private static final int ROW_HEIGHT = 22;
	private static final int DETAIL_X_OFFSET = LIST_WIDTH + 30;
	private static final int OPTION_WIDTH = 150;
	private static final int SETTINGS_TOP_PADDING = 20;
	private static final int SETTING_LABEL_GAP = 12;

	private static final int AVAILABLE_WIDTH = 110;
	private static final int SELECTED_NAME_WIDTH = 90;
	private static final int ARROW_WIDTH = 18;
	private static final int ARROW_GAP = 2;
	private static final int COLUMN_GAP = 16;
	private static final int COLUMN_HEADER_GAP = 12;

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
			y += SETTING_LABEL_GAP + COLUMN_HEADER_GAP;
			y = addChoiceColumnWidgets(multi, detailX, y);
		}
	}

	/**
	 * Builds one setting's Available/Selected columns starting at {@code startY}, and returns
	 * the y position immediately below the taller of the two columns.
	 *
	 * <p>The wildcard-typed {@code MultiChoiceSetting<?>} captured by the {@code instanceof}
	 * pattern in the caller is capture-converted here: the compiler binds {@code T} to that
	 * capture for the whole call, so every option flowing through this method is a genuinely
	 * typed {@code T} — no {@code Object}/unchecked-cast juggling needed.
	 */
	private <T> int addChoiceColumnWidgets(MultiChoiceSetting<T> multi, int detailX, int startY) {
		int availableX = detailX;
		int selectedX = detailX + AVAILABLE_WIDTH + COLUMN_GAP;
		int upArrowX = selectedX + SELECTED_NAME_WIDTH + ARROW_GAP;
		int downArrowX = upArrowX + ARROW_WIDTH + ARROW_GAP;

		List<T> available = availableOptions(multi);
		List<T> selected = selectedInScope(multi);
		int rowCount = Math.max(available.size(), selected.size());

		for (int row = 0; row < rowCount; row++) {
			int rowY = startY + row * ROW_HEIGHT;

			if (row < available.size()) {
				T option = available.get(row);
				addRenderableWidget(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					this.rebuildWidgets();
				}).pos(availableX, rowY).size(AVAILABLE_WIDTH, 20).build());
			}

			if (row < selected.size()) {
				T option = selected.get(row);
				addRenderableWidget(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					this.rebuildWidgets();
				}).pos(selectedX, rowY).size(SELECTED_NAME_WIDTH, 20).build());

				if (row > 0) {
					addRenderableWidget(Button.builder(Component.literal("▲"), button -> {
						multi.moveUp(option);
						this.rebuildWidgets();
					}).pos(upArrowX, rowY).size(ARROW_WIDTH, 20).build());
				}
				if (row < selected.size() - 1) {
					addRenderableWidget(Button.builder(Component.literal("▼"), button -> {
						multi.moveDown(option);
						this.rebuildWidgets();
					}).pos(downArrowX, rowY).size(ARROW_WIDTH, 20).build());
				}
			}
		}

		return startY + rowCount * ROW_HEIGHT;
	}

	/** Options not currently selected, in {@link MultiChoiceSetting#getOptions()} order. */
	private static <T> List<T> availableOptions(MultiChoiceSetting<T> multi) {
		List<T> selected = multi.getValue();
		return multi.getOptions().stream().filter(option -> !selected.contains(option)).toList();
	}

	/**
	 * Selected options that are still valid for the current option list, in
	 * selection/priority order. An option can be selected but no longer present in
	 * {@link MultiChoiceSetting#getOptions()} (e.g. after a location change) — such options
	 * are intentionally omitted here, matching the previous single-column behavior.
	 */
	private static <T> List<T> selectedInScope(MultiChoiceSetting<T> multi) {
		List<T> options = multi.getOptions();
		return multi.getValue().stream().filter(options::contains).toList();
	}

	/**
	 * Row count shared by {@link #addChoiceColumnWidgets} and {@link #render}, so their
	 * vertical layout math can't drift apart.
	 */
	private static int columnRowCount(MultiChoiceSetting<?> multi) {
		return Math.max(availableOptions(multi).size(), selectedInScope(multi).size());
	}

	private int detailSettingsStartY() {
		return this.height / 2 - 40 + SETTINGS_TOP_PADDING;
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
			y += SETTING_LABEL_GAP;

			int selectedX = detailX + AVAILABLE_WIDTH + COLUMN_GAP;
			guiGraphics.drawString(this.font, "Available", detailX, y, 0xA0A0A0);
			guiGraphics.drawString(this.font, "Selected", selectedX, y, 0xA0A0A0);
			y += COLUMN_HEADER_GAP;

			y += columnRowCount(multi) * ROW_HEIGHT;
		}
	}
}
