package fe.igscraper;

import com.github.zafarkhaja.semver.*;
import com.sun.javafx.application.*;
import fe.binaryversion.BinaryVersion;
import fe.igscraper.instagram.util.*;
import com.google.gson.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.temporal.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import fe.igscraper.mediaplayer.*;
import fe.logger.Logger;
import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.*;
import net.sourceforge.argparse4j.inf.*;
import javafx.beans.value.*;

public class Main {
    private Logger logger;
    private BinaryVersion binaryVersion = new BinaryVersion(true);
    public static final String APP_NAME = "InstagramScraper";

    public Main(final String configPath, boolean enableDownloadCompleteSound, boolean metadata) {
        try {
            this.binaryVersion.loadVersion();
            this.binaryVersion.checkUpdate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.logger = new Logger(String.format("%s-%s", APP_NAME, this.binaryVersion.toString()), true);
        this.logger.print(Logger.Type.INFO, "Sound: %b, Metadata: %b", enableDownloadCompleteSound, metadata);
        this.logger.print(Logger.Type.INFO, "Loading config from %s", configPath);
        if (enableDownloadCompleteSound) {
            PlatformImpl.startup(() -> {
            });
        }

        try {
            long start = System.currentTimeMillis();
            File config = new File(configPath);

            ConfigLoader configLoader = new ConfigLoader(config, metadata);
            configLoader.loadLogins();
            configLoader.loadUsers();

            ContentManager contentManager = new ContentManager(configLoader.getDatabase(), configLoader.getUsers());
            contentManager.findContent();
            contentManager.downloadContent();
            Util.writeJson(config, configLoader.getJsonConfig());

            long time = System.currentTimeMillis() - start;
            this.logger.print(Logger.Type.INFO, "Elapsed time: %s", Duration.of(time, ChronoUnit.MILLIS).toString().substring(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (enableDownloadCompleteSound) {
            final Player player = new Player();
            player.play(Main.class.getResource("/resources/download-complete.wav").toString());
            player.finishedProperty().addListener((v, o, n) -> System.exit(0));
        } else {
            System.exit(0);
        }
    }

    public static void main(final String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("InstagramScraper").build().defaultHelp(true).description("Scrape a list of given Instagram accounts incrementally");
        parser.addArgument("config").help("Path to the config used by the scraper");
        parser.addArgument("-s", "--enablecompletesound").action(Arguments.storeTrue()).help("Play a sound when scraping is done");
        parser.addArgument("-m", "--metadata").action(Arguments.storeTrue()).help("Scrape metadata of content and store it");
        try {
            Namespace ns = parser.parseArgs(args);
            new Main(ns.getString("config"), ns.getBoolean("enablecompletesound"), ns.getBoolean("metadata"));
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(0);
        }
    }
}
