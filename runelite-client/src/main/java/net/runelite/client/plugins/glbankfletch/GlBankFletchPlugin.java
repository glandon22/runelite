package net.runelite.client.plugins.glbankfletch;

import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
        name = "Bank Fletcher",
        description = "Auto fletches",
        tags = {"bot", "goonlite", "fletch"},
        enabledByDefault = false
)
public class GlBankFletchPlugin extends Plugin {
    private static final Logger logger = LoggerFactory.getLogger(GlBankFletchPlugin.class);
    @Getter(AccessLevel.PACKAGE)
    private String status;

    @Getter(AccessLevel.PACKAGE)
    private String break_start;

    @Getter(AccessLevel.PACKAGE)
    private String break_end;

    private boolean terminate;

    private HttpServer server = null;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    ProcessBuilder processBuilder;
    Process process;

    @Value
    private static class StatusRes {
        boolean terminate;
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ScriptOverlay overlay;

    @Override
    protected void startUp() throws Exception
    {
        terminate = false;
        break_start = "Unknown";
        status = "Starting up.";
        break_end = "Unknown";
        server = HttpServer.create(new InetSocketAddress("localhost", 56798), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(threadPoolExecutor);
        server.start();
        processBuilder = new ProcessBuilder("python3", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool/fletching/string_v3.py");
        processBuilder.redirectErrorStream(true);
        // Get the environment variables from the ProcessBuilder instance
        Map<String, String> environment = processBuilder.environment();

        // Set a new environment variable
        environment.put("PYTHONPATH", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool");
        processBuilder.start();


        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        Process p = new ProcessBuilder(
                "/bin/sh",
                "-c",
                "pgrep -f '.*AutoOldSchool/fletching/string_v3.py' | xargs kill -9").start();
        overlayManager.remove(overlay);
        server.stop(0);
    }
}
