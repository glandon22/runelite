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
import java.util.Arrays;
import java.util.HashMap;
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
        try {
            List<Slot> inv = null;
            ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
            if (ic != null) {
                Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
                inv = new ArrayList<Slot>();
                for (int i = 0; i < items.length; ++i) {
                    if (items[i] != null && items[i].getId() > 0) {
                        final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                        final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                        Utilities u = new Utilities();
                        HashMap<Character, Integer> center = u.getCenter(r);
                        // For some reason, right as I open an interface it sometimes says the points are all located
                        // in a small 50x50 corner of the upper right-hand screen.
                        if (center.get('x') > 50 && center.get('y') > 50) {
                            Slot slot = new Slot(center.get('x'), center.get('y'), i, items[i].getId(), items[i].getQuantity());
                            inv.add(slot);
                        }
                    }
                }
            }
            return inv;
        } catch (Exception e) {
            System.out.println("Exception while parsing inventory");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<Slot>();
    }

    public List<Slot> getEquipmentInventory(Client client) {
        try {
            List<Slot> inv = null;
            ItemContainer ic = client.getItemContainer(InventoryID.EQUIPMENT);
            if (ic != null) {
                Item[] items = ic.getItems();
                inv = new ArrayList<Slot>();
                for (Item item : items) {
                    if (item != null && item.getId() > 0) {
                        System.out.println(item.getId());
                        /*final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.EQUIPMENT), i);
                        final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                        Utilities u = new Utilities();
                        HashMap<Character, Integer> center = u.getCenter(r);
                        // For some reason, right as I open an interface it sometimes says the points are all located
                        // in a small 50x50 corner of the upper right-hand screen.
                        if (center.get('x') > 50 && center.get('y') > 50) {
                            Slot slot = new Slot(center.get('x'), center.get('y'), i, items[i].getId(), items[i].getQuantity());
                            inv.add(slot);
                        }*/
                    }
                }
            }
            return inv;
        } catch (Exception e) {
            System.out.println("Exception while parsing inventory");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }
}
