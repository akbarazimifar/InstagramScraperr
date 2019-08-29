# How to setup a config file


Start by creating a file (for example config.json). You can put the file in the same folder as the downloader binary.

You can also use [this democonfig.json](https://gitlab.com/insta-dl/InstagramSaver/blob/master/democonfig.json) and modify it according to your needs, or use [this](https://gitlab.com/Moquo/instagramsaver-config-generator) config generation tool.


Open the config file and insert the following structure:

```
{
    "instagram_logins": [

    ],

    "database_path": "%PATH%",

    "download_users": [

    ]
}
```

Now you can replace `%PATH` with the path to your database (it will be created if it doesn't exist yet).

This database will be used by the saver to determine if a certain post/story/profilepicture has already been downlaoded.
For example, if your downloader binary is located in `/home/insta-dl/instasaver/`, you could use `/home/insta-dl/instasaver/database.sqlite`

After that, you can start adding instagram accounts which will be used to send requests to instagram api endpoints.

Go to
```
    "instagram_logins": [

    ],
```

and insert an account by inserting
```
    "instagram_logins": [
        {
            "username": "%INSTAGRAM_USERNAME%",
            "password": "%INSTAGRAM_PASSWORD%"
        }
    ],
```

If you want to use a proxy, you just have to add a proxy object to the json

```
    "instagram_logins": [
        {
            "username": "%INSTAGRAM_USERNAME%",
            "password": "%INSTAGRAM_PASSWORD%",
            "proxy": {
                "ip": "%PROXY_IP%",
                "port": %PROXY_PORT%,
                "type": "%PROXY_TYPE%",
                "auth": {
                    "username": "%PROXY_USERNAME%",
                    "password": "%PROXY_PASSWORD%"
                }
            }
        }
    ],
```

Supported proxy types: `HTTP, SOCKS`

If your proxyserver doesn't require authentication, you can just remove the auth part (also remember to remove the comman after the proxy type)

```
    ,
    auth": {
        "username": "%PROXY_USERNAME%",
        "password": "%PROXY_PASSWORD%"
    }
```                

To add more accounts, just repeat this process:

```

    "instagram_logins": [
        {
            "username": "%INSTAGRAM_USERNAME%",
            "password": "%INSTAGRAM_PASSWORD%",
            "proxy": {
                "ip": "%PROXY_IP%",
                "port": %PROXY_PORT%,
                "type": "%PROXY_TYPE%",
                "auth": {
                    "username": "%PROXY_USERNAME%",
                    "password": "%PROXY_PASSWORD%"
                }
            }
        },
        {
            "username": "%INSTAGRAM_USERNAME2%",
            "password": "%INSTAGRAM_PASSWORD2%"
        },
        {
            "username": "%INSTAGRAM_USERNAME3%",
            "password": "%INSTAGRAM_PASSWORD3%",
            "proxy": {
                "ip": "%PROXY_IP2%",
                "port": %PROXY_PORT2%,
                "type": "%PROXY_TYPE2%"
            }
        }
    ],
```

To add accounts which should be scraped, go to

```
    "download_users": [

    ]
```    

And insert

```
    "download_users": [
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
            "posts",
            "stories",
            "profilePictures"
          ],
          "instagram_login": "%INSTAGRAM_USERNAME%"
        }
    ]
```    

* `%INSTAGRAM_USER_TO_SCRAPE%` username of the user you want to scrape
* `%INSTAGRAM_USERID%` userid of the user to scrape, you can retrieve it by going to [this page](https://codeofaninja.com/tools/find-instagram-user-id) or directly use the [instragram endpoint](https://www.instagram.com/web/search/topsearch/?query=%USERNAME%).
* `overwrite_files` determines of files that already exist should be overwritten
* `%SAVE_FOLDER%` the folder where the scraped content should be saved to (you should use an absolute path like, for example /home/insta-dl/output/thisuser)

    If you want to dynamically create a folder with the current time, you can include `%current_datetime{HH:mm dd.MM.yyyy}%` in your path. This part will be replaced with the current time/date.
    The part between the curly brackets is the datetime format, for example, `HH:mm dd.MM.yyyy` equals "11:45 10.02.2019".
    To learn more about the datetime format, go to [this page](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)

    Example: `/home/insta-dl/output/thisuser/%current_datetime{HH:mm dd.MM.yyyy}%` creates a folder with the current time in the folder `thisuser`.

* Content selection

    `>=v1.4`

    In version v1.4, this has part of the config has been changed to:

    ```
    "save": [
        {
          "type": "posts",
          "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
        },
        {
          "type": "stories",
          "fileNameScheme": "%type%_%owner%_%urlfile%"
        },
        {
          "type": "profilepictures",
          "fileNameScheme": "%type%_%owner%_%urlfile%"
        },
        {
          "type": "collections",
          "fileNameScheme": "%collection_name%_%type%_%owner%_%urlfile%"
        }
    ]
    ```

    This scrapes posts, stories, profilepictures and collections.

    You can also change the name of the outputted files using the `fileNameScheme`:

    * `%type%` will be replaced with the type of content scraped
    * `%owner%` will be replaced with the name of the instagram account owning this media
    * `%urlfile%` will be replaced with the filename of the direct url of the downloaded media


   For posts you can also use `%shortcode%` which will be replaced with the instagram shortcode (instagram.com/p/SHORTCODE) of the post.
   You should include `%urlfile%` in your post fileNameScheme, or else, multimedia posts won't be downloaded correctly!

   For collections, you can use `%collection_name%` which will be replaced with the name of the collection.

   You can also use `/` in the `fileNameScheme` to create a folder (Example: `%collection_name%/%type%_%owner%_%urlfile%` will create an own folder for every collection)



   Examples:

    * only scrapes posts
    
    ```
        "save": [
            {
              "type": "posts",
              "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
            }
        ]
    ```

    * output format will be `TEST_post_BuxydR7nQ3I` (example, `BuxydR7nQ3I` is the shortcode)
    
    ```
        "save": [
            {
              "type": "posts",
              "fileNameScheme": "TEST_%type%_%shortcode%"
            }
        ]
    ```


    *  scrape posts and stories


    ```
    "save": [
        {
          "type": "posts",
          "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
        },
        {
          "type": "stories",
          "fileNameScheme": "%type%_%owner%_%urlfile%"
        }
    ]
    ```

    After the last `}`, there is **NO** comma!


    `<=v1.4`

    to change what type of content is scraped, you can change

    ```
        "save": [
            "posts",
            "stories",
            "profilePictures"
        ],
    ```

    to your liking. The order of the contenttypes doesn't matter, but you should watch out for the comma.

    Examples:

    * only scrapes posts
    ```
        "save": [
            "posts"
        ],
    ```

    * scrapes posts and stories
    ```
        "save": [
            "posts",
            "stories"
        ],
    ```

    * since version v1.2, collection scraping is supported (example scrapes posts, collections and stories)

    ```
        "save": [
            "posts",
            "collections",
            "stories"
        ],
    ```


* `%INSTAGRAM_USERNAME%` username of the account which should be used to scrape this account. The account must be defined in

    
    ```
        "instagram_logins": [

        ],
    ```

    
    (see above)


Repeat this for every account you want to scrape:

`>=1.4`
    

    
    "download_users": [
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
                {
                  "type": "posts",
                  "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
                },
                {
                  "type": "stories",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "profilepictures",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "collections",
                  "fileNameScheme": "%collection_name%_%type%_%owner%_%urlfile%"
                }
          ],
          "instagram_login": "%INSTAGRAM_USERNAME%"
        },
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
                {
                  "type": "posts",
                  "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
                },
                {
                  "type": "stories",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "profilepictures",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "collections",
                  "fileNameScheme": "%collection_name%_%type%_%owner%_%urlfile%"
                }
          ],
          "instagram_login": "%INSTAGRAM_USERNAME2%"
        },
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
                {
                  "type": "posts",
                  "fileNameScheme": "%shortcode%_%type%_%owner%_%urlfile%"
                },
                {
                  "type": "stories",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "profilepictures",
                  "fileNameScheme": "%type%_%owner%_%urlfile%"
                },
                {
                  "type": "collections",
                  "fileNameScheme": "%collection_name%_%type%_%owner%_%urlfile%"
                }
          ],
          "instagram_login": "%INSTAGRAM_USERNAME3%"
        }
    ]
    
    
    
 `<v1.4`


    
    "download_users": [
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
            "posts",
            "stories",
            "profilePictures"
          ],
          "instagram_login": "%INSTAGRAM_USERNAME%"
        },
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
            "posts",
            "stories",
            "profilePictures"
          ],
          "instagram_login": "%INSTAGRAM_USERNAME2%"
        },
        {
          "username": "%INSTAGRAM_USER_TO_SCRAPE%",
          "id": "%INSTAGRAM_USERID%",
          "overwrite_files": false,
          "save_folder": "%SAVE_FOLDER%",
          "save": [
            "posts",
            "stories",
            "profilePictures"
          ],
          "instagram_login": "%INSTAGRAM_USERNAME3%"
        }
    ]
 	
