/*
 * Copyright (c) 2022, Jamal <http://github.com/1Defence>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.worldcycleplugin;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WorldCyclePanel extends PluginPanel
{
    private final MaterialTabGroup tabGroup;
    final UICycleInputArea uiInput = new UICycleInputArea();

    private Timer configUpdateTimer;
    private Timer worldSetChangeTimer;


    ConfigManager configManager;
    WorldCyclePlugin plugin;
    public boolean pendingRequest = false;

    WorldCyclePanel(Client client, ConfigManager configManager, WorldCyclePlugin plugin)
    {
        super();
        this.configManager = configManager;
        this.plugin = plugin;
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        tabGroup = new MaterialTabGroup();
        tabGroup.setLayout(new GridLayout(0, 6, 7, 7));


        uiInput.setBorder(new EmptyBorder(15, 0, 15, 0));
        uiInput.setBackground(ColorScheme.DARK_GRAY_COLOR);


        SetFromConfig();

        uiInput.getUiFieldWorldSet().getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
            }
            public void removeUpdate(DocumentEvent e) {
                if(pendingRequest){//gets set to false on the insert, as it removes then inserts.
                    //System.out.println("Pending request.. wont call remove");
                }else {
                    RequestChangeWorldSet();
                }
                RequestConfigUpdate();
            }
            public void insertUpdate(DocumentEvent e) {
                if(pendingRequest){
                    pendingRequest = false;
                    //System.out.println("Pending request.. wont call insert");
                }else {
                    RequestChangeWorldSet();
                }
                RequestConfigUpdate();
            }});

        add(tabGroup, c);
        c.gridy++;

        add(uiInput, c);
        c.gridy++;
    }

    private void RequestChangeWorldSet(){

        if(worldSetChangeTimer != null){
            worldSetChangeTimer.restart();
            return;
        }

        ActionListener actionListener = new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                ChangeWorldSet();
            }
        };
        worldSetChangeTimer = new Timer(600, actionListener);
        worldSetChangeTimer.setRepeats(false);
        worldSetChangeTimer.start();
    }

    public void ChangeWorldSet(){
        plugin.ChangeWorldSet(uiInput.getWorldSetInput(),false);
    }

    private void RequestConfigUpdate(){

        if(configUpdateTimer != null){
            configUpdateTimer.restart();
            return;
        }

        ActionListener actionListener = new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                UpdateConfig();
            }
        };
        configUpdateTimer = new Timer(2000, actionListener);
        configUpdateTimer.setRepeats(false);
        configUpdateTimer.start();

    }

    private void UpdateConfig(){

        if(uiInput.getWorldSetInput().isEmpty()){
            configManager.unsetConfiguration(WorldCycleConfig.GROUP, WorldCycleConfig.CONFIG_WORLDSET);
        }else{
            configManager.setConfiguration(WorldCycleConfig.GROUP, WorldCycleConfig.CONFIG_WORLDSET,uiInput.getWorldSetInput());
        }

        configUpdateTimer = null;
    }

    public void SetFromConfig(){

        if(configManager == null){
            return;
        }

        String worldSet = configManager.getConfiguration(WorldCycleConfig.GROUP, WorldCycleConfig.CONFIG_WORLDSET);
        if(worldSet != null){
            uiInput.setWorldSetInput(configManager.getConfiguration(WorldCycleConfig.GROUP, WorldCycleConfig.CONFIG_WORLDSET));
        }

    }




}
