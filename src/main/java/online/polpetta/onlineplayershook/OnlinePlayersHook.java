package online.polpetta.onlineplayershook;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayersHook implements ModInitializer {
  public static final Logger LOGGER =
      LoggerFactory.getLogger("online-players-hook");

  public static final Path CONFIG_PATH =
      Path.of("./config/online-players-hook.txt");

  private URI hookUrl = null;
  private final Set<String> onlinePlayers =
      Collections.synchronizedSet(new HashSet<String>());

  private final Gson gson = new Gson();

  @Override
  public void onInitialize() {
    LOGGER.info("Hello Fabric world!");
    try {
      hookUrl = URI.create(Files.readString(CONFIG_PATH).trim());
    } catch (Exception exception) {
      LOGGER.error("Error while reading hook url, disabling the mod: " +
                   exception.toString());
    }

    if (hookUrl == null)
      return;

    LOGGER.info("Hooking to " + hookUrl);

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      onChange(true, handler.player.getGameProfile().getName());
    });
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      onChange(false, handler.player.getGameProfile().getName());
    });
  }

  private class Payload {
    String event;
    String name;
    Set<String> players;
  }

  private void onChange(boolean isJoin, String name) {
    if (isJoin) {
      onlinePlayers.add(name);
    } else {
      onlinePlayers.remove(name);
    }

    var payload = new Payload();
    if (isJoin) {
      payload.event = "join";
    } else {
      payload.event = "leave";
    }
    payload.name = name;
    payload.players = onlinePlayers;
    var json = gson.toJson(payload);
    System.out.println(json);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
                              .uri(hookUrl)
                              .header("Content-Type", "application/json")
                              .POST(BodyPublishers.ofString(json))
                              .build();

    try {
      client.sendAsync(request, BodyHandlers.discarding());
    } catch (Exception exception) {
      LOGGER.error("Error while posting online players: " +
                   exception.toString());
    }
  }
}