package com.emc.object;

public class Range {
    private long first;
    private long last;

    public static Range fromLength(long offset, long length) {
        return new Range(offset, offset + length - 1);
    }

    public Range(long first, long last) {
        this.first = first;
        this.last = last;
    }

    public long getSize() {
        return last - first + 1;
    }

    public long getFirst() {
        return first;
    }

    public long getLast() {
        return last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (first != range.first) return false;
        if (last != range.last) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (first ^ (first >>> 32));
        result = 31 * result + (int) (last ^ (last >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "" + first + "-" + last;
    }
}
