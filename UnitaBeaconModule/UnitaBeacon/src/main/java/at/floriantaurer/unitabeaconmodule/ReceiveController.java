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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.ac.fhstp.sonitalk.SoniTalkDecoder;
import at.ac.fhstp.sonitalk.SoniTalkMessage;
import at.ac.fhstp.sonitalk.SoniTalkMultiMessage;
import at.ac.fhstp.sonitalk.SoniTalkPermissionsResultReceiver;
import at.ac.fhstp.sonitalk.exceptions.DecoderStateException;
import at.ac.fhstp.sonitalk.utils.DecoderUtils;
import at.floriantaurer.unitabeaconmodule.utils.MessageUtils;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;

public class ReceiveController implements SoniTalkDecoder.MessageListener, SoniTalkPermissionsResultReceiver.Receiver{

    public static ReceiveController instance;

    SoniTalkDecoder soniTalkDecoder;
    private SoniTalkContext soniTalkContext;
    private SoniTalkPermissionsResultReceiver soniTalkPermissionsResultReceiver;
    private ResultReceiver unitaSdkListener;

    public static final int ON_RECEIVING_REQUEST_CODE = 2002;

    private int samplingRate = 44100;

    private ReceiveController(){
    }

    public static ReceiveController getInstance() { //getInstance method for Singleton pattern
        if(instance == null) { //if no instance of MicCapture is there create a new one, otherwise return the existing
            instance = new ReceiveController();
        }
        return instance;
    }

    public void init(ResultReceiver sdkListener){
        if (instance != null) {
            soniTalkPermissionsResultReceiver = new SoniTalkPermissionsResultReceiver(new Handler());
            soniTalkPermissionsResultReceiver.setReceiver(this);
            this.unitaSdkListener = sdkListener;
        }
    }


    public interface BeaconListener {

        void onUnitaMessageReceived(UnitaMessage receivedMessage);

        void onTextMessageReceived(TextMessage receivedMessage);

        void onCommandMessageReceived(CommandMessage receivedMessage);

        void onTokenMessageReceived(TokenMessage receivedMessage);

        void onUrlMessageReceived(UrlMessage receivedMessage);

        void onStatusMessageReceived(StatusMessage statusMessage);

        void onUnitaMessageError(String errorMessage);

    }

    private List<BeaconListener> beaconListeners = new ArrayList<>();

    public void startReceiving(Context context, UnitaSettings unitaSettings){
        if (soniTalkContext == null) {
            soniTalkContext = SoniTalkContext.getInstance(context, soniTalkPermissionsResultReceiver);
        }

        try{
            int nMessageBlocks = (unitaSettings.getnMessageBlocks()+2) / 2; // Default is 10 (transmitting 20 bytes with 16 frequencies)
            unitaSettings.setnMessageBlocks(nMessageBlocks);
            soniTalkDecoder = soniTalkContext.getDecoder(samplingRate, unitaSettings);
            soniTalkDecoder.addMessageListener(this);

            soniTalkDecoder.receiveBackground(ON_RECEIVING_REQUEST_CODE);
        } catch (DecoderStateException e) {
            Log.e("RecConStartReceiving",context.getString(R.string.decoder_exception_state) + e.getMessage());
        }
    }

    public void stopReceiveing(){
        if (soniTalkDecoder != null) {
            soniTalkDecoder.stopReceiving();
        }
        soniTalkDecoder = null;
    }


    public void checkMessageTypeAndNotifyAccordingMessage(UnitaMessage unitaMessage){
        for(BeaconListener listener: beaconListeners) {
            switch (unitaMessage.getHeader().getHeaderMessageCode()){
                case 0:
                    listener.onUnitaMessageReceived(unitaMessage);
                    break;
                case 1:
                    TextMessage textMessage = MessageUtils.convertUnitaMessageToTextMessage(unitaMessage);
                    listener.onTextMessageReceived(textMessage);
                    break;
                case 2:
                    CommandMessage commandMessage = MessageUtils.convertUnitaMessageToCommandMessage(unitaMessage);
                    listener.onCommandMessageReceived(commandMessage);
                    break;
                case 3:
                    UrlMessage urlMessage = MessageUtils.convertUnitaMessageToUrlMessage(unitaMessage);
                    listener.onUrlMessageReceived(urlMessage);
                    break;
                case 4:
                    TokenMessage tokenMessage = MessageUtils.convertUnitaMessageToTokenMessage(unitaMessage);
                    listener.onTokenMessageReceived(tokenMessage);
                    break;
                case 5:
                    StatusMessage statusMessage = MessageUtils.convertUnitaMessageToStatusMessage(unitaMessage);
                    listener.onStatusMessageReceived(statusMessage);
                    break;
                default:
                    listener.onUnitaMessageReceived(unitaMessage);
                    break;
            }
        }
    }

    public void notifyUnitaMessageError(String errorMessage){
        for(BeaconListener listener: beaconListeners) {
            listener.onUnitaMessageError(errorMessage);
        }
    }

    public void addMessageListener(ReceiveController.BeaconListener listener) {
        this.beaconListeners.add(listener);
    }

    @Override
    public void onMessageReceived(final SoniTalkMessage receivedMessage) {
        if(receivedMessage.isCrcCorrect()){
            UnitaMessage unitaMessage = MessageUtils.convertSoniTalkMessageToUnitaMessage(receivedMessage);

            checkMessageTypeAndNotifyAccordingMessage(unitaMessage);
        } else {
        }
    }

    @Override
    public void onMessageReceived(final SoniTalkMultiMessage receivedMessage){
        if(receivedMessage.isCrcCorrect()) {
            UnitaMessage unitaMessage = MessageUtils.convertSoniTalkMultiMessageToUnitaMessage(receivedMessage);
            checkMessageTypeAndNotifyAccordingMessage(unitaMessage);
        } else {
        }
    }

    @Override
    public void onDecoderError(String errorMessage) {
        notifyUnitaMessageError(errorMessage);
    }

    @Override
    public void onSoniTalkPermissionResult(int resultCode, Bundle resultData) {
        unitaSdkListener.send(resultCode, resultData);

    }
}
