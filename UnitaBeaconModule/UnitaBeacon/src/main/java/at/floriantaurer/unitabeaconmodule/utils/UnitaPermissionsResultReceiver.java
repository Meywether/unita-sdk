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

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.ResultReceiver;

public class UnitaPermissionsResultReceiver extends ResultReceiver {

    private UnitaPermissionsResultReceiver.Receiver mReceiver;

    public UnitaPermissionsResultReceiver(Handler handler) {
        super(handler);
    }

    /**
     * Receiver interface for classes that should listen for callbacks from SoniTalk.
     * Result codes are currently defined in SoniTalkContext.
     */
    public interface Receiver {
        /**
         * Receiver interface for classes that should listen for callbacks from SoniTalk.
         * Result codes are currently defined in SoniTalkContext.
         * @param resultCode Possible results code are:
         *                   - ON_PERMISSION_LEVEL_DECLINED:
         *                    When the user chose to "Decline" when asked for the privacy level.
         *                   - ON_REQUEST_GRANTED:
         *                    When a request (send/receive data) was accepted by the user.
         *                   The Bundle will contain the requestCode of the request (if you need to
         *                   know which send/receive actions were really executed).
         *                   - ON_REQUEST_DENIED:
         *                    When a request (send/receive data) was rejected by the user.
         *                   The Bundle will contain the requestCode of the request (if you need to
         *                   know which send/receive actions were really executed). At least this
         *                   callback (and ON_REQUEST_L0_DENIED) or
         *                   ON_SHOULD_SHOW_RATIONALE_FOR_ALLOW_ALWAYS should be used to show some
         *                   rationale to the user.
         *                   - ON_SEND_JOB_FINISHED:
         *                    When the user should show a rationale
         *                   for the "Allow Always" (L0) SoniTalk permission. At least this callback
         *                   or ON_REQUEST_DENIED should be used to show some rationale to the user.
         *                   - ON_SHOULD_SHOW_RATIONALE_FOR_ALLOW_ALWAYS:
         *                    When the user should show a rationale for the "Allow Always" (L0)
         *                   SoniTalk permission. At least this callback or ON_REQUEST_DENIED should
         *                   be used to show some rationale to the user.
         *                   - ON_REQUEST_L0_DENIED:
         *                    When the user denied a permission request from the Android permission
         *                   system (L0). At least this callback (and ON_REQUEST_DENIED) or
         *                   ON_SHOULD_SHOW_RATIONALE_FOR_ALLOW_ALWAYS should be used to show some
         *                   rationale to the user.
         *
         * @param resultData Bundle containing additional information such as a requestCode
         *                   identifying the call
         */
        public void onUnitaPermissionResult(int resultCode, Bundle resultData);
    }

    public void setReceiver(UnitaPermissionsResultReceiver.Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onUnitaPermissionResult(resultCode, resultData);
        }
    }

    public static ResultReceiver receiverForSending(ResultReceiver actualReceiver) {
        Parcel parcel = Parcel.obtain();
        actualReceiver.writeToParcel(parcel,0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }
}
