package net.driedsponge;

import io.sentry.Sentry;
import net.driedsponge.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    public static final String OWNER_ID = System.getenv("OWNER_ID");

    public static void main(String[] args) throws LoginException, IOException, ParseException, SpotifyWebApiException {

        intializeSentry();
        SpotifyLookup.clientCredentials_Sync();

        String token = System.getenv("DISCORD_TOKEN");

        JDABuilder builder = JDABuilder.createDefault(token);
        // Disable parts of the cache
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES);
        builder.enableCache(CacheFlag.VOICE_STATE);
        // Enable the bulk delete event
        builder.setBulkDeleteSplittingEnabled(false);
        // Disable compression (not recommended)
        builder.setCompression(Compression.NONE);

        builder.setActivity(Activity.watching("for /help"));

        builder.addEventListeners(new Ping());
        builder.addEventListeners(new JoinLeave());
        builder.addEventListeners(new UserVoiceEvents());
        builder.addEventListeners(new Play());
        builder.addEventListeners(new Pause());
        builder.addEventListeners(new Owner());
        builder.addEventListeners(new NowPlaying());
        builder.addEventListeners(new Queue());
        builder.addEventListeners(new Skip());
        builder.addEventListeners(new Help());
        builder.addEventListeners(new Bug());
        builder.addEventListeners(new Shuffle());
        builder.addEventListeners(new MessageListener());
        builder.addEventListeners(new ButtonListener());
        JDA jda = builder.build();
    }

    private static void intializeSentry() {
        Sentry.init(options -> {
            options.setDsn(System.getenv("SENTRY_DSN"));
            // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
            // We recommend adjusting this value in production.
            options.setTracesSampleRate(1.0);

        });

    }
}
