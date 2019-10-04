package fe.igscraper;

import fe.binaryversion.BinaryVersion;
import fe.igscraper.instagram.util.Util;
import fe.logger.Logger;
import fe.mediaplayer.Player;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Main {
    private static final String APP_NAME = "InstagramScraper";

    private Main(String configPath, boolean enableDownloadCompleteSound, boolean metadata) {
        try {
            BinaryVersion binaryVersion = new BinaryVersion(Main.class, true).loadVersion().checkUpdate();

            Logger logger = new Logger(String.format("%s-%s", APP_NAME, binaryVersion.getVersion().toString()), true);
            logger.print(Logger.Type.INFO, "Full version: %s", binaryVersion.toString());
            logger.print(Logger.Type.INFO, "Sound: %b, Metadata: %b", enableDownloadCompleteSound, metadata);
            logger.print(Logger.Type.INFO, "Loading config from %s", configPath);

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
            logger.print(Logger.Type.INFO, "Elapsed time: %s", Duration.of(time, ChronoUnit.MILLIS).toString().substring(2));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (enableDownloadCompleteSound) {
            new Player(Main.class.getResource("/download-complete.wav").toString()).addFinishedListener((v, o, n) -> System.exit(0)).play();
        } else {
            System.exit(0);
        }
    }

    public static void main(final String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor(APP_NAME).build().defaultHelp(true).description("Scrape a list of given Instagram accounts incrementally");
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
