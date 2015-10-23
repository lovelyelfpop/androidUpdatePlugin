var exec = require('cordova/exec');

var Update = function() { };

Update.prototype = {
    updateApp: function(url, successCallback, errorCallback) {
        exec(successCallback, errorCallback, "UpdateApp", 'checkAndUpdate', [url]);
    },
    getVersionName: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, "UpdateApp", 'getVersionName', []);
    }
};

module.exports = new Update();
