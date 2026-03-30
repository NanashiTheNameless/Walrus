package dev.namelessnanashi.walrus.device.proxmark3;

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

        Proxmark3Command.SliceResult sliced = Proxmark3Command.slice(frame);

        assertNotNull(sliced);
        assertEquals(544, sliced.consumedBytes);
        assertEquals("Iceman", sliced.command.dataAsString());
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

        Proxmark3Command.SliceResult sliced = Proxmark3Command.slice(frame);

        assertNotNull(sliced);
        assertEquals(frame.length, sliced.consumedBytes);
        assertEquals(Proxmark3Command.ACK, sliced.command.op);
        assertEquals(1, sliced.command.args[0]);
        assertEquals(2, sliced.command.args[1]);
        assertEquals(3, sliced.command.args[2]);
        assertArrayEquals(payload, sliced.command.data);
        assertEquals("ABC", sliced.command.dataAsString());
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
