/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;
import optimized.utils.StreamByteWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Fields {

    public static Field assoc(List<Field> l, Slice tag) {
        for (Field f : l) {
            if (f.tag.equals(tag)) {
                return f;
            }
        }
        return null;
    }

    public static class Field {
        public final Slice tag;

        public Field(Slice tag) {
            this.tag = tag;
        }

        public void format(StreamByteWriter out) throws IOException {
            out.writeByte((byte) '\t');
            tag.writeBuffer(out);
        }
    }

    public static class CharacterField extends Field {
        public char value;

        public CharacterField(Slice tag, char value) {
            super(tag);
            this.value = value;
        }

        private static byte[] iSeparator = ":A:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            super.format(out);
            out.write(iSeparator);
            out.writeByte((byte) value);
        }
    }

    public static class StringField extends Field {
        public Slice value;

        public StringField(Slice tag, Slice value) {
            super(tag);
            this.value = value;
        }

        private static byte[] iSeparator = ":Z:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            out.writeByte((byte) '\t');
            tag.writeBuffer(out);
            out.write(iSeparator);
            value.writeBuffer(out);
        }
    }

    public static class IntegerField extends Field {
        public int value;

        public IntegerField(Slice tag, int value) {
            super(tag);
            this.value = value;
        }

        private static byte[] iSeparator = ":i:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            out.writeByte((byte) '\t');
            tag.writeBuffer(out);
            out.write(iSeparator);
            out.printInt(value);
        }
    }

    public static class FloatField extends Field {
        public float value;

        public FloatField(Slice tag, float value) {
            super(tag);
            this.value = value;
        }

        private static byte[] iSeparator = ":f:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            super.format(out);
            out.write(iSeparator);
            out.write(Float.toString(value).getBytes());
        }
    }

    public static class ByteArrayField extends Field {
        public short[] value;

        public ByteArrayField(Slice tag, short[] value) {
            super(tag);
            this.value = value;
        }

        private static byte[] iSeparator = ":H:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            super.format(out);
            out.write(iSeparator);
            for (short val : value) {
                if (val < 16) {
                    out.writeByte((byte) '0');
                }
                out.write(Integer.toHexString(val).getBytes());
            }
        }
    }

    public static class NumericArrayField<T> extends Field {
        public List<T> value;
        char ntype;

        public NumericArrayField(Slice tag, char ntype, List<T> value) {
            super(tag);
            this.ntype = ntype;
            this.value = value;
        }

        private static byte[] iSeparator = ":B:".getBytes();

        @Override
        public void format(StreamByteWriter out) throws IOException {
            super.format(out);
            out.write(iSeparator);

            out.writeByte((byte) ntype);
            for (T val : value) {
                out.writeByte((byte) ',');
                out.write(val.toString().getBytes());
            }
        }
    }
}
