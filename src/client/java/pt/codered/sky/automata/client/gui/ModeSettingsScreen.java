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
 * Settings detail screen for the mode {@link ModeUiViewModel#getSelectedId()} points at when
 * this screen opens, navigated to from {@link ModeListScreen}. {@link MultiChoiceSetting}
 * options are rendered via {@link MultiChoiceColumns} as two side-by-side Available/Selected
 * panels; a single explicit "Activate" button applies the mode and returns to the list, while
 * "Back"/Escape return without activating.
 */
public class ModeSettingsScreen extends Screen {
	private static final int CONTENT_X = 10;
	private static final int OPTION_WIDTH = 150;
	private static final int BUTTON_GAP = 10;
	private static final int SETTINGS_TOP_PADDING = 20;
	// Anchored to a fixed distance from the top of the screen, NOT this.height/2 — the
	// two-column panel can be several hundred pixels tall (SETTING_ROW_HEIGHT rows,
	// PANEL_BOTTOM_MARGIN clearance above Activate), so starting halfway down the screen ran out
	// of room and pushed rows past this.height entirely on smaller/high-GUI-scale windows —
	// they'd render behind/below the hotbar instead of inside their panel. A fixed top margin
	// always leaves the maximum room the current window height can offer.
	private static final int DETAIL_TOP_MARGIN = 20;
	private static final int SETTING_LABEL_GAP = 12;
	private static final int PANEL_BOTTOM_MARGIN = 50;

	private final ModeUiViewModel viewModel;
	private final Screen parent;

	// Assumes a single MultiChoiceSetting fills the panel — a mode with a second one would need
	// its own MultiChoiceColumns instance instead of sharing this one.
	private final MultiChoiceColumns columns = new MultiChoiceColumns();

	public ModeSettingsScreen(ModeUiViewModel viewModel, Screen parent) {
		super(Component.literal("Sky Automata"));
		this.viewModel = viewModel;
		this.parent = parent;
	}

	private Mode selectedMode() {
		return ModeRegistry.get(viewModel.getSelectedId());
	}

	@Override
	protected void init() {
		addSettingWidgets(selectedMode());

		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.pos(CONTENT_X, this.height - 40).size(OPTION_WIDTH, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Activate"), button -> {
			viewModel.activate(viewModel.getSelectedId());
			this.minecraft.setScreen(parent);
		}).pos(CONTENT_X + OPTION_WIDTH + BUTTON_GAP, this.height - 40).size(OPTION_WIDTH, 20).build());
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	private void addSettingWidgets(Mode selectedMode) {
		int y = detailSettingsStartY();
		for (ModeSetting<?> setting : selectedMode.getSettings()) {
			if (!(setting instanceof MultiChoiceSetting<?> multi) || multi.getOptions().isEmpty()) {
				continue;
			}
			y += SETTING_LABEL_GAP;
			y = addChoiceColumnWidgets(multi, y);
		}
	}

	/**
	 * Capture-converts the wildcard {@code MultiChoiceSetting<?>} from the caller's
	 * {@code instanceof} pattern to a genuinely typed {@code T} for the whole call, so
	 * {@link MultiChoiceColumns#addWidgets} can wire up option toggling without unchecked casts.
	 */
	private <T> int addChoiceColumnWidgets(MultiChoiceSetting<T> multi, int titleY) {
		return columns.addWidgets(multi, this::addRenderableWidget, this::rebuildWidgets, CONTENT_X, this.width,
				titleY, panelBottom());
	}

	private int panelBottom() {
		return this.height - PANEL_BOTTOM_MARGIN;
	}

	private int detailSettingsStartY() {
		return DETAIL_TOP_MARGIN + SETTINGS_TOP_PADDING;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderSettingsPanels(guiGraphics, true);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		renderSettingsPanels(guiGraphics, false);
	}

	/**
	 * Draws the settings detail panel in two passes sharing one traversal (so the layout math
	 * can never drift between them): {@code backgroundOnly} first, before widgets render (so the
	 * panel fill sits behind the buttons instead of painting over them), then again with
	 * {@code backgroundOnly = false} after widgets render, for borders/titles/labels/scrollbars
	 * that should sit on top.
	 */
	private void renderSettingsPanels(GuiGraphics guiGraphics, boolean backgroundOnly) {
		Mode selectedMode = selectedMode();

		if (!backgroundOnly) {
			guiGraphics.drawString(this.font, selectedMode.getName() + " settings", CONTENT_X, DETAIL_TOP_MARGIN,
					0xFFFFFF);
		}

		if (selectedMode.getSettings().isEmpty()) {
			if (!backgroundOnly) {
				guiGraphics.drawString(this.font, "No settings yet for " + selectedMode.getName() + ".", CONTENT_X,
						DETAIL_TOP_MARGIN + 14, 0xA0A0A0);
			}
			return;
		}

		int y = detailSettingsStartY();
		for (ModeSetting<?> setting : selectedMode.getSettings()) {
			if (!(setting instanceof MultiChoiceSetting<?> multi)) {
				continue;
			}
			if (multi.getOptions().isEmpty()) {
				if (!backgroundOnly) {
					guiGraphics.drawString(this.font,
							"No " + setting.getLabel().toLowerCase() + " known for this location.", CONTENT_X, y,
							0xA0A0A0);
				}
				continue;
			}
			if (!backgroundOnly) {
				guiGraphics.drawString(this.font, setting.getLabel() + ":", CONTENT_X, y, 0xFFFFFF);
			}
			y += SETTING_LABEL_GAP;

			int titleY = y;
			if (backgroundOnly) {
				columns.renderBackground(guiGraphics, CONTENT_X, this.width, titleY, panelBottom());
			} else {
				columns.renderForeground(guiGraphics, this.font, multi, CONTENT_X, this.width, titleY,
						panelBottom());
			}

			// Panels stretch to a fixed height well below the actual rows (rows stay
			// top-aligned within them) rather than tightly wrapping the row count, so the
			// column looks like a real list panel instead of a shrink-wrapped box. Assumes a
			// single setting fills the panel; a second MultiChoiceSetting would currently start
			// right after this panel's true (small) row content, not its inflated visual bottom.
			y = MultiChoiceColumns.contentBottom(multi, titleY);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Mode selectedMode = selectedMode();
		for (ModeSetting<?> setting : selectedMode.getSettings()) {
			if (setting instanceof MultiChoiceSetting<?> multi && !multi.getOptions().isEmpty()
					&& handleColumnScroll(multi, mouseX, mouseY, scrollY)) {
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private <T> boolean handleColumnScroll(MultiChoiceSetting<T> multi, double mouseX, double mouseY,
			double scrollY) {
		int titleY = detailSettingsStartY() + SETTING_LABEL_GAP;
		return columns.handleScroll(multi, mouseX, mouseY, scrollY, CONTENT_X, this.width, titleY, panelBottom(),
				this::rebuildWidgets);
	}
}
