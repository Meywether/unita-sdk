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
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CommandController {

    public static ArrayList<String[]> loadCommandsFromJson(String filename, Context context) throws IOException {
        InputStream is = context.getAssets().open("commands/"+filename);
        JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
        try {
            return readCommandsArray(reader);
        } finally {
            reader.close();
        }
    }

    private static ArrayList<String[]> readCommandsArray(JsonReader reader) throws IOException {
        ArrayList<String[]> commands = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            commands.add(readCommand(reader));
        }
        reader.endArray();
        return commands;
    }

    private static String[] readCommand(JsonReader reader) throws IOException{
        String[] command = new String[2];
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("commandcode")) {
                command[0] = reader.nextString();
            } else if (name.equals("function")) {
                command[1] = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return command;
    }
}
