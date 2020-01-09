/*
 * Copyright (c) 2019. Florian Taurer.
 *
 * This file is part of Unita SDK.
 *
 * Unita is free a SDK: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unita is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Unita.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.floriantaurer.unitabeaconmodule;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import at.ac.fhstp.sonitalk.SoniTalkDecoder;
import at.floriantaurer.unitabeaconmodule.BuildConfig;
import at.floriantaurer.unitabeaconmodule.utils.LocationController;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketController {

    private static SocketController instance;

    Socket socket;

    private SocketController() {
        try {
            socket = IO.socket(BuildConfig.SERVER_URL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("SocketController", "Socket.Event_Connect");
            }

        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("SocketController", "Socket.Event_Connect");
            }

        });

        socket.on("requestLocation", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("SocketController", "requestLocation");
                JSONObject dataResponse = (JSONObject) args[0];
                String socketId = null;
                try {
                    socketId = dataResponse.getString("socketId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LocationController locationController = LocationController.getInstance();
                locationController.requestLocationForServer(socketId);
                socket.off("requestLocation", this);
            }
        });

        socket.on("requestLocationCheck", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("SocketController", "requestLocation");
                JSONObject dataResponse = (JSONObject) args[0];
                String socketId = null;
                try {
                    socketId = dataResponse.getString("socketId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LocationController locationController = LocationController.getInstance();
                locationController.requestLocationCheckForServer(socketId);
            }
        });

        socket.connect();
    }

    public static SocketController getInstance() {
        if(instance == null) {
            instance = new SocketController();
        }
        return instance;
    }

    public interface LoginListener {

        void onLoginResponse(JSONObject loginResponse);

    }

    private List<SocketController.LoginListener> loginListeners = new ArrayList<>();

    public void addLoginListener(SocketController.LoginListener listener) {
        this.loginListeners.add(listener);
    }

    public interface SocketListener {

        void onSendMessageResponse(JSONObject loginResponse);

        void onGetUrlForCommandGetAllMessagesResult(JSONObject urlResponse, JSONObject senderResponse);

        void onGetUrlForCommandgetAllBroadcastMessages(JSONObject urlResponse, JSONObject senderResponse);

        void onGetUrlForCommandgetAllMessagesFromContact(JSONObject urlResponse, JSONObject senderResponse);

    }

    private List<SocketController.SocketListener> socketListener = new ArrayList<>();

    public void addMessageListener(SocketController.SocketListener listener) {
        Log.d("SocketController", "addMessageListener");
        this.socketListener.add(listener);
    }

    public void loginBeacon(String beaconName){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", beaconName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("loginBeacon", jsonObject);

        socket.on("loginBeaconResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONArray jArray = (JSONArray) args[0];
                JSONObject loginResponse = null;
                try {
                    loginResponse = jArray.getJSONObject(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifyLoginResponse(loginResponse);

                socket.off("loginBeaconResult", this);
            }
        });
    }

    public void notifyLoginResponse(JSONObject loginResponse){
        for(SocketController.LoginListener listener: loginListeners) {
            listener.onLoginResponse(loginResponse);
        }
    }

    public void notifySaveMessageResponse(JSONObject saveMessageResponse){
        for(SocketController.SocketListener listener: socketListener) {
            listener.onSendMessageResponse(saveMessageResponse);
        }
    }

    public void notifyGetUrlForCommandGetAllMessagesResult(JSONObject urlResponse, JSONObject senderResponse){
        for(SocketController.SocketListener listener: socketListener) {
            listener.onGetUrlForCommandGetAllMessagesResult(urlResponse, senderResponse);
        }
    }

    public void notifyGetUrlForCommandgetAllBroadcastMessages(JSONObject urlResponse, JSONObject senderResponse){
        for(SocketController.SocketListener listener: socketListener) {
            listener.onGetUrlForCommandgetAllBroadcastMessages(urlResponse, senderResponse);
        }
    }

    public void notifyGetUrlForCommandgetAllMessagesFromContact(JSONObject urlResponse, JSONObject senderResponse){
        for(SocketController.SocketListener listener: socketListener) {
            listener.onGetUrlForCommandgetAllMessagesFromContact(urlResponse, senderResponse);
        }
    }

    public void saveMessage(JSONObject message){
        socket.emit("sendMessage", message);


        socket.on("sendMessageResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                //Log.d("loginBeaconResult", String.valueOf(args));
                JSONArray jArray = (JSONArray) args[0];
                JSONObject saveMessageResponse = null;
                try {
                    saveMessageResponse = jArray.getJSONObject(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifySaveMessageResponse(saveMessageResponse);

                socket.off("sendMessageResult", this);
            }
        });
    }

    public void getUrlForCommandGetAllMessages(JSONObject message){
        Log.d("SocketController", "getUrlForCommandGetAllMessages");
        socket.emit("getUrlForCommandGetAllMessages", message);


        socket.on("getUrlForCommandGetAllMessagesResult", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("SocketController", "getUrlForCommandGetAllMessagesResult");
                //Log.d("loginBeaconResult", String.valueOf(args));
                JSONObject dataResponse = (JSONObject) args[0];
                JSONObject urlResponse = dataResponse;
                JSONObject senderResponse = null;

                    //urlResponse = dataResponse;

                try {
                    senderResponse = dataResponse.getJSONArray("sender").getJSONObject(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifyGetUrlForCommandGetAllMessagesResult(urlResponse, senderResponse);

                socket.off("getUrlForCommandGetAllMessagesResult", this);
            }
        });
    }

    public void sendLocationUpdateToServer(JSONObject message) {
        socket.emit("sendLocation", message);
    }

    public void sendLocationCheckUpdateToServer(JSONObject message) {
        socket.emit("sendLocationCheck", message);
    }
}
