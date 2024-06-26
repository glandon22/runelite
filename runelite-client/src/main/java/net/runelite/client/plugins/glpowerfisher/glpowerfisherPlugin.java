package net.runelite.client.plugins.glpowerfisher;


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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@PluginDescriptor(
        name = "glpowerfisher",
        description = "fish and drop",
        tags = {"bot", "fishing", "goonlite"},
        enabledByDefault = false
)
public class glpowerfisherPlugin extends Plugin {
    @Getter(AccessLevel.PACKAGE)
    private String status;

    @Getter(AccessLevel.PACKAGE)
    private String break_start;

    @Getter(AccessLevel.PACKAGE)
    private String break_end;

    private boolean terminate;

    @Inject
    private glpowerfisherConfig config;

    @Provides
    glpowerfisherConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(glpowerfisherConfig.class);
    }
    private HttpServer server = null;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    ProcessBuilder processBuilder;

    @Value
    private static class StatusRes {
        boolean terminate;
    }

    @Inject
    private OverlayManager overlayManager;

    @Override
    protected void startUp() throws Exception
    {
        terminate = false;
        break_start = "Unknown";
        status = "Starting up.";
        break_end = "Unknown";
        String command = "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool/fishing/powerfish_v3.py";
        System.out.println("sdfsdffsd");
        System.out.println(System.getProperty("user.dir") + command);
        System.out.println(config.fish().getName());
        processBuilder = new ProcessBuilder(
                "python3",
                System.getProperty("user.dir") + command,
                config.fish().getName()
        );
        processBuilder.redirectOutput(new File("./test.txt"));
        // Get the environment variables from the ProcessBuilder instance
        Map<String, String> environment = processBuilder.environment();

        // Set a new environment variable
        environment.put("PYTHONPATH", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool");
        processBuilder.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        new ProcessBuilder(
                "/bin/sh",
                "-c",
                "pgrep -f '.*AutoOldSchool/fishing/powerfish_v3.py' | xargs kill -9").start();
    }
}
