# Android auto update plugin for Phonegap/Cordova 1.0.0 #


## Adding the Plugin to your project ##

The plugin conforms to the Cordova plugin specification, it can be installed
using the Cordova / Phonegap command line interface.

```
phonegap plugin add https://github.com/lovelyelfpop/androidUpdatePlugin

cordova plugin add https://github.com/lovelyelfpop/androidUpdatePlugin
```

## version.txt ##

Modify the "version.txt", and put it on your server.

The encoding of this file must be UTF-8.

`verCode`: - version Code, must be an integer

`verName`: - version Name

`apkPath`: - the url of the apk file

`releaseNote`: - release Note


## Using the plugin ##

To use, call the method as follows:

```javascript
   plugins.update.updateApp(upgradeUrl, success, failure);
```

`upgradeUrl`: - the url of the file "version.txt" , 'http://www.aio7.com:8189/version.txt' for example.