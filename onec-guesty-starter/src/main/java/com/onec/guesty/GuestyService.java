package com.onec.guesty;

import com.onec.guesty.model.Listing;
import com.onec.guesty.model.Page;
import com.onec.guesty.model.Reservation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Application-facing facade over {@link GuestyClient} that adds the conveniences most callers want:
 * transparent pagination across an entire collection, and a few common reservation queries. The host
 * application maps the returned {@link Listing}/{@link Reservation} records into its own domain.
 */
public class GuestyService {

    /** Guesty's documented maximum page size for list endpoints. */
    public static final int MAX_PAGE_SIZE = 100;

    private final GuestyClient client;

    public GuestyService(GuestyClient client) {
        this.client = client;
    }

    public GuestyClient client() {
        return client;
    }

    /** Every listing in the account, paginating until exhausted. */
    public List<Listing> allListings() {
        return drain(skip -> client.listListings(pageQuery(Map.of(), skip)));
    }

    /** Every reservation matching {@code filters} (raw Guesty query params), paginating until exhausted. */
    public List<Reservation> allReservations(Map<String, ?> filters) {
        return drain(skip -> client.listReservations(pageQuery(filters, skip)));
    }

    /** All reservations for a single listing. */
    public List<Reservation> reservationsForListing(String listingId) {
        return allReservations(Map.of("listingId", listingId));
    }

    /**
     * Reservations whose check-in falls within {@code [from, to]} (inclusive), as a Guesty filter
     * expression. Dates are ISO {@code yyyy-MM-dd}. Useful for an arrivals report.
     */
    public List<Reservation> arrivalsBetween(String fromDate, String toDate) {
        String filters = "[{\"field\":\"checkInDateLocalized\",\"operator\":\"$between\","
                + "\"from\":\"" + fromDate + "\",\"to\":\"" + toDate + "\"}]";
        return allReservations(Map.of("filters", filters, "sort", "checkIn"));
    }

    private Map<String, Object> pageQuery(Map<String, ?> base, int skip) {
        Map<String, Object> query = new LinkedHashMap<>(base);
        query.put("limit", MAX_PAGE_SIZE);
        query.put("skip", skip);
        return query;
    }

    /** Walk a paginated endpoint from skip=0 until {@link Page#hasMore()} is false. */
    private <T> List<T> drain(Function<Integer, Page<T>> fetch) {
        List<T> all = new ArrayList<>();
        int skip = 0;
        while (true) {
            Page<T> page = fetch.apply(skip);
            all.addAll(page.results());
            if (!page.hasMore() || page.results().isEmpty()) {
                return all;
            }
            skip = page.nextSkip();
        }
    }
}
