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

package at.floriantaurer.unitabeaconmodule.utils;

// Base Stitch Packages
import android.content.Context;
import android.util.Log;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;

// Packages needed to interact with MongoDB and Stitch
import com.mongodb.client.MongoClient;

// Necessary component for working with MongoDB Mobile
import com.mongodb.stitch.android.services.mongodb.local.LocalMongoDbService;

import org.bson.Document;
import org.bson.types.Binary;

import java.util.ArrayList;
import java.util.List;

import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.TextMessage;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;

public class LocalMessageDatabase {
    private static LocalMessageDatabase instance;

    StitchAppClient client;
    MongoClient mobileClient;
    MongoCollection<Document> localCollection;
    Context currentContext;

    private LocalMessageDatabase(){
        connectToLocalDatabase();
    }

    public static LocalMessageDatabase getInstance() {
        if(instance == null) {
            instance = new LocalMessageDatabase();
        }
        return instance;
    }

    public void setCurrentContext(Context context){
        this.currentContext = context;
    }

    public interface LocalMessageDatabaseListener {

        void onLocalTextMessageReceived(TextMessage receivedLocalTextMessage);

    }

    private List<LocalMessageDatabase.LocalMessageDatabaseListener> localMessageDatabaseisteners = new ArrayList<>();

    public void notifyLocalTextMessageReceived(TextMessage receivedLocalTextMessage){
        for(LocalMessageDatabaseListener listener: localMessageDatabaseisteners) {
            listener.onLocalTextMessageReceived(receivedLocalTextMessage);
        }
    }

    public void addMessageListener(LocalMessageDatabase.LocalMessageDatabaseListener listener) {
        this.localMessageDatabaseisteners.add(listener);
    }

    private void connectToLocalDatabase() {
        client = Stitch.initializeDefaultAppClient("UnitaLocalMongoDB");

        mobileClient = client.getServiceClient(LocalMongoDbService.clientFactory);

        localCollection = mobileClient.getDatabase("unita_db").getCollection("textmessage_private");
    }

    public void saveMessage(TextMessage textMessage) {
        Document textMessageDocument = new Document();
        textMessageDocument.put("Sender", textMessage.getHeader().getSender().getId());
        textMessageDocument.put("CommunicationPartner", textMessage.getCommunicationPartner().getId());
        textMessageDocument.put("Message", textMessage.getMessageBody().getMessageBodyRaw());
        localCollection.insertOne(textMessageDocument);
    }

    public void getLatestMessage(int senderId, int communicationPartnerId) {
        Log.d("getLatestMessage", senderId + " " + communicationPartnerId);
        FindIterable<Document> findResults = localCollection.find().sort(descending("_id")).limit(1);
        findResults.forEach((Block<? super Document>) item -> {
            Document doc = new Document(item);

            int sender = doc.getInteger("Sender");
            int communicationPartner = doc.getInteger("CommunicationPartner");
            Binary bin = doc.get("Message", org.bson.types.Binary.class);
            byte[] message = bin.getData();
            TextMessage textMessage = new TextMessage(LoginUtils.getLoggedInBeacon(currentContext), new Peer(sender), new Peer(communicationPartner), message);
            notifyLocalTextMessageReceived(textMessage);
        });
    }
}
