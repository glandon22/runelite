package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Inventory {

    @Value
    public static class Slot
    {
        int x;
        int y;
        int index;
        int id;
        int quantity;
    }

    private static WidgetItem getWidgetItem(Widget parentWidget, int idx)
    {
        if (parentWidget.isIf3())
        {
            Widget wi = parentWidget.getChild(idx);
            return new WidgetItem(wi.getItemId(), wi.getItemQuantity(), -1, wi.getBounds(), parentWidget, wi.getBounds());
        }
        else
        {
            return parentWidget.getWidgetItem(idx);
        }
    }

    public List<Slot> getInventory(Client client) {
        System.out.println("herererererer");
        List<Slot> inv = null;
        try {
            client.getItemContainer(InventoryID.INVENTORY);
        } catch (Exception e) {
            System.out.println("failed in first fall123123123123");
            System.out.println(client);
        }
        ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
        System.out.println("123123123");
        System.out.println(ic);
        if (ic != null) {
            Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
            inv = new ArrayList<Slot>();
            for (int i = 0; i < items.length; ++i) {
                if (items[i] != null && items[i].getId() > 0) {
                    final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                    final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                    int cx = (int)(r.getX() + (r.getWidth() / 2));
                    int cy = (int)(r.getY() + 23 + (r.getHeight() / 2));
                    Slot slot = new Slot(cx, cy, i, items[i].getId(), items[i].getQuantity());
                    inv.add(slot);
                }
            }
        }
        return inv;
    }
}
