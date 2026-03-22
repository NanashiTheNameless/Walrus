/*
 * Copyright 2018 Daniel Underhay & Matthew Daley.
 *
 * This file is part of Walrus.
 *
 * Walrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Walrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Walrus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bugfuzz.android.projectwalrus.device.proxmark3;

import android.util.Pair;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Proxmark3CommandTest {

    @Test
    public void dataAsStringUsesPayloadBytesForLegacyVersionReplies() {
        byte[] frame = new byte[544];
        ByteBuffer bb = ByteBuffer.wrap(frame);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putLong(Proxmark3Command.ACK);
        bb.putLong(0x12345678L);
        bb.putLong(0);
        bb.putLong(0);
        bb.put("Iceman".getBytes());

        Pair<Proxmark3Command, Integer> sliced = Proxmark3Command.slice(frame);

        assertNotNull(sliced);
        assertEquals(544, sliced.second.intValue());
        assertEquals("Iceman", sliced.first.dataAsString());
    }

    @Test
    public void sliceParsesMixRepliesIntoLegacyArgsAndPayload() {
        byte[] payload = new byte[]{0x41, 0x42, 0x43};
        byte[] frame = new byte[12 + 24 + payload.length];
        ByteBuffer bb = ByteBuffer.wrap(frame);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put(new byte[]{0x50, 0x4d, 0x33, 0x62});
        bb.putShort((short) (24 + payload.length));
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.putShort((short) Proxmark3Command.ACK);
        bb.putLong(1);
        bb.putLong(2);
        bb.putLong(3);
        bb.put(payload);
        bb.putShort((short) 0x3362);

        Pair<Proxmark3Command, Integer> sliced = Proxmark3Command.slice(frame);

        assertNotNull(sliced);
        assertEquals(frame.length, sliced.second.intValue());
        assertEquals(Proxmark3Command.ACK, sliced.first.op);
        assertEquals(1, sliced.first.args[0]);
        assertEquals(2, sliced.first.args[1]);
        assertEquals(3, sliced.first.args[2]);
        assertArrayEquals(payload, sliced.first.data);
        assertEquals("ABC", sliced.first.dataAsString());
    }

    @Test
    public void toBytesEncodesMixCommands() {
        Proxmark3Command command = new Proxmark3Command(Proxmark3Command.MIFARE_READBL,
                new long[]{7, 8, 9}, new byte[]{0x11, 0x22, 0x33});

        byte[] encoded = command.toBytes();
        ByteBuffer bb = ByteBuffer.wrap(encoded);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        byte[] magic = new byte[4];
        bb.get(magic);

        assertArrayEquals(new byte[]{0x50, 0x4d, 0x33, 0x61}, magic);
        assertEquals(27, bb.getShort() & 0xffff);
        assertEquals(Proxmark3Command.MIFARE_READBL, bb.getShort() & 0xffffL);
        assertEquals(7, bb.getLong());
        assertEquals(8, bb.getLong());
        assertEquals(9, bb.getLong());
        assertEquals(0x11, bb.get() & 0xff);
        assertEquals(0x22, bb.get() & 0xff);
        assertEquals(0x33, bb.get() & 0xff);
        assertEquals(0x3361, bb.getShort() & 0xffff);
    }
}
