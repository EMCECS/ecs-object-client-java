package com.emc.object;

public class Range {
    private Long first;
    private Long last;

    public static Range fromOffsetLength(Long offset, Long length) {
        return new Range(offset, offset + length - 1);
    }

    public Range(Long first, Long last) {
        this.first = first;
        this.last = last;
    }

    public Long getFirst() {
        return first;
    }

    public Long getLast() {
        return last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (first != null ? !first.equals(range.first) : range.first != null) return false;
        if (last != null ? !last.equals(range.last) : range.last != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (last != null ? last.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "" + first + "-" + (last == null ? "" : last);
    }
}
