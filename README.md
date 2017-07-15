BitlDroid-app - Android app for communicating with SMS BitlBee plugin
=========

- Use in conjunction with [BitlDroid-plugin](https://github.com/NikolaKozina/bitldroid-plugin)
- Android phone and BitlBee server must be on the same network
- *NO ENCRYPTION/AUTHENTICATION yet* - All messages are sent in the clear and anyone on the network can connect to your phone and send SMS messages

Building
--------

To build, you will need an Android SDK.

1. First initialize the Android project using the "android" program found in the SDK tools directory. In the bitldroid-app directory:

    `~/android-sdk-linux/tools/android update project --path . --target android-23`

2. Now Compile: 

    `make`

3. Then, with your phone connected through ADB, install the APK:

    `make install`

Using
--------

1. Start your BitlBee server with the BitlDroid-plugin (see [BitlDroid-plugin](https://github.com/NikolaKozina/bitldroid-plugin) )
2. Make sure your phone and the BitlBee server are on the same network.
3. Open the BitlDroid app on your phone and make sure the service is enabled (the checkbox at the top).
4. Press the probe button at the bottom.
