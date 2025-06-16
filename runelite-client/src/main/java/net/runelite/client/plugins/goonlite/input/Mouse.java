package net.runelite.client.plugins.goonlite.input;

import com.google.inject.Inject;
import net.runelite.api.Point;
import net.runelite.client.plugins.goonlite.GoonLitePlugin;
import net.runelite.client.plugins.goonlite.Goonlite;

import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Mouse {
    private final ScheduledExecutorService scheduledExecutorService;

    @Inject
    public Mouse() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(10);
    }

    private synchronized void handleClick(Point point, boolean rightClick) {
        int button = rightClick ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1;

        // Enter event
        MouseEvent enterEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        enterEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(enterEvent);

        // Exit event
        MouseEvent exitEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        exitEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(exitEvent);

        // Move event
        MouseEvent moveEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        moveEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(moveEvent);

        // Press event
        MouseEvent pressEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        pressEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(pressEvent);

        // Release event
        MouseEvent releaseEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        releaseEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(releaseEvent);

        // Click event
        MouseEvent clickEvent = new MouseEvent(Goonlite.getClient().getCanvas(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        clickEvent.setSource("Goonlite");
        Goonlite.getClient().getCanvas().dispatchEvent(clickEvent);
        System.out.println("ran everything");
    }


    public Mouse click(Point p, Boolean r) {
        if (p == null) return this;

        Runnable runnable = () -> handleClick(p, r);

        if (Goonlite.getClient().isClientThread()) {
            System.out.println("we are here");
            scheduledExecutorService.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        }
        else runnable.run();

        return this;
    }

}
