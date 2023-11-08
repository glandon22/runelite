package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Bank {
    @Value
    public static class BankSlot
    {
        int x;
        int y;
        int id;
        int quantity;
    }

    public ArrayList<BankSlot> getBankItems(Client client) {
        final Widget bankItemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        ItemContainer bankContainer = null;
        Widget[] children = null;
        if (bankItemContainer != null) {
            bankContainer = client.getItemContainer(InventoryID.BANK);
            children = bankItemContainer.getChildren();
        }

        ArrayList<BankSlot> bankStuff = new ArrayList<>();
        if (bankContainer != null && children != null) {
            // The first components are the bank items, followed by tabs etc. There are always 816 components regardless
            // of bank size, but we only need to check up to the bank size.
            for (int i = 0; i < bankContainer.size(); ++i) {
                Widget child = children[i];
                if (child != null && !child.isSelfHidden() && child.getItemId() > -1) {
                    Rectangle r = child.getBounds();
                    Utilities u = new Utilities();
                    HashMap<Character, Integer> center = u.getCenter(r);
                    // For some reason, right as I open an interface it sometimes says the points are all located
                    // in a small 50x50 corner of the upper right-hand screen.
                    if (center.get('x') > 50 && center.get('y') > 50) {
                        BankSlot bs = new BankSlot(center.get('x'), center.get('y'), child.getItemId(), child.getItemQuantity());
                        bankStuff.add(bs);
                    }
                }
            }
        }

        return bankStuff;
    }


    public ArrayList<BankSlot> getDepositItems(Client client) {
        final Widget bankItemContainer = client.getWidget(192,2);
        Widget[] children = null;
        if (bankItemContainer != null) {
            children = bankItemContainer.getChildren();
        }

        ArrayList<BankSlot> bankStuff = new ArrayList<>();
        if (children != null) {
            // The first components are the bank items, followed by tabs etc. There are always 816 components regardless
            // of bank size, but we only need to check up to the bank size.
            for (Widget child : children) {
                if (child != null && !child.isSelfHidden() && child.getItemId() > -1 && !child.getName().isEmpty()) {
                    Rectangle r = child.getBounds();
                    Utilities u = new Utilities();
                    HashMap<Character, Integer> center = u.getCenter(r);
                    // For some reason, right as I open an interface it sometimes says the points are all located
                    // in a small 50x50 corner of the upper right-hand screen.
                    if (center.get('x') > 50 && center.get('y') > 50) {
                        BankSlot bs = new BankSlot(center.get('x'), center.get('y'), child.getItemId(), child.getItemQuantity());
                        bankStuff.add(bs);
                    }
                }
            }
        }

        return bankStuff;
    }
    public HashMap<Character, Integer> getDumpInventoryLoc(Client client) {
        final Widget bankDumpContainer = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (bankDumpContainer != null) {
            Rectangle r = bankDumpContainer.getBounds();
            Utilities u = new Utilities();
            HashMap<Character, Integer> center = u.getCenter(r);
            // For some reason, right as I open an interface it sometimes says the points are all located
            // in a small 50x50 corner of the upper right-hand screen.
            if (center.get('x') > 50 && center.get('y') > 50) {
                return center;
            }
        }
        return null;
    }
}
