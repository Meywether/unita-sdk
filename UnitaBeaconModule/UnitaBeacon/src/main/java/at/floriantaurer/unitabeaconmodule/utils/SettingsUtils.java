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

import android.content.Context;

import java.io.IOException;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.exceptions.ConfigException;
import at.ac.fhstp.sonitalk.utils.ConfigFactory;

public final class SettingsUtils {

    public static final String DEFAULT_PROFILE_FILENAME = "default_config.json";

    public static UnitaSettings getDefaultConfig(Context context) {
        return loadSettingsFromJson(DEFAULT_PROFILE_FILENAME, context);
    }

    public static String[] getConfigList(Context context){
        return ConfigFactory.getConfigList(context);
    }

    public static UnitaSettings loadSettingsFromJson(String filename, Context context){
        SoniTalkConfig config = null;
        String configName = null;
        try {
            config = ConfigFactory.loadFromJson(filename, context);
            configName = filename;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigException e) {
            e.printStackTrace();
        }
        if(config != null){
            return new UnitaSettings(config.getFrequencyZero(), config.getBitperiod(), config.getPauseperiod(),
                    config.getnMessageBlocks(), config.getnFrequencies(), config.getFrequencySpace(), configName);
        }
        return null;
    }
}
