package dev.namelessnanashi.walrus.device.proxmark3;

import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import dev.namelessnanashi.walrus.card.carddata.ISO14443ACardData;
import dev.namelessnanashi.walrus.card.carddata.MifareCardData;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Proxmark3DeviceTest {

    @Test
    public void hasSuccessfulIso14443ASelectSupportsMixAndNgReplies() throws IOException {
        byte[] payload = iso14443ASelectPayload(
                new byte[]{0x04, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66},
                (short) 0x0004,
                (byte) 0x08,
                new byte[]{0x75, 0x77});

        Proxmark3Command mixReply = mixReply(Proxmark3Command.ACK, 1, 0, 0, payload);
        Proxmark3Command ngReply = ngReply(Proxmark3Command.READER_ISO_14443A, 0, 0, payload);

        assertTrue(Proxmark3Device.hasSuccessfulIso14443ASelect(mixReply));
        assertTrue(Proxmark3Device.hasSuccessfulIso14443ASelect(ngReply));

        ISO14443ACardData parsed = Proxmark3Device.parseIso14443ACardData(ngReply);
        assertEquals(new BigInteger("04112233445566", 16), parsed.uid);
        assertEquals((short) 0x0004, parsed.atqa);
        assertEquals((byte) 0x08, parsed.sak);
        assertArrayEquals(new byte[]{0x75, 0x77}, parsed.ats);
    }

    @Test
    public void parseIso14443ACardDataRejectsShortPayloads() {
        Proxmark3Command reply = ngReply(Proxmark3Command.READER_ISO_14443A, 0, 0,
                new byte[14]);

        try {
            Proxmark3Device.parseIso14443ACardData(reply);
            fail("Expected IOException");
        } catch (IOException expected) {
            assertEquals("Incomplete ISO14443A card response", expected.getMessage());
        }
    }

    @Test
    public void hasSuccessfulMifareBlockReadSupportsMixAndNgReplies() throws IOException {
        byte[] blockData = new byte[MifareCardData.Block.SIZE];
        for (int i = 0; i < blockData.length; ++i) {
            blockData[i] = (byte) i;
        }

        Proxmark3Command mixReply = mixReply(Proxmark3Command.ACK, 1, 0, 0, blockData);
        Proxmark3Command ngReply = ngReply(Proxmark3Command.MIFARE_READBL, 0, 0, blockData);

        assertTrue(Proxmark3Device.hasSuccessfulMifareBlockRead(mixReply));
        assertTrue(Proxmark3Device.hasSuccessfulMifareBlockRead(ngReply));
        assertArrayEquals(blockData, Proxmark3Device.parseMifareBlockData(ngReply).data);
    }

    @Test
    public void parseMifareBlockDataRejectsShortPayloads() {
        Proxmark3Command reply = ngReply(Proxmark3Command.MIFARE_READBL, 0, 0, new byte[8]);

        try {
            Proxmark3Device.parseMifareBlockData(reply);
            fail("Expected IOException");
        } catch (IOException expected) {
            assertEquals("Incomplete MIFARE block response", expected.getMessage());
        }
    }

    private static Proxmark3Command mixReply(long op, long arg0, long arg1, long arg2,
            byte[] payload) {
        ByteBuffer bb = ByteBuffer.allocate(10 + 24 + payload.length + 2);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put(new byte[]{0x50, 0x4d, 0x33, 0x62});
        bb.putShort((short) (24 + payload.length));
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.putShort((short) op);
        bb.putLong(arg0);
        bb.putLong(arg1);
        bb.putLong(arg2);
        bb.put(payload);
        bb.putShort((short) 0x3362);

        Proxmark3Command.SliceResult sliced = Proxmark3Command.slice(bb.array());
        if (sliced == null) {
            throw new AssertionError("Failed to slice MIX reply");
        }

        return sliced.command;
    }

    private static Proxmark3Command ngReply(long op, int status, int reason, byte[] payload) {
        ByteBuffer bb = ByteBuffer.allocate(10 + payload.length + 2);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put(new byte[]{0x50, 0x4d, 0x33, 0x62});
        bb.putShort((short) (0x8000 | payload.length));
        bb.put((byte) status);
        bb.put((byte) reason);
        bb.putShort((short) op);
        bb.put(payload);
        bb.putShort((short) 0x3362);

        Proxmark3Command.SliceResult sliced = Proxmark3Command.slice(bb.array());
        if (sliced == null) {
            throw new AssertionError("Failed to slice NG reply");
        }

        return sliced.command;
    }

    private static byte[] iso14443ASelectPayload(byte[] uid, short atqa, byte sak, byte[] ats) {
        ByteBuffer bb = ByteBuffer.allocate(10 + 1 + 2 + 1 + 1 + ats.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put(uid);
        bb.position(10);
        bb.put((byte) uid.length);
        bb.putShort(atqa);
        bb.put(sak);
        bb.put((byte) ats.length);
        bb.put(ats);

        return bb.array();
    }
}
