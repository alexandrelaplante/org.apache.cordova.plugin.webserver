org.apache.cordova.plugin.webserver
===================================

A simple webserver plugin for Cordova / PhoneGap on Android using NanoHTTPD.

## Installation

    cordova plugin add https://github.com/alexandrelaplante/org.apache.cordova.plugin.webserver.git

Add the following permissions to AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

## Usage

```javascript

window.plugins.webserver.start(callback, [{'port' : 8080}]);

function callback (request) {

    if (request.constructor == String){
        // The first thing sent to the callback is the connection address.

        alert("direct your browser to " + request);

    } else {
        // All further calls are http requests.

        var response = [{
            'status' : 200,
            'mimetype' : 'text/html',
            'data' : '<html><head></head><body>'+ JSON.stringify(request) +'</body></html>'
        }];

        window.plugins.webserver.respond(response);
    }
}
```