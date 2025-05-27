package online.polpetta.onlineplayershook;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayersHook implements ModInitializer {
  public static final Logger LOGGER =
      LoggerFactory.getLogger("online-players-hook");

  public static final Path CONFIG_PATH =
      Path.of("./config/online-players-hook.properties");

  private URI hookUrl = null;
  private String secret = null;

  private HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private void loadConfig() {
    Properties properties = new Properties();
    try (var input = new FileInputStream(CONFIG_PATH.toFile())) {
      properties.load(input);

      try {
        hookUrl = new URI(properties.getProperty("endpoint"));
      } catch (URISyntaxException ex){
        LOGGER.error("Bad URI: "+ ex.toString());
      }
      secret = properties.getProperty("secret");

      LOGGER.info("API Endpoint loaded from config: " + hookUrl);

    } catch (IOException ex) {
      LOGGER.error("Error loading config.properties", ex);
    }
  }

  // https://stackoverflow.com/a/53225089/13166735
  public static URI appendToUrl(URI uri, Map<String, String> parameters)
  {
      String query = uri.getQuery();

      StringBuilder builder = new StringBuilder();

      if (query != null)
          builder.append(query);

      for (Map.Entry<String, String> entry: parameters.entrySet())
      {
          String keyValueParam = entry.getKey() + "=" + entry.getValue();
          if (!builder.toString().isEmpty())
              builder.append("&");

          builder.append(keyValueParam);
      }

      try {
        return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), builder.toString(), uri.getFragment());
      } catch (URISyntaxException ex) {
        return null;
      }
  }
  
  private void makeRequest(URI uri) {
    HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
    client.sendAsync(request, BodyHandlers.ofString()).thenAccept(res -> {
      if (res.statusCode() != 200) {
        LOGGER.error("Failed to send event");
      }
    }).exceptionally(e -> {
      LOGGER.error("Error sending event", e);
      return null;
    });
  }

  @Override
  public void onInitialize() {
    loadConfig();

    if (hookUrl == null)
      return;

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      var uri = appendToUrl(hookUrl, Map.of("secret", secret, "action", "join", "player", handler.player.getGameProfile().getName()));
      makeRequest(uri);
    });
    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
      var uri = appendToUrl(hookUrl, Map.of("secret", secret, "action", "leave", "player", handler.player.getGameProfile().getName()));
      makeRequest(uri);
    });
    ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
      var uri = appendToUrl(hookUrl, Map.of("secret", secret, "action", "stop"));
      makeRequest(uri);
    });

    ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
      var uri = appendToUrl(hookUrl, Map.of("secret", secret, "action", "start"));
      makeRequest(uri); 
    });
   
  }
}
