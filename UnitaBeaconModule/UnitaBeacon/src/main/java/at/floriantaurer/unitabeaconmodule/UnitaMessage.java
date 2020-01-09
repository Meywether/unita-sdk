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

public class UnitaMessage {

    private UnitaHeader header;
    private UnitaMessageBody messageBody;


    public UnitaMessage(int headerMessageCode, Peer sender, Peer receiver, byte[] messageBodyRaw){
        this.header = new UnitaHeader(headerMessageCode, sender, receiver);
        this.messageBody = new UnitaMessageBody(messageBodyRaw);
    }

    public UnitaMessage(Peer sender, Peer receiver, byte[] messageBodyRaw){
        this.header = new UnitaHeader(0, sender, receiver);
        this.messageBody = new UnitaMessageBody(messageBodyRaw);
    }

    public UnitaHeader getHeader() {
        return header;
    }

    public UnitaMessageBody getMessageBody() {
        return messageBody;
    }
}
