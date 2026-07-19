package pt.codered.sky.automata.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
 * <p>{@link MultiChoiceSetting} options are rendered as two side-by-side, independently
 * scrollable columns — "Available" and "Selected", mirroring vanilla's resource-pack screen,
 * sized to fill the available screen width — where clicking an entry moves it between columns,
 * and rows in "Selected" carry a single combined move-up/move-down icon ({@link MoveButtons},
 * matching vanilla's Multiplayer server list exactly: both sprites blitted at the same square,
 * top half moves up, bottom half moves down) to reorder priority. Each column's panel has a
 * fixed height (see {@link #panelBottom()}); when a list has more entries than fit, the panel
 * shows a scrollbar and the mouse wheel scrolls whichever column it's hovering, one row at a
 * time — rows scrolled out of view are simply not added as widgets (no partial-row clipping).
 */
public class ModeScreen extends Screen {
	private static final int LIST_WIDTH = 100;
	private static final int ROW_HEIGHT = 22;
	private static final int DETAIL_X_OFFSET = LIST_WIDTH + 30;
	private static final int OPTION_WIDTH = 150;
	private static final int SETTINGS_TOP_PADDING = 20;
	// Anchored to a fixed distance from the top of the screen, NOT this.height/2 — the
	// two-column panel can be several hundred pixels tall (SETTING_ROW_HEIGHT rows,
	// PANEL_BOTTOM_MARGIN clearance above Activate), so starting halfway down the screen ran out
	// of room and pushed rows past this.height entirely on smaller/high-GUI-scale windows —
	// they'd render behind/below the hotbar instead of inside their panel. A fixed top margin
	// always leaves the maximum room the current window height can offer.
	private static final int DETAIL_TOP_MARGIN = 20;
	private static final int SETTING_LABEL_GAP = 12;

	private static final int RIGHT_MARGIN = 10;
	private static final int ARROW_GAP = 2;
	private static final int COLUMN_GAP = 16;
	private static final int COLUMN_HEADER_GAP = 12;
	// Name buttons (Available/Selected) stay this small regardless of arrow icon size — the two
	// are sized independently (see MOVE_ICON_SIZE) so making the icon bigger never widens/grows
	// the selection buttons themselves.
	private static final int ROW_BUTTON_HEIGHT = 20;
	private static final int ROW_GAP = 3;
	// One combined icon square — matching vanilla's real design, where up/down are the top/bottom
	// halves of ONE icon, not two separate icons. Two separate ImageButtons (tried first) could
	// never look right here: each server_list sprite PNG carries its own internal padding
	// (designed to be one quadrant of a combined icon, not a standalone glyph), so their bounding
	// boxes could touch exactly while the visible triangles still looked separated by a gap.
	// Blitting both sprites at the identical square — same as vanilla's own Gui/Entry render code
	// — lets their own transparency compose correctly, with no artificial gap and the boundary
	// between them exactly at the row's center, REGARDLESS of the square's size — that fix was
	// about the rendering technique, not about making the icon bigger. Matches ROW_BUTTON_HEIGHT
	// so the row pitch (SETTING_ROW_HEIGHT) and column widths (usableWidth in columnLayout, which
	// reserves MOVE_ICON_SIZE off the available width) go back to their original, smaller sizing.
	private static final int MOVE_ICON_SIZE = ROW_BUTTON_HEIGHT;
	// The row's actual content band is whichever of the two is taller; both the name button and
	// the icon are centered within it, so their centers line up with each other and with the row.
	private static final int ROW_CONTENT_HEIGHT = Math.max(ROW_BUTTON_HEIGHT, MOVE_ICON_SIZE);
	private static final int SETTING_ROW_HEIGHT = ROW_CONTENT_HEIGHT + ROW_GAP;
	private static final int ROW_BUTTON_Y_OFFSET = (ROW_CONTENT_HEIGHT - ROW_BUTTON_HEIGHT) / 2;
	private static final int MOVE_ICON_Y_OFFSET = (ROW_CONTENT_HEIGHT - MOVE_ICON_SIZE) / 2;
	private static final int PANEL_PADDING = 4;
	private static final int PANEL_BOTTOM_MARGIN = 50;
	private static final int PANEL_BACKGROUND_COLOR = 0x60000000;
	private static final int PANEL_BORDER_COLOR = 0xFF8B8B8B;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int SCROLLBAR_TRACK_COLOR = 0x40FFFFFF;
	private static final int SCROLLBAR_THUMB_COLOR = 0xFFC0C0C0;

	/** Same sprites vanilla's Multiplayer server list uses to reorder favorite servers. */
	private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
	private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
	private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
	private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");

	private final ModeUiViewModel viewModel;

	// Scroll position (in whole rows) for each column. Assumes a single MultiChoiceSetting fills
	// the panel (matches the same assumption already made elsewhere in this class) — a second
	// setting would need its own pair of these instead of one shared pair.
	private int availableScrollRow = 0;
	private int selectedScrollRow = 0;

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
	 * Builds one setting's Available/Selected columns starting at {@code startY}, showing only
	 * as many rows as fit in the fixed-height panel ({@link #visibleRowCount}), offset by each
	 * column's own scroll position — clamped here in case the underlying lists changed size
	 * since the last scroll (e.g. an item just got toggled between columns).
	 *
	 * <p>The wildcard-typed {@code MultiChoiceSetting<?>} captured by the {@code instanceof}
	 * pattern in the caller is capture-converted here: the compiler binds {@code T} to that
	 * capture for the whole call, so every option flowing through this method is a genuinely
	 * typed {@code T} — no {@code Object}/unchecked-cast juggling needed.
	 */
	private <T> int addChoiceColumnWidgets(MultiChoiceSetting<T> multi, int detailX, int startY) {
		ColumnLayout layout = columnLayout(detailX, this.width);

		List<T> available = availableOptions(multi);
		List<T> selected = selectedInScope(multi);
		int visibleRowCount = visibleRowCount(startY);
		availableScrollRow = clampScrollRow(availableScrollRow, available.size(), visibleRowCount);
		selectedScrollRow = clampScrollRow(selectedScrollRow, selected.size(), visibleRowCount);

		for (int row = 0; row < visibleRowCount; row++) {
			int rowY = startY + row * SETTING_ROW_HEIGHT;
			int buttonY = rowY + ROW_BUTTON_Y_OFFSET;
			int iconY = rowY + MOVE_ICON_Y_OFFSET;

			int availableIndex = availableScrollRow + row;
			if (availableIndex < available.size()) {
				T option = available.get(availableIndex);
				addRenderableWidget(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					this.rebuildWidgets();
				}).pos(layout.availableX(), buttonY).size(layout.availableWidth(), ROW_BUTTON_HEIGHT).build());
			}

			int selectedIndex = selectedScrollRow + row;
			if (selectedIndex < selected.size()) {
				T option = selected.get(selectedIndex);
				addRenderableWidget(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					this.rebuildWidgets();
				}).pos(layout.selectedX(), buttonY).size(layout.selectedNameWidth(), ROW_BUTTON_HEIGHT).build());

				boolean canMoveUp = selectedIndex > 0;
				boolean canMoveDown = selectedIndex < selected.size() - 1;
				if (canMoveUp || canMoveDown) {
					addRenderableWidget(new MoveButtons(layout.arrowX(), iconY, canMoveUp, canMoveDown,
							() -> {
								multi.moveUp(option);
								this.rebuildWidgets();
							},
							() -> {
								multi.moveDown(option);
								this.rebuildWidgets();
							}));
				}
			}
		}

		return startY + visibleRowCount * SETTING_ROW_HEIGHT;
	}

	/** How many rows fit between {@code rowsStartY} and the panel's fixed bottom edge. */
	private int visibleRowCount(int rowsStartY) {
		return Math.max(1, (panelBottom() - rowsStartY) / SETTING_ROW_HEIGHT);
	}

	private int panelBottom() {
		return this.height - PANEL_BOTTOM_MARGIN;
	}

	private static int clampScrollRow(int scrollRow, int totalCount, int visibleRowCount) {
		int maxScroll = Math.max(0, totalCount - visibleRowCount);
		return Math.max(0, Math.min(scrollRow, maxScroll));
	}

	/**
	 * A single square icon combining move-up (top half) and move-down (bottom half), matching
	 * vanilla's own server-list/resource-pack reorder control exactly — same sprites, same
	 * "click top half to move up, bottom half to move down" hit-testing, both sprites blitted at
	 * the identical bounding box so their own artwork (each is padded to be one quadrant of a
	 * combined icon) composes correctly instead of leaving a gap between two separate icons.
	 */
	private final class MoveButtons extends AbstractWidget {
		private final boolean canMoveUp;
		private final boolean canMoveDown;
		private final Runnable onMoveUp;
		private final Runnable onMoveDown;

		MoveButtons(int x, int y, boolean canMoveUp, boolean canMoveDown, Runnable onMoveUp, Runnable onMoveDown) {
			super(x, y, MOVE_ICON_SIZE, MOVE_ICON_SIZE, Component.empty());
			this.canMoveUp = canMoveUp;
			this.canMoveDown = canMoveDown;
			this.onMoveUp = onMoveUp;
			this.onMoveDown = onMoveDown;
		}

		private boolean hoveringTopHalf(int mouseX, int mouseY) {
			return isMouseOver(mouseX, mouseY) && mouseY < getY() + getHeight() / 2;
		}

		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			boolean hoverTop = hoveringTopHalf(mouseX, mouseY);
			if (canMoveUp) {
				Identifier sprite = hoverTop ? MOVE_UP_HIGHLIGHTED_SPRITE : MOVE_UP_SPRITE;
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
			}
			if (canMoveDown) {
				Identifier sprite = !hoverTop && isMouseOver(mouseX, mouseY) ? MOVE_DOWN_HIGHLIGHTED_SPRITE : MOVE_DOWN_SPRITE;
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
			}
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (event.y() < getY() + getHeight() / 2.0) {
				if (canMoveUp) {
					onMoveUp.run();
				}
			} else if (canMoveDown) {
				onMoveDown.run();
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/**
	 * Column x positions and widths, shared by {@link #addChoiceColumnWidgets} (button
	 * placement) and {@link #drawColumnBackgrounds}/{@link #drawColumnBordersAndTitles} (panel
	 * drawing), so the panels always exactly frame the buttons they surround. Available/Selected
	 * widths are computed from the screen width (not a fixed constant) so the columns fill the
	 * space actually available rather than a small carved-out corner of it.
	 */
	private record ColumnLayout(int availableX, int availableWidth, int selectedX, int selectedNameWidth, int arrowX) {
		int selectedPanelWidth() {
			return arrowX + MOVE_ICON_SIZE - selectedX;
		}
	}

	private static ColumnLayout columnLayout(int detailX, int screenWidth) {
		int usableWidth = screenWidth - detailX - RIGHT_MARGIN - ARROW_GAP - MOVE_ICON_SIZE - COLUMN_GAP;
		int availableWidth = usableWidth / 2;
		int selectedNameWidth = usableWidth - availableWidth;

		int availableX = detailX;
		int selectedX = detailX + availableWidth + COLUMN_GAP;
		int arrowX = selectedX + selectedNameWidth + ARROW_GAP;
		return new ColumnLayout(availableX, availableWidth, selectedX, selectedNameWidth, arrowX);
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
		return DETAIL_TOP_MARGIN + SETTINGS_TOP_PADDING;
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

	/** Semi-transparent panel fill — drawn in a pass BEFORE widgets, so it sits behind them. */
	private void drawColumnBackgrounds(GuiGraphics guiGraphics, int detailX, int headerY, int rowsBottom) {
		ColumnLayout layout = columnLayout(detailX, this.width);
		fillPanel(guiGraphics, layout.availableX(), layout.availableWidth(), headerY, rowsBottom);
		fillPanel(guiGraphics, layout.selectedX(), layout.selectedPanelWidth(), headerY, rowsBottom);
	}

	private void fillPanel(GuiGraphics guiGraphics, int x, int width, int headerY, int rowsBottom) {
		guiGraphics.fill(x - PANEL_PADDING, headerY - PANEL_PADDING, x + width + PANEL_PADDING,
				rowsBottom + PANEL_PADDING, PANEL_BACKGROUND_COLOR);
	}

	/** Border + centered title + scrollbar (if needed) — drawn in a pass AFTER widgets. */
	private void drawColumnBordersAndTitles(GuiGraphics guiGraphics, int detailX, int headerY, int rowsBottom,
			int availableCount, int selectedCount, int visibleRowCount) {
		ColumnLayout layout = columnLayout(detailX, this.width);
		outlinePanel(guiGraphics, layout.availableX(), layout.availableWidth(), headerY, rowsBottom, "Available",
				availableScrollRow, availableCount, visibleRowCount);
		outlinePanel(guiGraphics, layout.selectedX(), layout.selectedPanelWidth(), headerY, rowsBottom, "Selected",
				selectedScrollRow, selectedCount, visibleRowCount);
	}

	private void outlinePanel(GuiGraphics guiGraphics, int x, int width, int headerY, int rowsBottom, String title,
			int scrollRow, int totalCount, int visibleRowCount) {
		int left = x - PANEL_PADDING;
		int right = x + width + PANEL_PADDING;
		int top = headerY - PANEL_PADDING;
		int bottom = rowsBottom + PANEL_PADDING;

		guiGraphics.renderOutline(left, top, right - left, bottom - top, PANEL_BORDER_COLOR);
		guiGraphics.drawCenteredString(this.font, title, x + width / 2, headerY, 0xE0E0E0);

		if (totalCount > visibleRowCount) {
			drawScrollbar(guiGraphics, right - SCROLLBAR_WIDTH - 2, top + 2, bottom - 2, scrollRow, totalCount, visibleRowCount);
		}
	}

	private void drawScrollbar(GuiGraphics guiGraphics, int x, int top, int bottom, int scrollRow, int totalCount,
			int visibleRowCount) {
		guiGraphics.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLLBAR_TRACK_COLOR);

		int trackHeight = bottom - top;
		int thumbHeight = Math.max(10, trackHeight * visibleRowCount / totalCount);
		int maxScroll = totalCount - visibleRowCount;
		int thumbY = top + (maxScroll <= 0 ? 0 : (trackHeight - thumbHeight) * scrollRow / maxScroll);
		guiGraphics.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_THUMB_COLOR);
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
		String selectedId = viewModel.getSelectedId();
		if (selectedId == null) {
			return;
		}
		Mode selectedMode = ModeRegistry.get(selectedId);
		int detailX = 10 + DETAIL_X_OFFSET;
		int detailY = DETAIL_TOP_MARGIN;

		if (!backgroundOnly) {
			guiGraphics.drawString(this.font, selectedMode.getName() + " settings", detailX, detailY, 0xFFFFFF);
		}

		if (selectedMode.getSettings().isEmpty()) {
			if (!backgroundOnly) {
				guiGraphics.drawString(this.font, "No settings yet for " + selectedMode.getName() + ".", detailX,
						detailY + 14, 0xA0A0A0);
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
							"No " + setting.getLabel().toLowerCase() + " known for this location.", detailX, y, 0xA0A0A0);
				}
				continue;
			}
			if (!backgroundOnly) {
				guiGraphics.drawString(this.font, setting.getLabel() + ":", detailX, y, 0xFFFFFF);
			}
			y += SETTING_LABEL_GAP;

			int headerY = y;
			int rowsStartY = headerY + COLUMN_HEADER_GAP;
			int rowsBottom = y + COLUMN_HEADER_GAP + columnRowCount(multi) * SETTING_ROW_HEIGHT;
			// Panels stretch to a fixed height well below the actual rows (rows stay
			// top-aligned within them) rather than tightly wrapping the row count, so the
			// column looks like a real list panel instead of a shrink-wrapped box. Assumes a
			// single setting fills the panel; a second MultiChoiceSetting would currently start
			// right after this panel's true (small) row content, not its inflated visual bottom.
			int panelBottom = panelBottom();
			if (backgroundOnly) {
				drawColumnBackgrounds(guiGraphics, detailX, headerY, panelBottom);
			} else {
				drawColumnBordersAndTitles(guiGraphics, detailX, headerY, panelBottom,
						availableOptions(multi).size(), selectedInScope(multi).size(), visibleRowCount(rowsStartY));
			}

			y = rowsBottom;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		String selectedId = viewModel.getSelectedId();
		if (selectedId != null) {
			Mode selectedMode = ModeRegistry.get(selectedId);
			for (ModeSetting<?> setting : selectedMode.getSettings()) {
				if (setting instanceof MultiChoiceSetting<?> multi && !multi.getOptions().isEmpty()
						&& handleColumnScroll(multi, mouseX, mouseY, scrollY)) {
					return true;
				}
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/**
	 * Scrolls whichever column the mouse is over by one row, if the cursor is within this
	 * (single, assumed) setting's panel bounds. Returns {@code false} (letting the default
	 * scroll handling run instead) if the cursor isn't over either column.
	 */
	private <T> boolean handleColumnScroll(MultiChoiceSetting<T> multi, double mouseX, double mouseY, double scrollY) {
		int direction = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;
		if (direction == 0) {
			return false;
		}

		int detailX = 10 + DETAIL_X_OFFSET;
		int headerY = detailSettingsStartY() + SETTING_LABEL_GAP;
		int rowsStartY = headerY + COLUMN_HEADER_GAP;
		int top = headerY - PANEL_PADDING;
		int bottom = panelBottom() + PANEL_PADDING;
		if (mouseY < top || mouseY > bottom) {
			return false;
		}

		ColumnLayout layout = columnLayout(detailX, this.width);
		int visibleRowCount = visibleRowCount(rowsStartY);

		if (isWithinColumn(mouseX, layout.availableX(), layout.availableWidth())) {
			int total = availableOptions(multi).size();
			availableScrollRow = clampScrollRow(availableScrollRow + direction, total, visibleRowCount);
			this.rebuildWidgets();
			return true;
		}
		if (isWithinColumn(mouseX, layout.selectedX(), layout.selectedPanelWidth())) {
			int total = selectedInScope(multi).size();
			selectedScrollRow = clampScrollRow(selectedScrollRow + direction, total, visibleRowCount);
			this.rebuildWidgets();
			return true;
		}
		return false;
	}

	private static boolean isWithinColumn(double mouseX, int x, int width) {
		return mouseX >= x - PANEL_PADDING && mouseX <= x + width + PANEL_PADDING;
	}
}
