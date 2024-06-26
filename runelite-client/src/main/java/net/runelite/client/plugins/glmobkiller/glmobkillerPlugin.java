package net.runelite.client.plugins.glmobkiller;

import java.io.File;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Provides;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.compress.utils.IOUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@PluginDescriptor(
        name = "Mob Killer",
        description = "Kills a configured NPC, drinks potions, and eats food",
        tags = {"combat", "killer", "goonlite"},
        enabledByDefault = false
)
public class glmobkillerPlugin extends Plugin {
    @Getter(AccessLevel.PACKAGE)
    private String status;

    @Getter(AccessLevel.PACKAGE)
    private String break_start;

    @Getter(AccessLevel.PACKAGE)
    private String break_end;

    private boolean terminate;

    @Inject
    private glmobkillerConfig config;

    @Provides
    glmobkillerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(glmobkillerConfig.class);
    }

    ProcessBuilder processBuilder;
    Process p;


    @Override
    protected void startUp() throws Exception
    {
        terminate = false;
        break_start = "Unknown";
        status = "Starting up.";
        break_end = "Unknown";
        String command = "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool/combat/kill_and_loot_v2.py";

        if (System.getProperty("os.name").toUpperCase(Locale.ROOT).contains("WINDOWS")) {
            System.out.println("dddd");
            String env_p = System.getProperty("user.dir") + "\\runelite-client\\src\\main\\resources\\net\\runelite\\client\\AutoOldSchool";

            String cmd = "python " + System.getProperty("user.dir") + command + ' ' + config.npcToKill().replaceAll("\\s+","_") + ' ' + config.pot().getName() + ' ' + String.valueOf(config.potInterval()) + ' ' + String.valueOf(config.minEat());
            processBuilder = new ProcessBuilder(
                    "C:\\Windows\\system32\\cmd.exe", "/c", cmd
            );
            processBuilder.redirectOutput(new File("last_script_run_logs.txt"));
            // Get the environment variables from the ProcessBuilder instance
            Map<String, String> environment = processBuilder.environment();
            environment.put("PYTHONPATH", env_p);
        }

        else {
            System.out.println("sdsdsdsd");

            processBuilder = new ProcessBuilder(
                    "python3",
                    System.getProperty("user.dir") + command,
                    config.npcToKill(),
                    config.pot().getName(),
                    String.valueOf(config.potInterval()),
                    String.valueOf(config.minEat()),
                    String.valueOf(config.safeSpotX()),
                    String.valueOf(config.safeSpotY()),
                    String.valueOf(config.safeSpotZ())
            );
            processBuilder.redirectOutput(new File("last_script_run_logs.txt"));
            // Get the environment variables from the ProcessBuilder instance
            Map<String, String> environment = processBuilder.environment();

            // Set a new environment variable
            environment.put("PYTHONPATH", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool");
        }

        p = processBuilder.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (System.getProperty("os.name").toUpperCase(Locale.ROOT).contains("WINDOWS")) {
            new ProcessBuilder(
                    "C:\\Windows\\system32\\cmd.exe", "/c", "taskkill /IM python.exe /F"
            ).start();
        }
        else {
            new ProcessBuilder(
                    "/bin/sh",
                    "-c",
                    "pgrep -f '.*AutoOldSchool/combat/kill_and_loot_v2.py' | xargs kill -9").start();
        }
    }
}
