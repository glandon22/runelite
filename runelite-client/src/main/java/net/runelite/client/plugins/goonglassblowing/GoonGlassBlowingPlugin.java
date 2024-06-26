package net.runelite.client.plugins.goonglassblowing;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.compress.utils.IOUtils;

import javax.inject.Inject;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@PluginDescriptor(
        name = "Blow Glass",
        description = "Auto Blows Glass",
        tags = {"bot", "goonlite", "crafting"},
        enabledByDefault = false
)
public class GoonGlassBlowingPlugin extends Plugin {
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

    private class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                handleResponse(httpExchange, httpExchange.getRequestBody());
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            }
        }

        private void handleResponse(HttpExchange httpExchange, InputStream reqBody) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            OutputStream outputStream = httpExchange.getResponseBody();
            byte[] bytes = IOUtils.toByteArray(reqBody);
            String text = new String(bytes, CHARSET);
            try {
                new JsonParser().parse(text).getAsJsonObject();
            } catch (Exception e) {
                String resText = "Exception while trying to parse request body.";
                httpExchange.sendResponseHeaders(403, resText.length());

                outputStream.write(resText.getBytes());
                outputStream.flush();
                outputStream.close();
                return;
            }
            final JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
            if (jsonObject.get("status") != null) {
                status = jsonObject.get("status").getAsString();
            }

            if (jsonObject.get("next_break") != null) {
                break_start = jsonObject.get("next_break").getAsString();
            }

            if (jsonObject.get("break_end") != null) {
                break_end = jsonObject.get("break_end").getAsString();
            }

            Headers headers = httpExchange.getResponseHeaders();
            // Tell my downstream consumer we are sending JSON back
            headers.set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));

            // Convert my game data object into a JSON string for down stream consumption
            Gson gson = new Gson();
            String responseBody = gson.toJson(new StatusRes(terminate));
            byte[] rawResponseBody = responseBody.getBytes(CHARSET);
            httpExchange.sendResponseHeaders(200, rawResponseBody.length);
            outputStream.write(rawResponseBody);
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    protected void startUp() throws Exception
    {
        terminate = false;
        break_start = "Unknown";
        status = "Starting up.";
        break_end = "Unknown";
        server = HttpServer.create(new InetSocketAddress("localhost", 56798), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/manager", new MyHttpHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        processBuilder = new ProcessBuilder("python3", System.getProperty("user.dir") + "/runelite-client/src/main/resources/net/runelite/client/AutoOldSchool/crafting/blow_glass_v3.py");
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
                "pgrep -f '.*AutoOldSchool/crafting/blow_glass_v3.py' | xargs kill -9").start();
        overlayManager.remove(overlay);
        server.stop(0);
    }
}
