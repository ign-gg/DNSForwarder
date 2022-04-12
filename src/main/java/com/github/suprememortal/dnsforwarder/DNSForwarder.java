package com.github.suprememortal.dnsforwarder;

import org.itxtech.nemisys.Client;
import org.itxtech.nemisys.command.Command;
import org.itxtech.nemisys.command.CommandSender;
import org.itxtech.nemisys.event.EventHandler;
import org.itxtech.nemisys.event.EventPriority;
import org.itxtech.nemisys.event.Listener;
import org.itxtech.nemisys.event.player.PlayerLoginEvent;
import org.itxtech.nemisys.plugin.PluginBase;
import org.itxtech.nemisys.utils.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DNSForwarder extends PluginBase implements Listener {

    private final Map<String, String> mappings = new HashMap<>();
    private String def;
    private String off;
    private List<String> noLobby;

    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("reloaddns")) {
            reloadConfig();
            loadConfig();
            sender.sendMessage("Config reloaded");
        }
        return true;
    }

    private void loadConfig() {
        saveDefaultConfig();
        Config config = getConfig();

        if (!config.exists("forward") || !config.exists("default") || !config.exists("offlineNoLobby") || !config.exists("noLobby")) {
            getLogger().warning("Config is incomplete");
            this.saveResource("config.yml", false);
            reloadConfig();
            config = getConfig();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forward = config.getList("forward", Collections.emptyList());

        for (Map<String, Object> map : forward) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String clientId = (String) entry.getValue();
                String address = entry.getKey().toLowerCase();

                getLogger().info("Mapped: " + address + " ---> " + clientId);

                this.mappings.put(address, clientId);
            }
        }

        def = config.getString("default").replace("&n", "\n");
        off = config.getString("offlineNoLobby");
        noLobby = config.getStringList("noLobby");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onJoin(PlayerLoginEvent event) {
        String address = event.getPlayer().getLoginChainData().getServerAddress().toLowerCase().split(":")[0];
        if (!mappings.containsKey(address) && !address.startsWith("192.168.")) {
            event.setKickMessage(def.replace("%address%", address));
            event.setCancelled(true);
            return;
        }
        String clientId = mappings.get(address);
        Client client = clientId == null ? null : getServer().getClientByDesc(clientId);
        if (client != null && client.getPlayers().size() < client.getMaxPlayers()) {
            event.setClientHash(client.getHash());
        } else if (noLobby.contains(address)) {
            event.setKickMessage(off);
            event.setCancelled(true);
        }
    }
}
