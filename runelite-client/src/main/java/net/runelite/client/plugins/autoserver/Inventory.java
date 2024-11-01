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

    Interfaces i = new Interfaces();

    private static WidgetItem getWidgetItem(Widget parentWidget, int idx)
    {
        assert parentWidget.isIf3();
        Widget wi = parentWidget.getChild(idx);
        if (wi == null) {
            return null;
        }
        return new WidgetItem(wi.getItemId(), wi.getItemQuantity(), wi.getBounds(), parentWidget, wi.getBounds());
    }

    public List<Slot> getInventory(Client client) {
        try {
            Interfaces.CanvasData canvasData = i.getCanvasData(client);
            List<Slot> inv = null;
            ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
            if (ic != null) {
                Item[] items = ic.getItems();
                inv = new ArrayList<Slot>();
                Widget invWidget = client.getWidget(WidgetInfo.INVENTORY);
                for (int i = 0; i < items.length; ++i) {
                    if (items[i] != null && items[i].getId() > 0 && invWidget != null) {
                        final WidgetItem targetWidgetItem = getWidgetItem(invWidget, i);
                        final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                        Utilities u = new Utilities();
                        HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
            System.out.println("Exception while parsing inventory111111");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<Slot>();
    }

    public List<Slot> getBankInventory(Client client) {
        try {
            Interfaces.CanvasData canvasData = i.getCanvasData(client);
            List<Slot> inv = null;
            ItemContainer ic = client.getItemContainer(InventoryID.BANK);
            if (ic != null) {
                Item[] items = ic.getItems();
                inv = new ArrayList<Slot>();
                Widget invWidget = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
                for (int i = 0; i < items.length; ++i) {
                    if (items[i] != null && items[i].getId() > 0 && invWidget != null) {
                        final WidgetItem targetWidgetItem = getWidgetItem(invWidget, i);
                        if (targetWidgetItem == null) continue;
                        final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                        Utilities u = new Utilities();
                        HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getXOffset());
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
            System.out.println("Exception while parsing inventory111111");
            System.out.println(e.getMessage());
            System.out.println(e.getCause());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<Slot>();
    }

    public List<Slot> getShopInventory(Client client) {
        try {
            Interfaces.CanvasData canvasData = i.getCanvasData(client);
            List<Slot> inv = new ArrayList<Slot>();
            for (int j = 0; j < 28; j++) {
                Interfaces.EnrichedInterfaceData slot = i.getWidget(client, String.format("301,0,%s", j));
                assert slot != null;
                inv.add(new Slot(
                    slot.getX(),
                    slot.getY(),
                    j,
                    slot.getItemID(),
                    slot.getQuantity()
                ));
            }
            return inv;
        } catch (Exception e) {
            System.out.println("Exception while parsing inventory111111");
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
                System.out.println("got some items");
                System.out.println(items.length);
                inv = new ArrayList<Slot>();
                for (int i = 0; i < items.length; i++) {
                    System.out.println("my items");
                    System.out.println(items[i].getId());
                    Item item = items[i];
                    if (item != null && item.getId() > 0) {
                        Slot slot = new Slot(0, 0, i, items[i].getId(), items[i].getQuantity());
                        inv.add(slot);
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
