package org.apache.cordova.plugin.webserver;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.content.Context;

import org.apache.cordova.plugin.webserver.NanoHTTPD.Response;

public class CordovaWebserver extends CordovaPlugin{
    
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String RESPOND = "respond";
    private CallbackContext callbackContext = null; 

    private static int PORT;
    private MyHTTPD server;
    private Handler handler = new Handler();

    private LinkedBlockingQueue responses = new LinkedBlockingQueue();
    
    public CordovaWebserver () {}


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (this.callbackContext == null){
            this.callbackContext = callbackContext;  
        }

        if (action.contentEquals(START)){

            try {

                JSONObject obj = args.getJSONObject(0);
                PORT = obj.getInt("port");
        
                server = new MyHTTPD();
                server.start();

                Context context = cordova.getActivity().getApplicationContext();
                WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();
                int ipAddress = info.getIpAddress();
                String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
                formatedIpAddress = "http://" + formatedIpAddress + ":" + PORT;

                message(formatedIpAddress);

                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (action.contentEquals(STOP)) {

            server.stop();

        } else if (action.contentEquals(RESPOND)) {

            JSONObject obj = args.getJSONObject(0);
            int statusNum = obj.getInt("status");
            Response.Status status = Response.Status.OK; //default status
            String mimetype = obj.getString("mimetype");
            String data = obj.getString("data");

            // Use the correct status from value given
            for (Response.Status s : Response.Status.values()){
                if (s.getRequestStatus() == statusNum){
                    status = s;
                }
            }

            Response response = new Response(status, mimetype, data);

            try{
                responses.put(response);
                return true;
            } catch(InterruptedException e){
                e.printStackTrace();
            }

        }

        callbackContext.error("error: " + action);
        return false;
    }

    public void message(String args) {
       PluginResult res = new PluginResult(PluginResult.Status.OK, args);
       res.setKeepCallback(true);
       this.callbackContext.sendPluginResult(res);
    }
    public void message(JSONObject args) {
       PluginResult res = new PluginResult(PluginResult.Status.OK, args);
       res.setKeepCallback(true);
       this.callbackContext.sendPluginResult(res);
    }


    private class MyHTTPD extends NanoHTTPD {
        public MyHTTPD() throws IOException {
           super(PORT);
        }

        @Override
        public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms,
                                   Map<String, String> files) {

            final String f_uri = uri;
            final Method f_method = method;
            final Map<String, String> f_files = files;
            final Map<String, String> f_parms = parms;
            final Map<String, String> f_headers = headers;


            handler.post(new Runnable() {
                @Override
                public void run() {

                    // Convert the request data into JSON to send to the javascript
                    try{
                        JSONObject obj = new JSONObject();
                        obj.put("uri", f_uri);

                        String method = "";
                        if (f_method == Method.GET){ method = "GET";}
                        if (f_method == Method.PUT){ method = "PUT";}
                        if (f_method == Method.POST){ method = "POST";}
                        if (f_method == Method.DELETE){ method = "DELETE";}
                        if (f_method == Method.HEAD){ method = "HEAD";}
                        if (f_method == Method.OPTIONS){ method = "OPTIONS";}
                        obj.put("method", method);

                        JSONObject parms = new JSONObject();
                        for (Map.Entry<String, String> entry : f_parms.entrySet()) {
                          parms.put(entry.getKey(), entry.getValue());
                        }
                        obj.put("params", parms);

                        JSONObject headers = new JSONObject();
                        for (Map.Entry<String, String> entry : f_headers.entrySet()) {
                          headers.put(entry.getKey(), entry.getValue());
                        }
                        obj.put("headers", headers);

                        JSONObject body = new JSONObject();
                        for (Map.Entry<String, String> entry : f_files.entrySet()) {
                            body.put(entry.getKey(), entry.getValue());
                        }
                        obj.put("body", body);
                        message(obj);
                        
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
     

            Response response;
            try{
                response = (Response) responses.take();
            } catch(InterruptedException e){
                e.printStackTrace();
                response = new Response(Response.Status.INTERNAL_ERROR, "text/plain", "InterruptedException");
            }
            return response;
        }
    }
}
