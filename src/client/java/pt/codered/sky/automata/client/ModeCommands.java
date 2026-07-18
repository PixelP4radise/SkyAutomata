package pt.codered.sky.automata.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import pt.codered.sky.automata.client.bot.ModeRegistry;

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
			dispatcher.register(root);
		});
	}
}
