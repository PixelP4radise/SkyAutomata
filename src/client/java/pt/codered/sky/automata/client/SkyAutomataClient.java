
package pt.codered.sky.automata.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import pt.codered.sky.automata.client.bot.ModeManager;
import pt.codered.sky.automata.client.bot.ModeRegistry;
import pt.codered.sky.automata.client.bot.modes.CombatMode;
import pt.codered.sky.automata.client.bot.modes.FarmingMode;
import pt.codered.sky.automata.client.bot.modes.FishingMode;
import pt.codered.sky.automata.client.bot.modes.ForagingMode;
import pt.codered.sky.automata.client.bot.modes.IdleMode;
import pt.codered.sky.automata.client.bot.modes.MiningMode;

public class SkyAutomataClient implements ClientModInitializer {
	public static final ModeManager MODE_MANAGER = new ModeManager();

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModeRegistry.register("idle", new IdleMode());
		ModeRegistry.register("farming", new FarmingMode());
		ModeRegistry.register("mining", new MiningMode());
		ModeRegistry.register("combat", new CombatMode());
		ModeRegistry.register("foraging", new ForagingMode());
		ModeRegistry.register("fishing", new FishingMode());

		MODE_MANAGER.addListener((previous, next) -> {
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				player.displayClientMessage(Component.literal("Sky Automata: " + next.getName() + " mode"), false);
			}
		});

		MODE_MANAGER.setMode(ModeRegistry.get("idle"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> MODE_MANAGER.tick());
	}
}
