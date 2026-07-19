package pt.codered.sky.automata.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A single square icon combining move-up (top half) and move-down (bottom half), matching
 * vanilla's own server-list/resource-pack reorder control exactly — same sprites, same
 * "click top half to move up, bottom half to move down" hit-testing, both sprites blitted at
 * the identical bounding box so their own artwork (each is padded to be one quadrant of a
 * combined icon) composes correctly instead of leaving a gap between two separate icons.
 */
public final class MoveButtons extends AbstractWidget {
	/** Same sprites vanilla's Multiplayer server list uses to reorder favorite servers. */
	private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
	private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
	private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
	private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");

	private final boolean canMoveUp;
	private final boolean canMoveDown;
	private final Runnable onMoveUp;
	private final Runnable onMoveDown;

	public MoveButtons(int x, int y, int size, boolean canMoveUp, boolean canMoveDown, Runnable onMoveUp,
			Runnable onMoveDown) {
		super(x, y, size, size, Component.empty());
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
