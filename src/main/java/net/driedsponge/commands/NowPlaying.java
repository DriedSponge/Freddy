package net.driedsponge.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.driedsponge.PlayerStore;
import net.driedsponge.TrackScheduler;
import net.driedsponge.VoiceController;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;

public class NowPlaying extends GuildCommand {
    public NowPlaying() {
        super("np");
    }

    @Override
    public void execute(SlashCommandEvent event){
        event.deferReply().queue();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.CYAN);
        if(event.getGuild().getAudioManager().isConnected() &&  PlayerStore.get(event.getGuild().getIdLong()) != null){
            VoiceController vc =  PlayerStore.get(event.getGuild().getIdLong());

            String title = String.format("Now Playing in %s",vc.getVoiceChannel().getName());
            event.getHook().sendMessageEmbeds(TrackScheduler.songCard(title,vc.getNowPlaying()).build()).queue();
        }else{
            embedBuilder.setTitle("Nothing is playing.");
            event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }
}
