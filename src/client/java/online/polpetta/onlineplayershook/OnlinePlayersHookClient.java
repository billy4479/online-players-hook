package online.polpetta.onlineplayershook;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayersHookClient implements ClientModInitializer {
  public static final Logger LOGGER =
      LoggerFactory.getLogger("online-players-hook");

  @Override
  public void onInitializeClient() {
    LOGGER.warn("Online Players Hook is a server only mod.");
  }
}