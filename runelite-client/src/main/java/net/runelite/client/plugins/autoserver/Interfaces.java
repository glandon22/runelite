package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Interfaces {
    @Value
    public static class InterfaceData {
        int x;
        int y;
    }

    @Value
    public static class RightClickMenu {
        int x;
        int y;
        int server_x;
        int server_y;
        int width;
        int height;
        ArrayList<String> entries;
    }

    @Value
    public static class RightClickMenuV2 {
        int x;
        int y;
        int server_x;
        int server_y;
        int width;
        int height;
        ArrayList<ArrayList<String>> entries;
    }

    @Value
    public static class EnrichedInterfaceData {
        int x;
        int y;
        String text;
        int textColor;
        int spriteID;
        String name;
        int itemID;
        int xMin;
        int xMax;
        int yMin;
        int yMax;
    }

    @Value
    public static class CanvasData {
        int xMin;
        int xMax;
        int yMin;
        int yMax;
    }

    public InterfaceData getClickToPlay(Client client) {
        GameState gs = client.getGameState();
        String state = gs.toString();
        if (Objects.equals(state, "LOGGED_IN")) {
            Widget clickToPlayButton = client.getWidget(WidgetInfo.CLICK_HERE_TO_PLAY_BUTTON);
            if (clickToPlayButton != null) {
                Rectangle r = clickToPlayButton.getBounds();
                Utilities u = new Utilities();
                HashMap<Character, Integer> center = u.getCenter(r);
                if (center.get('x') > 50 && center.get('y') > 50) {
                    return new InterfaceData(
                            center.get('x'),
                            center.get('y')
                    );
                }
            }
        }


        return null;
    }

    public EnrichedInterfaceData getWidget(Client client, String widget) {
        String[] childAndParent = widget.split(",");
        Widget targetWidget = client.getWidget(
            Integer.parseInt(childAndParent[0]),
            Integer.parseInt(childAndParent[1])
        );

        if (targetWidget != null && childAndParent.length == 3) {
            System.out.println("child");
            System.out.println(Integer.parseInt(childAndParent[2]));
            targetWidget = targetWidget.getChild(Integer.parseInt(childAndParent[2]));
        }

        if (targetWidget != null) {

            Rectangle r = targetWidget.getBounds();
            Utilities u = new Utilities();
            HashMap<Character, Integer> center = u.getCenter(r);
            return new EnrichedInterfaceData(
                    center.get('x'),
                    center.get('y'),
                    targetWidget.getText(),
                    targetWidget.getTextColor(),
                    targetWidget.getSpriteId(),
                    targetWidget.getName(),
                    targetWidget.getItemId(),
                    (int) r.getX(),
                    (int) ( r.getX() + r.getWidth()),
                    (int) r.getY() + 23,
                    (int) (r.getY() + r.getHeight() + 23)
            );
        }
        return null;
    }

    public HashMap<String, EnrichedInterfaceData> getWidgets(Client client, JsonArray widgetsToFind) {
        ArrayList<String> widgetList = new ArrayList<>();
        HashMap<String, EnrichedInterfaceData> widgetDataPacket = new HashMap<>();
        for (JsonElement elem : widgetsToFind) {
            try {
                String widget = elem.toString().replace("\"", "");
                widgetList.add(widget);
            } catch (Exception e) {
                System.out.println("Failed to find data for widget: ");
                System.out.println(elem);
            }
        }

        for (String widget: widgetList) {
            widgetDataPacket.put(widget, getWidget(client, widget));
        }

        return widgetDataPacket;
    }

    public EnrichedInterfaceData getWidgetV2(Client client, int widget) {
        Widget targetWidget = client.getWidget(widget);

        if (targetWidget != null) {

            Rectangle r = targetWidget.getBounds();
            Utilities u = new Utilities();
            HashMap<Character, Integer> center = u.getCenter(r);
            return new EnrichedInterfaceData(
                    center.get('x'),
                    center.get('y'),
                    targetWidget.getText(),
                    targetWidget.getTextColor(),
                    targetWidget.getSpriteId(),
                    targetWidget.getName(),
                    targetWidget.getItemId(),
                    (int) r.getX(),
                    (int) ( r.getX() + r.getWidth()),
                    (int) r.getY() + 23,
                    (int) (r.getY() + r.getHeight() + 23)
            );
        }
        return null;
    }

    public HashMap<String, EnrichedInterfaceData> getWidgetsV2(Client client, JsonArray widgetsToFind) {
        ArrayList<String> widgetList = new ArrayList<>();
        HashMap<String, EnrichedInterfaceData> widgetDataPacket = new HashMap<>();
        for (JsonElement elem : widgetsToFind) {
            try {
                String widget = elem.toString().replace("\"", "");
                widgetList.add(widget);
            } catch (Exception e) {
                System.out.println("Failed to find data for widget: ");
                System.out.println(elem);
            }
        }

        for (String widget: widgetList) {
            widgetDataPacket.put(String.valueOf(widget), getWidgetV2(client, Integer.parseInt(widget)));
        }

        return widgetDataPacket;
    }

    public String[] getChatOptions(Client client) {
        Widget playerDialogueOptionsWidget = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);
        if (playerDialogueOptionsWidget != null) {
            Widget[] dialogueOptions = playerDialogueOptionsWidget.getChildren();
            if (dialogueOptions != null) {
                String[] optionsText = new String[dialogueOptions.length];
                for (int i = 0; i < dialogueOptions.length; i++)
                {
                    System.out.println(dialogueOptions[i].getText());
                    optionsText[i] = dialogueOptions[i].getText();
                }
                return optionsText;
            }
        }
        return null;
    }

    public List getMenuEntries(Client client) {

        if (client.isMenuOpen()) {
            MenuEntry[] menuEntries = client.getMenuEntries();
            List menuItems = new List();
            for (MenuEntry entry : menuEntries)
            {
                String item = "";
                item = item.concat(entry.getOption()).concat(" ").concat(entry.getTarget());
                menuItems.add(item);
            }
            return menuItems;
        }
        return null;
    }

    public RightClickMenu getRightClickMenuEntries(Client client) {
        final int menuX = client.getMenuX();
        final int menuY = client.getMenuY();
        final int menuWidth = client.getMenuWidth();
        final int menuH = client.getMenuHeight();
        ArrayList<String> options = new ArrayList<>();
        final MenuEntry[] menuEntries = client.getMenuEntries();
        for(MenuEntry mu : menuEntries) {
            options.add(mu.getOption());
        }
        return new RightClickMenu(
                menuX,
                menuY,
                client.getMouseCanvasPosition().getX(),
                client.getMouseCanvasPosition().getY(),
                menuWidth,
                menuH,
                options
        );
    }

    public RightClickMenuV2 getRightClickMenuEntriesV2(Client client) {
        final int menuX = client.getMenuX();
        final int menuY = client.getMenuY();
        final int menuWidth = client.getMenuWidth();
        final int menuH = client.getMenuHeight();
        ArrayList<ArrayList<String>> options = new ArrayList<>();
        final MenuEntry[] menuEntries = client.getMenuEntries();
        for(MenuEntry mu : menuEntries) {
            ArrayList<String> opts = new ArrayList<>();
            opts.add(mu.getOption());
            opts.add(String.valueOf(mu.getIdentifier()));
            options.add(opts);
        }
        return new RightClickMenuV2(
                menuX,
                menuY,
                client.getMouseCanvasPosition().getX(),
                client.getMouseCanvasPosition().getY(),
                menuWidth,
                menuH,
                options
        );
    }

    public String[] getChatLines(Client client)
    {
        String[] chatLines = new String[8];
        Widget chatHolder = client.getWidget(162, 56);
        if (chatHolder != null && chatHolder.getChildren() != null) {
            for (int i = 0; i <= 28; i += 4) {
                Widget line = chatHolder.getChild(i);
                System.out.println("eeeeee");
                System.out.println(i);
                System.out.println(line.getText());
                chatLines[i / 4] = line.getText();
            }
        }
        return chatLines;
    }

    public CanvasData getCanvasData(Client client) {
        Canvas c = client.getCanvas();
        Rectangle r = c.getBounds();
        double xMin = c.getLocationOnScreen().getX();
        double xMax = xMin + r.getWidth();
        // Account for the runelite title bar
        double yMin = c.getLocationOnScreen().getY();
        double yMax = yMin + r.getHeight();
        return new CanvasData(
                (int) xMin,
                (int) xMax,
                (int) yMin,
                (int) yMax
        );
    }
}
