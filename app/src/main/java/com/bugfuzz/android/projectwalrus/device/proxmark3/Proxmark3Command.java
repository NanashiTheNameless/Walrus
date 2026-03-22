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

import android.support.annotation.LongDef;
import android.support.annotation.Size;
import android.util.Pair;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

class Proxmark3Command {

    private static final int LONG_BYTE_LENGTH = 8;
    private static final int SHORT_BYTE_LENGTH = 2;
    private static final int MAGIC_BYTE_LENGTH = 4;
    private static final int OLD_DATA_MAX_LENGTH = 512;
    private static final int LEGACY_ARGS_BYTE_LENGTH = 3 * LONG_BYTE_LENGTH;
    private static final int OLD_FRAME_LENGTH =
            LONG_BYTE_LENGTH + LEGACY_ARGS_BYTE_LENGTH + OLD_DATA_MAX_LENGTH;
    private static final int MIX_DATA_MAX_LENGTH = OLD_DATA_MAX_LENGTH - LEGACY_ARGS_BYTE_LENGTH;
    private static final int RESPONSE_HEADER_LENGTH = MAGIC_BYTE_LENGTH + SHORT_BYTE_LENGTH + 1 + 1
            + SHORT_BYTE_LENGTH;
    private static final int RESPONSE_MIN_LENGTH = RESPONSE_HEADER_LENGTH + SHORT_BYTE_LENGTH;
    private static final int COMMAND_HEADER_LENGTH = MAGIC_BYTE_LENGTH + SHORT_BYTE_LENGTH
            + SHORT_BYTE_LENGTH;
    private static final int COMMAND_CRC_PLACEHOLDER = 0x3361;
    private static final int NG_LENGTH_FLAG = 1 << 15;
    private static final byte[] COMMAND_MAGIC = new byte[]{0x50, 0x4d, 0x33, 0x61};
    private static final byte[] RESPONSE_MAGIC = new byte[]{0x50, 0x4d, 0x33, 0x62};

    static final long ACK = 0xff;
    static final long DEBUG_PRINT_STRING = 0x100;
    static final long VERSION = 0x107;
    static final long HID_DEMOD_FSK = 0x20b;
    static final long HID_CLONE_TAG = 0x210;
    static final long READER_ISO_14443A = 0x385;
    static final long MEASURE_ANTENNA_TUNING = 0x400;
    static final long MEASURED_ANTENNA_TUNING = 0x410;
    static final long MIFARE_READBL = 0x620;

    static final long MEASURE_ANTENNA_TUNING_FLAG_TUNE_LF = 1;
    static final long MEASURE_ANTENNA_TUNING_FLAG_TUNE_HF = 2;

    static final long ISO14A_CONNECT = 1 << 0;

    @Opcode
    final long op;
    final long[] args;
    final byte[] data;
    private final int dataLength;
    private final boolean legacyArgs;
    private final int status;
    private final int reason;

    Proxmark3Command(@Opcode long op, @Size(3) long[] args,
            @Size(max = OLD_DATA_MAX_LENGTH) byte[] data) {
        this(op, args, data, data.length, true, 0, 0);
    }

    private Proxmark3Command(@Opcode long op, @Size(3) long[] args,
            @Size(max = OLD_DATA_MAX_LENGTH) byte[] data, int dataLength, boolean legacyArgs,
            int status, int reason) {
        this.op = op;

        if (args.length != 3) {
            throw new IllegalArgumentException("Invalid number of args");
        }
        this.args = args;

        if (dataLength < 0 || dataLength > data.length) {
            throw new IllegalArgumentException("Invalid data length");
        }
        if (data.length > OLD_DATA_MAX_LENGTH) {
            throw new IllegalArgumentException("Data too long");
        }
        this.data = Arrays.copyOf(data, dataLength);
        this.dataLength = dataLength;
        this.legacyArgs = legacyArgs;
        this.status = status;
        this.reason = reason;
    }

    Proxmark3Command(@Opcode long op, @Size(max = OLD_DATA_MAX_LENGTH) long[] args) {
        this(op, args, new byte[0]);
    }

    Proxmark3Command(@Opcode @SuppressWarnings("SameParameterValue") long op) {
        this(op, new long[3]);
    }

    static Pair<Proxmark3Command, Integer> slice(byte[] bytes) {
        Pair<Proxmark3Command, Integer> response = sliceResponse(bytes);
        if (response != null) {
            return response;
        }

        if (bytes.length < OLD_FRAME_LENGTH) {
            return null;
        }

        return new Pair<>(fromOldBytes(bytes), OLD_FRAME_LENGTH);
    }

    private static Pair<Proxmark3Command, Integer> sliceResponse(byte[] bytes) {
        if (!startsWith(bytes, RESPONSE_MAGIC)) {
            return null;
        }
        if (bytes.length < RESPONSE_MIN_LENGTH) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.position(MAGIC_BYTE_LENGTH);
        int lengthAndFlags = bb.getShort() & 0xffff;
        boolean ng = (lengthAndFlags & NG_LENGTH_FLAG) != 0;
        int payloadLength = lengthAndFlags & ~NG_LENGTH_FLAG;
        int frameLength = RESPONSE_HEADER_LENGTH + payloadLength + SHORT_BYTE_LENGTH;
        if (bytes.length < frameLength) {
            return null;
        }

        int status = bb.get();
        int reason = bb.get();
        long op = bb.getShort() & 0xffffL;

        byte[] payload = new byte[payloadLength];
        bb.get(payload);

        if (!ng && payloadLength >= LEGACY_ARGS_BYTE_LENGTH) {
            ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
            payloadBuffer.order(ByteOrder.LITTLE_ENDIAN);

            long[] args = new long[3];
            for (int i = 0; i < args.length; ++i) {
                args[i] = payloadBuffer.getLong();
            }

            byte[] data = ArrayUtils.subarray(payload, LEGACY_ARGS_BYTE_LENGTH, payload.length);

            return new Pair<>(new Proxmark3Command(op, args, data, data.length, true,
                    status, reason), frameLength);
        }

        return new Pair<>(new Proxmark3Command(op, new long[3], payload, payload.length, false,
                status, reason), frameLength);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; ++i) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private static Proxmark3Command fromOldBytes(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        long op = bb.getLong();

        long[] args = new long[3];
        for (int i = 0; i < 3; ++i) {
            args[i] = bb.getLong();
        }

        byte[] data = new byte[OLD_DATA_MAX_LENGTH];
        bb.get(data);

        return new Proxmark3Command(op, args, data, data.length, true, 0, 0);
    }

    byte[] toBytes() {
        if ((op & 0xffffL) != op) {
            throw new IllegalArgumentException("MIX frames only support 16-bit commands");
        }
        if (data.length > MIX_DATA_MAX_LENGTH) {
            throw new IllegalArgumentException("Data too long for MIX frame");
        }

        ByteBuffer bb = ByteBuffer.allocate(COMMAND_HEADER_LENGTH + LEGACY_ARGS_BYTE_LENGTH
                + data.length + SHORT_BYTE_LENGTH);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.put(COMMAND_MAGIC);
        bb.putShort((short) (LEGACY_ARGS_BYTE_LENGTH + data.length));
        bb.putShort((short) op);

        for (long arg : args) {
            bb.putLong(arg);
        }

        bb.put(data);
        bb.putShort((short) COMMAND_CRC_PLACEHOLDER);

        return bb.array();
    }

    @Override
    public String toString() {
        return "<Proxmark3Command " + op + ", args " + Arrays.toString(args) + ", status "
                + status + ", reason " + reason + ", data " + Arrays.toString(data) + ">";
    }

    public String dataAsString() {
        int length = Math.min(dataLength, data.length);
        for (int i = 0; i < length; ++i) {
            if (data[i] == 0) {
                length = i;
                break;
            }
        }

        return new String(ArrayUtils.subarray(data, 0, length));
    }

    boolean usesLegacyArgs() {
        return legacyArgs;
    }

    boolean isSuccessful() {
        return status == 0;
    }

    int getReason() {
        return reason;
    }

    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    @Retention(RetentionPolicy.SOURCE)
    @LongDef({
            ACK,
            DEBUG_PRINT_STRING,
            VERSION,
            HID_DEMOD_FSK,
            HID_CLONE_TAG,
            READER_ISO_14443A,
            MEASURE_ANTENNA_TUNING,
            MEASURED_ANTENNA_TUNING,
            MIFARE_READBL
    })
    public @interface Opcode {
    }
}
