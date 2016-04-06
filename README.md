# TweetDucker
A sample [Fabric] app for Android.

![](doc/login.png?raw=true)


## Credentials
The Fabric integrations require a `app/fabric.properties` file containing secrets which has not been
included in this repository. Use the [Fabric plugin] to onboard or copy/paste your own keys into
this file.

It should have the following format:

    apiSecret=<64 character hex string>
    twitterPluginId=<14 character hex string>

The Login with Twitter and Twitter API functionality requires a Twitter Application consumer key
and secret to be provided in `app/src/main/assets/app.properties`. This is a Java properties file
and should have the following format.

    twitter_key=<24 character base 64 string>
    twitter_secret=<50 character base 64 string>


## License

    Copyright [2016] [Daithi O Crualaoich]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[Fabric]: https://fabric.io
[Fabric plugin]: https://get.fabric.io/android