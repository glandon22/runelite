package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

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
        int width;
        int height;
        Object[] entries;
    }

    @Value
    public static class EnrichedInterfaceData {
        int x;
        int y;
        String text;
        int spriteID;
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

        if (targetWidget != null) {
            Rectangle r = targetWidget.getBounds();
            Utilities u = new Utilities();
            HashMap<Character, Integer> center = u.getCenter(r);
            return new EnrichedInterfaceData(
                    center.get('x'),
                    center.get('y'),
                    targetWidget.getText(),
                    targetWidget.getSpriteId()
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
        final MenuEntry[] menuEntries = client.getMenuEntries();
        return new RightClickMenu(menuX, menuY, menuWidth, menuH, Arrays.stream(menuEntries).toArray());
    }
}
