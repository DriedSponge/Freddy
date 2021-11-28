package net.driedsponge;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class PingCommand extends ListenerAdapter {
    @Override
    public void onSlashCommand(SlashCommandEvent event) {

        if (event.getName().equals("ping")) {
            event.reply("Pong!").queue(response ->{
                response.editOriginal("Pong! "+String.valueOf(event.getJDA().getGatewayPing())).queue();
            }); // reply immediately
        }
    }
}
