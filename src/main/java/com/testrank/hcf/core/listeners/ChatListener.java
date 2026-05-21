package com.testrank.hcf.core.listeners;

import com.testrank.hcf.core.chat.ChatChannel;
import com.testrank.hcf.core.chat.ChatService;
import com.testrank.hcf.core.team.TeamService;
import com.testrank.hcf.core.threading.Threading;
import com.testrank.hcf.core.util.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatListener implements Listener {
    private final ChatService chat;
    private final TeamService teams;
    private final Threading threading;

    public ChatListener(ChatService chat, TeamService teams, Threading threading) {
        this.chat = chat;
        this.teams = teams;
        this.threading = threading;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!chat.canChat(event.getPlayer())) {
            event.setCancelled(true);
            threading.runSync(() -> event.getPlayer().sendMessage(Text.color(chat.muted() ? "&cChat is currently muted." : "&cChat is slowed right now.")));
            return;
        }
        String message = event.getMessage();
        ChatChannel channel = chat.channel(event.getPlayer());
        if (message.startsWith("!") && teams.byPlayer(event.getPlayer().getUniqueId()).isPresent()) {
            channel = ChatChannel.TEAM;
            message = message.substring(1);
        } else if (message.startsWith("@") && event.getPlayer().hasPermission("hcf.staff")) {
            channel = ChatChannel.STAFF;
            message = message.substring(1);
        }
        if (channel == ChatChannel.TEAM) {
            event.setCancelled(true);
            String routed = message;
            threading.runSync(() -> chat.teamChat(event.getPlayer(), routed));
        } else if (channel == ChatChannel.ALLY) {
            event.setCancelled(true);
            String routed = message;
            threading.runSync(() -> chat.allyChat(event.getPlayer(), routed));
        } else if (channel == ChatChannel.STAFF) {
            event.setCancelled(true);
            String routed = message;
            threading.runSync(() -> chat.staffChat(event.getPlayer(), routed));
        } else {
            event.setFormat(Text.color(chat.chatIdentity(event.getPlayer()) + "&7: " + chat.messageColor(event.getPlayer())) + "%2$s");
        }
    }
}
