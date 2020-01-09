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

import at.ac.fhstp.sonitalk.SoniTalkConfig;

public class UnitaSettings extends SoniTalkConfig {

    private String settingName;

    public UnitaSettings(int frequencyZero, int bitperiod, int pauseperiod, int nMessageBlocks, int nFrequencies, int frequencySpace, String settingName) {
        super(frequencyZero, bitperiod, pauseperiod, nMessageBlocks, nFrequencies, frequencySpace);
        this.settingName = settingName;
    }

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }
}
