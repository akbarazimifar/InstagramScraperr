# InstagramScraper

Java commandline Instagram scraper to easily download new content

***Supported content***

`Posts`, `Stories`, `Profile pictures (fullsize)`, `Collections`

***Features***

* Scrape multiple Instagram accounts
* Use multiple accounts to scrape to avoid bot detection
* Scrape private accounts (if you have an account which follows the private one)
* Proxy support
* Database storage to mark downloaded content
* Dynamic foldermanagement to easily sort the downloaded content
* Sessionstorage 

***Requirements***

* Java 8
* (on linux: OpenJFX 8)


***Downloads***

* [Releases](https://gitlab.com/grrfe/InstagramScraper/-/releases)

***Building***

Prerequisites: `Git`, `Gradle`, `JDK 8`, `(linux: OpenJFX 8)`

* `git clone https://gitlab.com/grrfe/InstagramScraper.git`
* `cd InstagramScraper`
* `gradle jar`

Binary will be located in `build/libs`

***[How to setup a config ](config_example/instagram_config_tutorial.md)***

***Usage***
```
grrfe@feowo:~$ java -jar InstagramScraper-LATEST.jar
usage: InstagramScraper [-h] [-s] [-m] config

Scrape a list of given Instagram accounts incrementally

positional arguments:
  config                 Path to the config used by the scraper

named arguments:
  -h, --help             show this help message and exit
  -s, --enablecompletesound
                         Play a sound when scraping is done (default: false)
  -m, --metadata         Scrape metadata of content and store it (default: false)
```

(Optional: Put the binary and a config file in the same folder)
