package htsjdk.samtools.cram.structure;

import htsjdk.samtools.SAMRecord;

import java.util.Objects;

/**
 * A span of reads on a single reference.
 */
public class AlignmentSpan {
    /**
     * A constant to represent an unmapped span.
     */
    public static final AlignmentSpan UNMAPPED_SPAN = new AlignmentSpan(SAMRecord.NO_ALIGNMENT_START, 0);

    private int start;
    private int span;
    private int count;

    /**
     * Create a new span with a single read in it.
     *
     * @param start alignment start of the span
     * @param span  alignment span
     */
    public AlignmentSpan(final int start, final int span) {
        this.setStart(start);
        this.setSpan(span);
        this.count = 1;
    }

    /**
     * Create a new span with a multiple reads in it.
     *
     * @param start alignment start of the span
     * @param span  alignment span
     * @param count number of reads in the span
     */
    public AlignmentSpan(final int start, final int span, final int count) {
        this.setStart(start);
        this.setSpan(span);
        this.count = count;
    }

    /**
     * Add multiple reads to the span.
     *
     * @param start alignment start
     * @param span  alignment span
     * @param count number of reads to add
     */
    public void add(final int start, final int span, final int count) {
        if (this.getStart() > start) {
            this.setSpan(Math.max(this.getStart() + this.getSpan(), start + span) - start);
            this.setStart(start);
        } else if (this.getStart() < start) {
            this.setSpan(Math.max(this.getStart() + this.getSpan(), start + span) - this.getStart());
        } else {
            this.setSpan(Math.max(this.getSpan(), span));
        }

        this.count += count;
    }

    /**
     * Combine two AlignmentSpans.
     * @param a the first AlignmentSpan to combine
     * @param b the second AlignmentSpan to combine
     * @return the new combined AlignmentSpan
     */
    public static AlignmentSpan add(final AlignmentSpan a, final AlignmentSpan b) {
        final int start = Math.min(a.getStart(), b.getStart());

        int span;
        if (a.getStart() == b.getStart()) {
            span = Math.max(a.getSpan(), b.getSpan());
        } else {
            span = Math.max(a.getStart() + a.getSpan(), b.getStart() + b.getSpan()) - start;
        }

        return new AlignmentSpan(start, span, a.count + b.count);
    }

    /**
     * Add a single read to the span
     *
     * @param start alignment start
     * @param span  read span on the reference
     */
    public void addSingle(final int start, final int span) {
        add(start, span, 1);
    }

    public int getStart() {
        return start;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(final int span) {
        this.span = span;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) return false;

        final AlignmentSpan that = (AlignmentSpan)obj;

        return this.start == that.start &&
                this.span == that.span &&
                this.count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, span, count);
    }
}
