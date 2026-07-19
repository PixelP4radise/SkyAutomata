package pt.codered.sky.automata.client.gui;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.client.bot.MultiChoiceSetting;

/**
 * Renders one {@link MultiChoiceSetting}'s options as two side-by-side, independently
 * scrollable columns — "Available" and "Selected", mirroring vanilla's resource-pack screen —
 * where clicking an entry moves it between columns, and rows in "Selected" carry a single
 * combined move-up/move-down icon ({@link MoveButtons}, matching vanilla's Multiplayer server
 * list exactly: both sprites blitted at the same square, top half moves up, bottom half moves
 * down) to reorder priority. When a list has more entries than fit, the panel shows a scrollbar
 * and the mouse wheel scrolls whichever column it's hovering, one row at a time — rows scrolled
 * out of view are simply not added as widgets (no partial-row clipping).
 *
 * <p>One instance covers a single setting and owns that setting's scroll position across screen
 * rebuilds — a screen showing more than one visible {@link MultiChoiceSetting} at once needs its
 * own instance per setting, not a shared one.
 */
public final class MultiChoiceColumns {
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
	private static final int PANEL_BACKGROUND_COLOR = 0x60000000;
	private static final int PANEL_BORDER_COLOR = 0xFF8B8B8B;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int SCROLLBAR_TRACK_COLOR = 0x40FFFFFF;
	private static final int SCROLLBAR_THUMB_COLOR = 0xFFC0C0C0;

	// Scroll position (in whole rows) for each column.
	private int availableScrollRow = 0;
	private int selectedScrollRow = 0;

	/**
	 * Builds this setting's Available/Selected column widgets starting at {@code titleY} (the
	 * baseline the column titles render at — rows start {@link #COLUMN_HEADER_GAP} below it),
	 * showing only as many rows as fit between there and {@code panelBottom}, offset by each
	 * column's own scroll position — clamped here in case the underlying lists changed size
	 * since the last scroll (e.g. an item just got toggled between columns). Returns the Y
	 * coordinate just past the rows, for the caller to lay out whatever comes next.
	 *
	 * <p>The wildcard-typed {@code MultiChoiceSetting<?>} a caller's {@code instanceof} pattern
	 * captures is capture-converted at the call site: the compiler binds {@code T} to that
	 * capture for the whole call, so every option flowing through this method is a genuinely
	 * typed {@code T} — no {@code Object}/unchecked-cast juggling needed.
	 */
	public <T> int addWidgets(MultiChoiceSetting<T> multi, Consumer<AbstractWidget> addWidget, Runnable onChange,
			int detailX, int screenWidth, int titleY, int panelBottom) {
		int rowsStartY = titleY + COLUMN_HEADER_GAP;
		ColumnLayout layout = columnLayout(detailX, screenWidth);

		List<T> available = availableOptions(multi);
		List<T> selected = selectedInScope(multi);
		int visibleRowCount = visibleRowCount(rowsStartY, panelBottom);
		availableScrollRow = clampScrollRow(availableScrollRow, available.size(), visibleRowCount);
		selectedScrollRow = clampScrollRow(selectedScrollRow, selected.size(), visibleRowCount);

		for (int row = 0; row < visibleRowCount; row++) {
			int rowY = rowsStartY + row * SETTING_ROW_HEIGHT;
			int buttonY = rowY + ROW_BUTTON_Y_OFFSET;
			int iconY = rowY + MOVE_ICON_Y_OFFSET;

			int availableIndex = availableScrollRow + row;
			if (availableIndex < available.size()) {
				T option = available.get(availableIndex);
				addWidget.accept(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					onChange.run();
				}).pos(layout.availableX(), buttonY).size(layout.availableWidth(), ROW_BUTTON_HEIGHT).build());
			}

			int selectedIndex = selectedScrollRow + row;
			if (selectedIndex < selected.size()) {
				T option = selected.get(selectedIndex);
				addWidget.accept(Button.builder(Component.literal(String.valueOf(option)), button -> {
					multi.toggle(option);
					onChange.run();
				}).pos(layout.selectedX(), buttonY).size(layout.selectedNameWidth(), ROW_BUTTON_HEIGHT).build());

				boolean canMoveUp = selectedIndex > 0;
				boolean canMoveDown = selectedIndex < selected.size() - 1;
				if (canMoveUp || canMoveDown) {
					addWidget.accept(new MoveButtons(layout.arrowX(), iconY, MOVE_ICON_SIZE, canMoveUp, canMoveDown,
							() -> {
								multi.moveUp(option);
								onChange.run();
							},
							() -> {
								multi.moveDown(option);
								onChange.run();
							}));
				}
			}
		}

		return rowsStartY + visibleRowCount * SETTING_ROW_HEIGHT;
	}

	/** Semi-transparent panel fill — draw this BEFORE widgets so it sits behind them. */
	public void renderBackground(GuiGraphics guiGraphics, int detailX, int screenWidth, int titleY, int panelBottom) {
		ColumnLayout layout = columnLayout(detailX, screenWidth);
		fillPanel(guiGraphics, layout.availableX(), layout.availableWidth(), titleY, panelBottom);
		fillPanel(guiGraphics, layout.selectedX(), layout.selectedPanelWidth(), titleY, panelBottom);
	}

	/** Border + centered title + scrollbar (if needed) — draw this AFTER widgets. */
	public void renderForeground(GuiGraphics guiGraphics, Font font, MultiChoiceSetting<?> multi, int detailX,
			int screenWidth, int titleY, int panelBottom) {
		ColumnLayout layout = columnLayout(detailX, screenWidth);
		int visibleRowCount = visibleRowCount(titleY + COLUMN_HEADER_GAP, panelBottom);
		outlinePanel(guiGraphics, font, layout.availableX(), layout.availableWidth(), titleY, panelBottom,
				"Available", availableScrollRow, availableOptions(multi).size(), visibleRowCount);
		outlinePanel(guiGraphics, font, layout.selectedX(), layout.selectedPanelWidth(), titleY, panelBottom,
				"Selected", selectedScrollRow, selectedInScope(multi).size(), visibleRowCount);
	}

	/**
	 * Scrolls whichever column the mouse is over by one row, if the cursor is within this
	 * setting's panel bounds. Returns {@code false} (letting the caller fall back to default
	 * scroll handling) if the cursor isn't over either column.
	 */
	public <T> boolean handleScroll(MultiChoiceSetting<T> multi, double mouseX, double mouseY, double scrollY,
			int detailX, int screenWidth, int titleY, int panelBottom, Runnable rebuild) {
		int direction = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;
		if (direction == 0) {
			return false;
		}

		int top = titleY - PANEL_PADDING;
		int bottom = panelBottom + PANEL_PADDING;
		if (mouseY < top || mouseY > bottom) {
			return false;
		}

		ColumnLayout layout = columnLayout(detailX, screenWidth);
		int visibleRowCount = visibleRowCount(titleY + COLUMN_HEADER_GAP, panelBottom);

		if (isWithinColumn(mouseX, layout.availableX(), layout.availableWidth())) {
			int total = availableOptions(multi).size();
			availableScrollRow = clampScrollRow(availableScrollRow + direction, total, visibleRowCount);
			rebuild.run();
			return true;
		}
		if (isWithinColumn(mouseX, layout.selectedX(), layout.selectedPanelWidth())) {
			int total = selectedInScope(multi).size();
			selectedScrollRow = clampScrollRow(selectedScrollRow + direction, total, visibleRowCount);
			rebuild.run();
			return true;
		}
		return false;
	}

	/**
	 * The Y coordinate just past this setting's actual row content, for a caller laying out
	 * settings one after another. Note the panels themselves always stretch to the caller's fixed
	 * {@code panelBottom} regardless (see {@link #renderBackground}) — this is only about where
	 * the *next* setting should start.
	 */
	public static int contentBottom(MultiChoiceSetting<?> multi, int titleY) {
		return titleY + COLUMN_HEADER_GAP + columnRowCount(multi) * SETTING_ROW_HEIGHT;
	}

	private static boolean isWithinColumn(double mouseX, int x, int width) {
		return mouseX >= x - PANEL_PADDING && mouseX <= x + width + PANEL_PADDING;
	}

	/** How many rows fit between {@code rowsStartY} and the panel's fixed bottom edge. */
	private static int visibleRowCount(int rowsStartY, int panelBottom) {
		return Math.max(1, (panelBottom - rowsStartY) / SETTING_ROW_HEIGHT);
	}

	private static int clampScrollRow(int scrollRow, int totalCount, int visibleRowCount) {
		int maxScroll = Math.max(0, totalCount - visibleRowCount);
		return Math.clamp(scrollRow, 0, maxScroll);
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
	 * are intentionally omitted here.
	 */
	private static <T> List<T> selectedInScope(MultiChoiceSetting<T> multi) {
		List<T> options = multi.getOptions();
		return multi.getValue().stream().filter(options::contains).toList();
	}

	private static int columnRowCount(MultiChoiceSetting<?> multi) {
		return Math.max(availableOptions(multi).size(), selectedInScope(multi).size());
	}

	private void fillPanel(GuiGraphics guiGraphics, int x, int width, int titleY, int panelBottom) {
		guiGraphics.fill(x - PANEL_PADDING, titleY - PANEL_PADDING, x + width + PANEL_PADDING,
				panelBottom + PANEL_PADDING, PANEL_BACKGROUND_COLOR);
	}

	private void outlinePanel(GuiGraphics guiGraphics, Font font, int x, int width, int titleY, int panelBottom,
			String title, int scrollRow, int totalCount, int visibleRowCount) {
		int left = x - PANEL_PADDING;
		int right = x + width + PANEL_PADDING;
		int top = titleY - PANEL_PADDING;
		int bottom = panelBottom + PANEL_PADDING;

		guiGraphics.renderOutline(left, top, right - left, bottom - top, PANEL_BORDER_COLOR);
		guiGraphics.drawCenteredString(font, title, x + width / 2, titleY, 0xE0E0E0);

		if (totalCount > visibleRowCount) {
			drawScrollbar(guiGraphics, right - SCROLLBAR_WIDTH - 2, top + 2, bottom - 2, scrollRow, totalCount,
					visibleRowCount);
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

	/**
	 * Column x positions and widths, shared by {@link #addWidgets} (button placement) and the
	 * render methods (panel drawing), so the panels always exactly frame the buttons they
	 * surround. Available/Selected widths are computed from the screen width (not a fixed
	 * constant) so the columns fill the space actually available rather than a small carved-out
	 * corner of it.
	 */
	private record ColumnLayout(int availableX, int availableWidth, int selectedX, int selectedNameWidth,
			int arrowX) {
		int selectedPanelWidth() {
			return arrowX + MOVE_ICON_SIZE - selectedX;
		}
	}

	private static ColumnLayout columnLayout(int detailX, int screenWidth) {
		int usableWidth = screenWidth - detailX - RIGHT_MARGIN - ARROW_GAP - MOVE_ICON_SIZE - COLUMN_GAP;
		int availableWidth = usableWidth / 2;
		int selectedNameWidth = usableWidth - availableWidth;

		int selectedX = detailX + availableWidth + COLUMN_GAP;
		int arrowX = selectedX + selectedNameWidth + ARROW_GAP;
		return new ColumnLayout(detailX, availableWidth, selectedX, selectedNameWidth, arrowX);
	}
}
