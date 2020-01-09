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
import android.content.SharedPreferences;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.ac.fhstp.sonitalk.SoniTalkEncoder;
import at.ac.fhstp.sonitalk.SoniTalkMessage;
import at.ac.fhstp.sonitalk.SoniTalkMultiMessage;
import at.ac.fhstp.sonitalk.SoniTalkPermissionsResultReceiver;
import at.ac.fhstp.sonitalk.SoniTalkSender;
import at.floriantaurer.unitabeaconmodule.utils.ConfigConstants;
import at.floriantaurer.unitabeaconmodule.utils.MessageUtils;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;

public class SendController implements SoniTalkPermissionsResultReceiver.Receiver{

    private SoniTalkSender soniTalkSender;
    private SoniTalkEncoder soniTalkEncoder;
    private SoniTalkMultiMessage currentMultiMessage;
    private SoniTalkContext soniTalkContext;
    private SoniTalkPermissionsResultReceiver soniTalkPermissionsResultReceiver;
    private ResultReceiver unitaSdkListener;

    private AudioTrack ultrasonicPlayer;

    private Handler resendMessage = new Handler();
    private Handler sendMessageAgain = new Handler();
    private Runnable resendRun;
    private Runnable sendMessageAgainRun;
    private int resendCounter = 0;

    public static final int ON_SENDING_REQUEST_CODE = 2001;

    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(NUMBER_OF_CORES + 1);

    public SendController(ResultReceiver sdkListener){
        soniTalkPermissionsResultReceiver = new SoniTalkPermissionsResultReceiver(new Handler());
        soniTalkPermissionsResultReceiver.setReceiver(this);
        this.unitaSdkListener = sdkListener;
    }

    public interface SendControllerListener {

        void onSendStateMessages();

    }

    private static List<SendController.SendControllerListener> sendControllerListeners = new ArrayList<>();

    public static void notifyState(){
        for(SendController.SendControllerListener listener: sendControllerListeners) {
            listener.onSendStateMessages();
        }
    }

    public static void addStateListener(SendController.SendControllerListener listener) {
        sendControllerListeners.add(listener);
    }

    private void createMessage(final Context context, final UnitaSettings unitaSettings, final UnitaMessage unitaMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        if(ultrasonicPlayer!=null){
            ultrasonicPlayer.stop();
            ultrasonicPlayer.flush();
            ultrasonicPlayer.release();
            ultrasonicPlayer = null;
        }

        if (soniTalkContext == null) {
            soniTalkContext = SoniTalkContext.getInstance(context, soniTalkPermissionsResultReceiver);
        }
        soniTalkEncoder = soniTalkContext.getEncoder(unitaSettings);

        currentMultiMessage = MessageUtils.convertUnitaMessageToSoniTalkMultiMessage(unitaMessage);

            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (soniTalkSender == null) {
                        soniTalkSender = soniTalkContext.getSender();
                    }
                    final int soniTalkSenderInterval = millisecondsBetweenMessages;
                    resendCounter = timesSend;
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, true).commit();
                    int messageLength = (unitaSettings.getnMessageBlocks()*2+2)*unitaSettings.getBitperiod()+(unitaSettings.getnMessageBlocks()*2+2)*unitaSettings.getPauseperiod();
                    int numberOfPackets = MessageUtils.calculateNumberOfPackets(currentMultiMessage, unitaSettings);

                    int sendingTime = (messageLength+soniTalkSenderInterval)*numberOfPackets;
                    int acknowledgementTime = messageLength + soniTalkSenderInterval;
                    int delayTime = sendingTime + acknowledgementTime*2 + soniTalkSenderInterval;

                    sendMessageAgainRun = new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                            boolean stillResending = sp.getBoolean(ConfigConstants.RESENDING_RUNNING, ConfigConstants.RESENDING_RUNNING_DEFAULT);
                            if (stillResending) {
                                notifyState();
                                sendMessageAgain.removeCallbacks(sendMessageAgainRun);
                            } else {
                                sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, true).commit();
                            }
                        }
                    };

                    resendRun = new Runnable() {
                        @Override
                        public void run() {
                            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                            boolean stillResending = sp.getBoolean(ConfigConstants.RESENDING_RUNNING, ConfigConstants.RESENDING_RUNNING_DEFAULT);
                            if(shouldBeResend) {
                                if (stillResending) {
                                    if (resendCounter > 0) {
                                        sendMessageAgain.removeCallbacks(sendMessageAgainRun);
                                        soniTalkSender.send(currentMultiMessage, soniTalkSenderInterval, TimeUnit.MILLISECONDS, ON_SENDING_REQUEST_CODE, unitaSettings, soniTalkEncoder);
                                        resendCounter--;
                                        resendMessage.postDelayed(resendRun, delayTime);
                                    }else{
                                        sendMessageAgain.postDelayed(sendMessageAgainRun, delayTime/2);
                                    }
                                } else {
                                    sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, true).commit();
                                    stopResending();
                                }
                            }
                        }
                    };
                    resendMessage.postDelayed(resendRun, delayTime);

                    soniTalkSender.send(currentMultiMessage, soniTalkSenderInterval, TimeUnit.MILLISECONDS, ON_SENDING_REQUEST_CODE, unitaSettings, soniTalkEncoder);
                    resendCounter--;
                }
            });
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, UnitaMessage unitaMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, TextMessage textMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        UnitaMessage unitaMessage = new UnitaMessage(textMessage.getHeader().getHeaderMessageCode(), textMessage.getHeader().getSender(), textMessage.getHeader().getReceiver(), MessageUtils.convertTextMessageAdditionalParametersToByteArray(textMessage.getCommunicationPartner(), textMessage.getMessageBody().getMessageBodyRaw()));
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, CommandMessage commandMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        UnitaMessage unitaMessage = new UnitaMessage(commandMessage.getHeader().getHeaderMessageCode(), commandMessage.getHeader().getSender(), commandMessage.getHeader().getReceiver(), commandMessage.getMessageBody().getMessageBodyRaw());
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, UrlMessage urlMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        UnitaMessage unitaMessage = new UnitaMessage(urlMessage.getHeader().getHeaderMessageCode(), urlMessage.getHeader().getSender(), urlMessage.getHeader().getReceiver(), urlMessage.getMessageBody().getMessageBodyRaw());
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, TokenMessage tokenMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        UnitaMessage unitaMessage = new UnitaMessage(tokenMessage.getHeader().getHeaderMessageCode(), tokenMessage.getHeader().getSender(), tokenMessage.getHeader().getReceiver(), tokenMessage.getMessageBody().getMessageBodyRaw());
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void sendMessage(Context context, UnitaSettings unitaSettings, StatusMessage statusMessage, boolean shouldBeResend, int timesSend, int millisecondsBetweenMessages){
        UnitaMessage unitaMessage = new UnitaMessage(statusMessage.getHeader().getHeaderMessageCode(), statusMessage.getHeader().getSender(), statusMessage.getHeader().getReceiver(), statusMessage.getMessageBody().getMessageBodyRaw());
        this.createMessage(context, unitaSettings, unitaMessage, shouldBeResend, timesSend, millisecondsBetweenMessages);
    }

    public void stopSending(){
        if (soniTalkSender != null) {
            soniTalkSender.releaseSenderResources();
        }
        //releaseSender();
    }

    public void stopResending(){
        stopSending();
        resendMessage.removeCallbacks(resendRun);
    }

    public void releaseSender() {
        if(soniTalkSender != null) {
            soniTalkSender.releaseSenderResources();
        }
    }

    @Override
    public void onSoniTalkPermissionResult(int resultCode, Bundle resultData) {
        //TODO; Trigger UnitaPermissionResult
        unitaSdkListener.send(resultCode, resultData);
    }
}
