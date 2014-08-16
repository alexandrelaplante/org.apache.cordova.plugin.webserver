
var exec = require("cordova/exec");

var webserver = {
    start: function (callback, args) {
        exec(callback, function(err){alert(err);},'CordovaWebserver', 'start', args);
    },

    respond: function (args) {
        exec(function(res){}, function(err){alert(err);},'CordovaWebserver', 'respond', args);
    },
}

module.exports = webserver;