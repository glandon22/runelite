package net.runelite.client.plugins.goonglassblowing;

import net.runelite.api.GameState;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@PluginDescriptor(
        name = "Blow Glass",
        description = "Auto Blows Glass",
        tags = {"bot", "goonlite", "crafting"},
        enabledByDefault = false
)
public class GoonGlassBlowingPlugin extends Plugin {
    ProcessBuilder processBuilder;
    Process process;

    @Override
    protected void startUp() throws Exception
    {
        processBuilder = new ProcessBuilder("python", "C:\\Users\\gland\\Desktop\\runelite-2\\runelite-client\\src\\main\\resources\\net\\runelite\\client\\python\\test.py");
        processBuilder.redirectErrorStream(true);
        process = processBuilder.start();
        InputStream results = process.getInputStream();
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (results, StandardCharsets.UTF_8))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }
        System.out.println("here");
        System.out.println(textBuilder.toString());
    }

    @Override
    protected void shutDown()
    {
        if (processBuilder != null) {
            process.destroy();
        }
    }
}
