package net.runelite.client.plugins.glvartabber;

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
import net.runelite.client.RuneLiteProperties;
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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@PluginDescriptor(
        name = "Varrock Tab Maker",
        description = "Make Varrock Teleport Tablets",
        tags = {"money", "magic", "goonlite"},
        enabledByDefault = false
)
public class glvartabberPlugin extends Plugin {
    @Getter(AccessLevel.PACKAGE)
    private String status;

    @Getter(AccessLevel.PACKAGE)
    private String break_start;

    @Getter(AccessLevel.PACKAGE)
    private String break_end;

    private boolean terminate;

    @Inject
    private glvartabberConfig config;

    @Provides
    glvartabberConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(glvartabberConfig.class);
    }
    private HttpServer server = null;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    ProcessBuilder processBuilder;

    @Value
    private static class StatusRes {
        boolean terminate;
    }

    @Override
    protected void startUp() throws Exception
    {
        terminate = false;
        break_start = "Unknown";
        status = "Starting up.";
        break_end = "Unknown";
        String command = "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool/money/make_varrock_tabs_phials.py";
        processBuilder = new ProcessBuilder(
                "python3",
                System.getProperty("user.dir") + command
        );
        processBuilder.redirectOutput(new File("last_script_run_logs.txt"));
        processBuilder.redirectError(new File("myerrs"));
        // Get the environment variables from the ProcessBuilder instance
        Map<String, String> environment = processBuilder.environment();
    
        // Set a new environment variable
        environment.put("PYTHONPATH", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool");
        System.out.println("starting var tab maker server on port: " + RuneLiteProperties.getPort());
        environment.put("SERVER_PORT", RuneLiteProperties.getPort());
        environment.put("USERNAME", RuneLiteProperties.getName());
        processBuilder.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        new ProcessBuilder(
                "/bin/sh",
                "-c",
                "pgrep -f '.*AutoOldSchool/money/make_varrock_tabs_phials.py' | xargs kill -9").start();
    }
}
