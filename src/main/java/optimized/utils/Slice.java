/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/
package optimized.utils;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Slice implements CharSequence {
    private static byte[] EmptyByteArray = new byte[0];
    private static ConcurrentMap<Slice, Slice> interned = new ConcurrentHashMap<>();
    public final int length;
    public final byte[] storage;
    private final int pos;

    public Slice(String s) {
        this(s.getBytes());
    }

    public Slice(byte[] s) {
        this(s, 0, s.length);
    }

    public Slice(byte[] s, int pos, int length) {
        if ((s != null) && (length > 0)) {
            this.storage = s;
            this.pos = pos;
            this.length = length;
        } else {
            this.storage = EmptyByteArray;
            this.pos = 0;
            this.length = 0;
        }
    }

    public Slice(Slice slice, int pos, int length) {
        this(slice.storage, slice.pos + pos, length);
    }

    public static boolean setUniqueEntry(Map<Slice, Slice> record, Slice key, Slice value) {
        if (record.get(key) == null) {
            record.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    public char charAt(int index) {
        return (char) storage[pos + index];
    }

    public int length() {
        return length;
    }

    public CharSequence subSequence(int start, int end) {
        return new Slice(this, start, end - start);
    }

    public String toString() {
        return new String(Arrays.copyOfRange(storage, pos, pos + length));
    }

    public int compareTo(Slice that) {
        if (this == that) {
            return 0;
        } else if ((this.storage == that.storage) &&
                (this.pos == that.pos) &&
                (this.length == that.length)) {
            return 0;
        } else {
            int minLen = Math.min(this.length, that.length);
            int j = that.pos;
            for (int i = this.pos; i < this.pos + minLen; ++i, ++j) {
                byte thisChar = this.storage[i];
                byte thatChar = that.storage[j];
                if (thisChar != thatChar) {
                    return thisChar - thatChar;
                }
            }
            return this.length - that.length;
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Slice)) {
            return false;
        }
        Slice that = (Slice) obj;
        if ((this.storage == that.storage) &&
                (this.pos == that.pos) &&
                (this.length == that.length)) {
            return true;
        }
        if (this.length != that.length) {
            return false;
        }
        int j = that.pos;
        for (int i = this.pos; i < this.pos + this.length; ++i, ++j) {
            if (this.storage[i] != that.storage[j]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result = 13;
        for (int i = pos; i < pos + length; ++i) {
            result = 73 * result + storage[i];
        }
        return result;
    }

    public Slice intern() {
        Slice prev = interned.putIfAbsent(this, this);
        if (prev == null) {
            return this;
        } else {
            return prev;
        }
    }

    public void write(PrintWriter out) {
        out.write(toString());
    }

    public void writeBuffer(StreamByteWriter outputStream) throws IOException {
        outputStream.write(storage, pos, length);
    }
}
