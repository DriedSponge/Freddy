package net.driedsponge;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.net.URI;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class Player {
    public static final HashMap<String, Player> PLAYERS = new HashMap<>();

    private final Guild guild;
    private AudioPlayer player;
    private AudioChannelUnion voiceChannel;
    private GuildMessageChannel textChannel;
    private TrackEvents schedule;
    private QueuedSong nowPlaying;
    private final BlockingQueue<QueuedSong> queue;
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    /**
     * @param channel
     * @throws IllegalArgumentException If the provided channel was null. If the provided channel is
     * not part of the Guild that the current audio connection is connected to.
     * @throws UnsupportedOperationException If audio is disabled due to an internal JDA error
     * @throws net.dv8tion.jda.api.exceptions.InsufficientPermissionException If the currently
     * logged in account does not have the Permission VOICE_MOVE_OTHERS and the user limit has been
     * exceeded!
     */
    public Player(AudioChannelUnion channel) {
        this.guild = channel.getGuild();
        this.voiceChannel = channel;
        this.player = Main.PLAYER_MANAGER.createPlayer();
        this.schedule = new TrackEvents();
        player.addListener(schedule);
        guild.getAudioManager().openAudioConnection(channel);
        guild.getAudioManager().setSendingHandler(new MusicHandler(player));
        this.queue = new LinkedBlockingQueue<>();
        this.textChannel = channel.asGuildMessageChannel();

    }

    public void play(String song, SlashCommandInteractionEvent event) {
        Main.PLAYER_MANAGER.loadItem(song, new StandardResultLoader(event, false));
    }

    public void play(URI song, SlashCommandInteractionEvent event) throws BadHostException {
        if (Main.getAllowedHosts().contains(song.getHost())) {
            if (song.getHost().equals("open.spotify.com")) {
                String[] paths = song.getPath().split("/", 3);
                if (paths[1].equals("playlist") && paths[2] != null) {
                    try {
                        SpotifyPlaylist playlist = SpotifyPlaylist.fromId(paths[2]);
                        SpotifyResultLoader loader = new SpotifyResultLoader(event);
                        List<PlaylistTrack> songs = playlist.getSongs();

                        if(player.getPlayingTrack() == null){
                            PlaylistTrack firstSong = songs.removeFirst();
                            Main.PLAYER_MANAGER.loadItem(
                                    "ytmsearch:" + firstSong.getTrack().getName(),
                                    new StandardResultLoader(event, true)
                            );
                        }

                        for (PlaylistTrack spotify : songs) {
                            Main.PLAYER_MANAGER.loadItem(
                                    "ytmsearch:" + spotify.getTrack().getName(), loader);
                        }
                        event.getHook().sendMessageEmbeds(
                                Embeds.playlistEmbed(
                                        playlist.getName(),
                                        playlist.getSongs().size(),
                                        playlist.getImage(),
                                        playlist.getUrl(),
                                        event.getUser()
                                ).build()
                        ).queue();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new BadHostException("Make sure it's a public playlist " + e.getMessage());
                    }
                } else {
                    throw new BadHostException("Invalid spotify Playlist link");
                }
            } else {
                Main.PLAYER_MANAGER.loadItem(song.toString(), new StandardResultLoader(event, false));
            }
        } else {
            throw new BadHostException("The URL must be valid YouTube URL or Spotify Playlist Link");
        }
    }

    public AudioChannelUnion getVoiceChannel() {
        return voiceChannel;
    }

    public void updateChannel(AudioChannelUnion channel) {
        this.guild.getAudioManager().openAudioConnection(channel);
        this.voiceChannel = channel;
    }

    public QueuedSong getNowPlaying(){
        return nowPlaying;
    }

    public List<QueuedSong> getQueue(){
        QueuedSong[] songs = this.queue.toArray(new QueuedSong[0]);
        return new ArrayList<>(List.of(songs));
    }

    public void playNow(String song) {

    }

    private final class TrackEvents extends AudioEventAdapter {
        @Override
        public void onTrackStart(AudioPlayer player, AudioTrack track) {
            voiceChannel.asVoiceChannel().modifyStatus(":musical_note: " + player.getPlayingTrack()
                    .getInfo().title).queue();
        }

        @Override
        public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
            if (queue.isEmpty()) {
                textChannel.sendMessage("Queue is empty! No more songs to play!").queue();
            } else {
                QueuedSong nextSong = queue.poll();
                player.playTrack(nextSong.getTrack());
                textChannel.sendMessageEmbeds(
                    Embeds.songCard("Now Playing", nextSong).build()
                ).queue();
            }
        }
    }

    private final class SpotifyResultLoader implements AudioLoadResultHandler {
        private SlashCommandInteractionEvent event;

        public SpotifyResultLoader(SlashCommandInteractionEvent event) {
            this.event = event;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            queue.offer(new QueuedSong(track, event));
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            trackLoaded(playlist.getTracks().getFirst());
        }

        @Override
        public void noMatches() {
            // do nothing
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            System.out.println(exception.getMessage());
        }
    }

    private final class StandardResultLoader implements AudioLoadResultHandler {
        private SlashCommandInteractionEvent event;
        private boolean now;
        public StandardResultLoader(SlashCommandInteractionEvent event, boolean now) {
            this.event = event;
            this.now = now;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            QueuedSong song = new QueuedSong(track, event);
            if (now) {
                nowPlaying = song;
                player.playTrack(track);
                event.getHook().sendMessageEmbeds(
                        Embeds.songCard("Now Playing", song).build()
                ).queue();
            } else {
                queue.offer(song);
                event.getHook().sendMessageEmbeds(
                        Embeds.songCard("Song Added To Queue", song).build()
                ).queue();
            }
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            // ignore playlist from search results
            if (playlist.isSearchResult()) {
                trackLoaded(playlist.getTracks().getFirst());
            } else {
                for (AudioTrack track : playlist.getTracks()) {
                    if (nowPlaying == null) {
                        nowPlaying = new QueuedSong(track, event);
                        player.playTrack(track);
                    } else {
                        queue.offer(new QueuedSong(track, event));
                    }
                }
                event.getHook().sendMessageEmbeds(Embeds.playlistEmbed(
                   playlist.getName(),
                   playlist.getTracks().size(),
                   null,
                   null,
                   event.getUser()
                ).build()).queue();
            }
        }

        @Override
        public void noMatches() {
            event.getHook().sendMessage("no matches for the desired song").queue();
        }

        @Override
        public void loadFailed(FriendlyException exception) {
            event.getHook().sendMessage(exception.getMessage()).queue();
        }
    }

}
