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
                    BankSlot bs = new BankSlot(center.get('x'), center.get('y'), child.getItemId(), child.getItemQuantity());
                    bankStuff.add(bs);
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
            return u.getCenter(r);
        }
        return null;
    }
}
