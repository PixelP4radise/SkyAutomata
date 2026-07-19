package pt.codered.sky.automata.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.Minecraft;

import pt.codered.sky.automata.SkyAutomata;
import pt.codered.sky.automata.client.bot.ModeRegistry;
import pt.codered.sky.automata.client.bot.viewmodel.ModeUiViewModel;
import pt.codered.sky.automata.client.gui.ModeListScreen;

/**
 * Registers {@code /automata <mode>}, with one subcommand per id in {@link ModeRegistry} —
 * translates player input into a {@link pt.codered.sky.automata.client.bot.ModeManager}
 * mode switch. New modes only need registering in {@link ModeRegistry}; the command tree
 * picks them up automatically.
 */
public final class ModeCommands {
	private ModeCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("automata");
			for (String id : ModeRegistry.ids()) {
				root.then(ClientCommandManager.literal(id).executes(context -> {
					SkyAutomataClient.MODE_MANAGER.setMode(ModeRegistry.get(id));
					return Command.SINGLE_SUCCESS;
				}));
			}
			root.then(ClientCommandManager.literal("ui").executes(context -> {
				// ChatScreen closes itself (setScreen(null)) right after a command runs, in the
				// same synchronous call stack. Minecraft.execute() does NOT defer this when
				// called from the render thread (it only queues cross-thread calls; same-thread
				// ones run immediately), so genuinely defer via SkyAutomataClient.runNextTick,
				// which only fires on the next END_CLIENT_TICK.
				SkyAutomataClient.runNextTick(() -> {
					try {
						Minecraft.getInstance().setScreen(new ModeListScreen(new ModeUiViewModel(SkyAutomataClient.MODE_MANAGER)));
					} catch (Exception e) {
						SkyAutomata.LOGGER.error("Failed to open ModeListScreen", e);
					}
				});
				return Command.SINGLE_SUCCESS;
			}));
			dispatcher.register(root);
		});
	}
}
