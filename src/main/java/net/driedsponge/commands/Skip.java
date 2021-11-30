package net.driedsponge.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.driedsponge.VoiceController;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Skip extends ListenerAdapter {
    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (!event.getName().equals("skip")) return;
        if(CommonChecks.playingMusic(event.getGuild())){
            VoiceController vc = Play.PLAYERS.get(event.getGuild());
            event.reply(":fast_forward: Skipping **"+vc.getNowPlaying().getInfo().title+"**").queue();
            vc.skip();
        }else{
            event.reply("Nothing to skip.").setEphemeral(true).queue();
        }
    }
}