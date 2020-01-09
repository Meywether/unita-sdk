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

import android.util.Log;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.SoniTalkMessage;
import at.ac.fhstp.sonitalk.SoniTalkMultiMessage;
import at.ac.fhstp.sonitalk.utils.DecoderUtils;
import at.ac.fhstp.sonitalk.utils.EncoderUtils;
import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.Broadcast;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.StatusMessage;
import at.floriantaurer.unitabeaconmodule.TextMessage;
import at.floriantaurer.unitabeaconmodule.TokenMessage;
import at.floriantaurer.unitabeaconmodule.UnitaMessage;
import at.floriantaurer.unitabeaconmodule.UrlMessage;
import at.floriantaurer.unitabeaconmodule.User;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class MessageUtils {

    public static SoniTalkMultiMessage convertUnitaMessageToSoniTalkMultiMessage(UnitaMessage unitaMessage){
        byte[] unitaMessageBody = unitaMessage.getMessageBody().getMessageBodyRaw();
        byte[] soniTalkMessageBody = null;
        soniTalkMessageBody = ArrayUtils.addAll(soniTalkMessageBody, EncoderUtils.intToByteArray(unitaMessage.getHeader().getHeaderMessageCode()));
        soniTalkMessageBody = ArrayUtils.addAll(soniTalkMessageBody, EncoderUtils.intToByteArray(unitaMessage.getHeader().getSender().getId()));
        soniTalkMessageBody = ArrayUtils.addAll(soniTalkMessageBody, EncoderUtils.intToByteArray(unitaMessage.getHeader().getReceiver().getId()));
        soniTalkMessageBody = ArrayUtils.addAll(soniTalkMessageBody, unitaMessageBody);

        return new SoniTalkMultiMessage(soniTalkMessageBody);
    }

    public static UnitaMessage convertSoniTalkMessageToUnitaMessage(SoniTalkMessage soniTalkMessage){
        byte[] senderIdArray = new byte[1];
        System.arraycopy(soniTalkMessage.getMessage(), 0, senderIdArray,0, 1);
        byte[] receiverIdArray = new byte[1];
        System.arraycopy(soniTalkMessage.getMessage(), 1, receiverIdArray,0, 1);
        byte[] headerMessageCodeArray = new byte[1];
        System.arraycopy(soniTalkMessage.getMessage(), 2, headerMessageCodeArray,0, 1);
        byte[] messageBodyArray = new byte[1];
        System.arraycopy(soniTalkMessage.getMessage(), 3, messageBodyArray,0, soniTalkMessage.getMessage().length-3);

        int senderId = senderIdArray[0] & (0xff);
        int receiverId = receiverIdArray[0] & (0xff);
        int headerMessageCode = headerMessageCodeArray[0] & (0xff);
        String messageBody = DecoderUtils.byteToUTF8(messageBodyArray);
        byte[] messageBodyRaw = messageBodyArray;
        User sender = new User(senderId, "Moto");
        Beacon receiver = new Beacon(receiverId, "RPi3");

        return new UnitaMessage(sender, receiver, messageBodyArray);
    }

    public static UnitaMessage convertSoniTalkMultiMessageToUnitaMessage(SoniTalkMultiMessage soniTalkMultiMessage) {
        byte[] headerMessageCodeArray = new byte[1];
        System.arraycopy(soniTalkMultiMessage.getMessage(), 0, headerMessageCodeArray,0, 1);
        byte[] senderIdArray = new byte[1];
        System.arraycopy(soniTalkMultiMessage.getMessage(), 1, senderIdArray,0, 1);
        byte[] receiverIdArray = new byte[1];
        System.arraycopy(soniTalkMultiMessage.getMessage(), 2, receiverIdArray,0, 1);
        byte[] messageBodyArray = new byte[soniTalkMultiMessage.getMessage().length-3];
        System.arraycopy(soniTalkMultiMessage.getMessage(), 3, messageBodyArray,0, soniTalkMultiMessage.getMessage().length-3);

        int senderId = senderIdArray[0] & (0xff);
        int receiverId = receiverIdArray[0] & (0xff);
        int headerMessageCode = headerMessageCodeArray[0] & (0xff);
        Peer sender = new Peer(senderId);
        Peer receiver = new Peer(receiverId);

        return new UnitaMessage(headerMessageCode, sender, receiver, messageBodyArray);
    }

    public static TextMessage convertUnitaMessageToTextMessage(UnitaMessage unitaMessage){
        byte[] communicationPartnerIdArray = new byte[1];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 0, communicationPartnerIdArray,0, 1);
        byte[] messageBodyArray = new byte[unitaMessage.getMessageBody().getMessageBodyRaw().length-1];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 1, messageBodyArray,0, unitaMessage.getMessageBody().getMessageBodyRaw().length-1);

        int communicationPartnerId = communicationPartnerIdArray[0] & (0xff);
        Peer communicationPartner = new Peer(communicationPartnerId);

        return new TextMessage(unitaMessage.getHeader().getSender(), unitaMessage.getHeader().getReceiver(), communicationPartner, messageBodyArray);
    }

    public static CommandMessage convertUnitaMessageToCommandMessage(UnitaMessage unitaMessage){
        byte[] messageBodyArray = new byte[unitaMessage.getMessageBody().getMessageBodyRaw().length];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 0, messageBodyArray,0, unitaMessage.getMessageBody().getMessageBodyRaw().length);

        return new CommandMessage(unitaMessage.getHeader().getSender(), unitaMessage.getHeader().getReceiver()/*, DecoderUtils.byteToUTF8(messageBodyArray)*/, messageBodyArray);
    }

    public static UrlMessage convertUnitaMessageToUrlMessage(UnitaMessage unitaMessage){
        byte[] messageBodyArray = new byte[unitaMessage.getMessageBody().getMessageBodyRaw().length];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 0, messageBodyArray,0, unitaMessage.getMessageBody().getMessageBodyRaw().length);

        return new UrlMessage(unitaMessage.getHeader().getSender(), unitaMessage.getHeader().getReceiver()/*, DecoderUtils.byteToUTF8(messageBodyArray)*/, messageBodyArray);
    }

    public static TokenMessage convertUnitaMessageToTokenMessage(UnitaMessage unitaMessage){
        byte[] messageBodyArray = new byte[unitaMessage.getMessageBody().getMessageBodyRaw().length];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 0, messageBodyArray,0, unitaMessage.getMessageBody().getMessageBodyRaw().length);

        return new TokenMessage(unitaMessage.getHeader().getSender(), unitaMessage.getHeader().getReceiver()/*, DecoderUtils.byteToUTF8(messageBodyArray)*/, messageBodyArray);
    }

    public static StatusMessage convertUnitaMessageToStatusMessage(UnitaMessage unitaMessage){
        byte[] messageBodyArray = new byte[unitaMessage.getMessageBody().getMessageBodyRaw().length];
        System.arraycopy(unitaMessage.getMessageBody().getMessageBodyRaw(), 0, messageBodyArray,0, unitaMessage.getMessageBody().getMessageBodyRaw().length);

        return new StatusMessage(unitaMessage.getHeader().getSender(), unitaMessage.getHeader().getReceiver(), messageBodyArray);
    }

    /*Convert additional data from subclasses*/
    public static byte[] convertTextMessageAdditionalParametersToByteArray(Peer communicationPartner, byte[] messageBody){
        byte[] additionalParameterByteArray = null;

        additionalParameterByteArray = ArrayUtils.addAll(additionalParameterByteArray, EncoderUtils.intToByteArray(communicationPartner.getId()));
        additionalParameterByteArray = ArrayUtils.addAll(additionalParameterByteArray, messageBody);

        return additionalParameterByteArray;
    }

    public static int calculateNumberOfPackets(SoniTalkMultiMessage message, SoniTalkConfig config){
        int numOfBytes = config.getnMessageBlocks()*(config.getnFrequencies()/8)-2;
        int fixedHeaderSize = 3; //4 Byte reserved = 1 Byte messageId, 1 Byte packetId, 1 Byte numberOfPackets
        final byte[] bytes = message.getMessage();
        byte[] headerBytesPlaceholder = new byte[fixedHeaderSize];
        byte[] checkSizeByte = ArrayUtils.addAll(bytes, headerBytesPlaceholder);
        if(EncoderUtils.isAllowedByteArraySize(checkSizeByte, config)){
            return 1;
        } else{
            if(bytes.length%(numOfBytes-fixedHeaderSize)==0){
                return bytes.length/(numOfBytes-fixedHeaderSize);
            }else{
                return bytes.length/(numOfBytes-fixedHeaderSize)+1;
            }
        }
    }
}
