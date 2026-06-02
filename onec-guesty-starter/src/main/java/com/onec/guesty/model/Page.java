package com.onec.guesty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Guesty's list-response envelope: a slice of {@code results} plus the total {@code count} and the
 * {@code limit}/{@code skip} that produced it. Use {@link #hasMore()} to drive pagination.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Page<T>(List<T> results, int count, int limit, int skip) {

    public List<T> results() {
        return results == null ? List.of() : results;
    }

    /** Whether more results exist beyond this page. */
    public boolean hasMore() {
        return skip + results().size() < count;
    }

    /** The {@code skip} value for the next page, or -1 if this is the last page. */
    public int nextSkip() {
        return hasMore() ? skip + results().size() : -1;
    }
}
