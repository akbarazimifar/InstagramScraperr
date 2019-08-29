# Scraper

Java commandline Instagram scraper to easily download new content

***Supported content***

`Posts, Stories, Profile pictures (fullsize), Collections`

***Features***

* Scrape multiple Instagram accounts
* Use multiple accounts to scrape to avoid bot detection
* Scrape private accounts (if you have an account which follows the private one)
* Proxy support
* Database storage to mark downloaded content
* Dynamic foldermanagement to easily sort the downloaded content
* Sessionstorage 

# Building


# How to setup a config 

* [Instagram](config_example/instagram_config_tutorial.md)

# How to use

Open the terminal/CMD in the folder where the binary is located and type


`java -jar scraper.jar /path/to/instagram_config.json [--enablecompletesound] [--metadata]`

(you don't need to supply both arguments if you only want to launch one module)

* `--enablecompletesound` plays a sound when all downloads are finished
* `--metadata` scrapes metadata

(Optional: Put the binary and a config file in the same folder)
