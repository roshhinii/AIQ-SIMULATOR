package flender.dib.aiq.deviceemulationservice;

import com.flender.vda.Base;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class ByteUtils {

    private ByteUtils() {
    }

    public static int lengthOfValueType(Base.ValueType valueType) {
        return switch (valueType) {
            case UINT16, SINT16 -> 2;
            case UINT32, SINT32, FLOAT -> 4;
            case UINT64, DOUBLE, SINT64 -> 8;
            default -> throw new IllegalArgumentException("Unknown ValueType: " + valueType);
        };
    }

    public static long intValueOfBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getInt();
    }

    public static long longValueOfBytes(byte[] bytes) {
        if (bytes.length == 4) {
            return intValueOfBytes(bytes);
        }
        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getLong();
    }

    public static byte[] bytesOfLongValue(long value, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length).order(LITTLE_ENDIAN);
        if (length == 4) {
            buffer.putInt((int) value); // Store as 4-byte integer
        } else {
            buffer.putLong(value); // Store as 8-byte long
        }
        return buffer.array();
    }

    public static long longValueOfBytes(byte[] byteArray, int offset, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray, offset, length).order(LITTLE_ENDIAN);
        if (length == 4) {
            return buffer.getInt() & 0xFFFFFFFFL; // Convert unsigned int to long
        } else {
            return buffer.getLong();
        }
    }


    public static BigInteger unsignedlongValueOfBytes(byte[] bytes) {
        return toUnsignedBigInteger(ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getLong());
    }

    public static BigInteger toUnsignedBigInteger(long i) {
        if (i >= 0L) {
            return BigInteger.valueOf(i);
        }
        int upper = (int) (i >>> 32);
        int lower = (int) i;

        // return (upper << 32) + lower
        return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }

    public static int shortValueOfBytes(byte[] bytes) {
        return Short.toUnsignedInt(ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getShort());
    }

    public static double doubleValueOfBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getDouble();
    }

    public static Float floatValueOfBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(LITTLE_ENDIAN).getFloat();
    }
}
