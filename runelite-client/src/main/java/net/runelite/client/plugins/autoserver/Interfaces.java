package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.*;
import java.util.HashMap;
import java.util.Objects;

public class Interfaces {
    @Value
    public static class InterfaceData {
        int x;
        int y;
    }

    @Value
    public static class EnrichedInterfaceData {
        int x;
        int y;
        String text;
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

    public EnrichedInterfaceData getWidget(Client client, Object widget) {
        String widgetString = (String) widget;
        String[] childAndParent = widgetString.split(",");
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
                    targetWidget.getText()
            );
        }
        return null;
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
}
