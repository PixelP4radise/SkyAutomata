
package pt.codered.sky.automata.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import pt.codered.sky.automata.client.bot.ModeManager;

public class SkyAutomataClient implements ClientModInitializer {
	public static final ModeManager MODE_MANAGER = new ModeManager();

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> MODE_MANAGER.tick());
	}
}
