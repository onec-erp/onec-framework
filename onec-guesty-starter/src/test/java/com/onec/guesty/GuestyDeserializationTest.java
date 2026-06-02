package com.onec.guesty;

import com.onec.guesty.model.Guest;
import com.onec.guesty.model.Listing;
import com.onec.guesty.model.Page;
import com.onec.guesty.model.Reservation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parses real-shaped Guesty payloads (captured from the live Open API) into the model records to lock
 * the {@code _id} mapping, nested objects, {@link java.time.Instant} and {@link BigDecimal} handling,
 * and the {@link Page} envelope's pagination math.
 */
class GuestyDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String LISTINGS_JSON = """
            {"results":[{
              "_id":"6986ec65dd286e001ca3f572",
              "title":"Can Joseph I","nickname":"CJ1",
              "propertyType":"House","roomType":"entire_home",
              "accommodates":4,"bedrooms":1.0,"bathrooms":1.0,"beds":2,
              "active":true,"isListed":true,"timezone":"Europe/Madrid",
              "accountId":"acc0000000000000000000a1",
              "address":{"full":"Camino de Can Parra 2242, 07870 Cap de Barbaria, Spain",
                "street":"Camino de Can Parra 2242","city":"Cap de Barbaria",
                "state":"Illes Balears","zipcode":"07870","country":"Spain",
                "lat":38.6770796,"lng":1.4261942},
              "prices":{"currency":"EUR","basePrice":250,"weekendBasePrice":250,
                "cleaningFee":null,"guestsIncludedInRegularFee":1},
              "createdAt":"2026-02-13T13:31:32.064Z","lastUpdatedAt":"2026-05-09T14:47:34.939Z"
            }],"title":"listings","fields":"...","count":3,"limit":1,"skip":0}
            """;

    private static final String RESERVATIONS_JSON = """
            {"results":[{
              "_id":"698f27c5deb6216166fd8f1c",
              "confirmationCode":"GY-zvj2uQ3E",
              "accountId":"acc0000000000000000000a1",
              "status":"confirmed","source":"manual",
              "listingId":"6986ec65dd286e001ca3f572","guestId":"698f27c436dbb23db89dfe16",
              "listing":{"_id":"6986ec65dd286e001ca3f572","title":"Can Joseph I"},
              "guest":{"_id":"698f27c436dbb23db89dfe16","fullName":"Olga Epova"},
              "money":{"currency":"EUR","fareAccommodation":2360,"fareCleaning":0,
                "subTotalPrice":2360,"hostPayout":2360,"netIncome":2360,
                "balanceDue":2360,"totalPaid":0},
              "nightsCount":14,
              "checkIn":"2026-06-06T14:00:00.000Z","checkOut":"2026-06-20T10:00:00.000Z",
              "createdAt":"2026-02-14T00:00:00.000Z"
            }],"title":"reservations","count":19,"limit":1,"skip":0}
            """;

    @Test
    void parsesListingsPage() throws Exception {
        Page<Listing> page = mapper.readValue(LISTINGS_JSON, new TypeReference<>() {});

        assertThat(page.count()).isEqualTo(3);
        assertThat(page.results()).hasSize(1);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextSkip()).isEqualTo(1);

        Listing listing = page.results().get(0);
        assertThat(listing.id()).isEqualTo("6986ec65dd286e001ca3f572");
        assertThat(listing.title()).isEqualTo("Can Joseph I");
        assertThat(listing.accommodates()).isEqualTo(4);
        assertThat(listing.address().city()).isEqualTo("Cap de Barbaria");
        assertThat(listing.address().country()).isEqualTo("Spain");
        assertThat(listing.prices().currency()).isEqualTo("EUR");
        assertThat(listing.prices().basePrice()).isEqualByComparingTo("250");
        assertThat(listing.prices().cleaningFee()).isNull();
        assertThat(listing.createdAt()).isNotNull();
    }

    @Test
    void parsesReservationsPage() throws Exception {
        Page<Reservation> page = mapper.readValue(RESERVATIONS_JSON, new TypeReference<>() {});

        assertThat(page.count()).isEqualTo(19);
        Reservation r = page.results().get(0);
        assertThat(r.id()).isEqualTo("698f27c5deb6216166fd8f1c");
        assertThat(r.confirmationCode()).isEqualTo("GY-zvj2uQ3E");
        assertThat(r.status()).isEqualTo("confirmed");
        assertThat(r.listingId()).isEqualTo("6986ec65dd286e001ca3f572");
        assertThat(r.listing().title()).isEqualTo("Can Joseph I");
        assertThat(r.guest().fullName()).isEqualTo("Olga Epova");
        assertThat(r.nightsCount()).isEqualTo(14);
        assertThat(r.money().hostPayout()).isEqualByComparingTo(new BigDecimal("2360"));
        assertThat(r.money().balanceDue()).isEqualByComparingTo(new BigDecimal("2360"));
        assertThat(r.checkIn()).isNotNull();
        assertThat(r.checkOut()).isAfter(r.checkIn());
    }

    /**
     * The {@code /guests-crud} endpoint names the page total {@code total}, not {@code count}. Without
     * the {@link Page} alias this deserialises to {@code count == 0}, so {@code hasMore()} is false and
     * {@link GuestyService} pagination stops after the first page — silently dropping every later guest.
     */
    private static final String GUESTS_JSON = """
            {"results":[
              {"_id":"698f27c436dbb23db89dfe16","fullName":"Olga Epova",
               "emails":["olga@example.com"],"phones":["+34600000000"]}
            ],"total":42,"limit":1,"skip":0}
            """;

    @Test
    void parsesGuestsPageWithTotalEnvelope() throws Exception {
        Page<Guest> page = mapper.readValue(GUESTS_JSON, new TypeReference<>() {});

        assertThat(page.count()).isEqualTo(42);   // mapped from "total"
        assertThat(page.hasMore()).isTrue();       // would be false if count fell back to 0
        assertThat(page.nextSkip()).isEqualTo(1);
        assertThat(page.results().get(0).fullName()).isEqualTo("Olga Epova");
    }

    @Test
    void lastPageHasNoMore() throws Exception {
        Page<Listing> page = mapper.readValue(
                LISTINGS_JSON.replace("\"count\":3", "\"count\":1"), new TypeReference<>() {});
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextSkip()).isEqualTo(-1);
    }
}
