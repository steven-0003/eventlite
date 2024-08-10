package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VenuesControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	@LocalServerPort
	private int port;

	private WebTestClient client;
	
	private int currentRows;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("venues");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}
	
	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
		.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
		.value(endsWith("/api/venues")).jsonPath("$._embedded.venues.length()").value(equalTo(currentRows))
		.jsonPath("$._links.profile.href").value(endsWith("/api/profile/venues"));
	}
	
	
	@Test
	public void getVenue() {
		client.get().uri("/venues/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
		.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
		.value(endsWith("/api/venues/1")).jsonPath("$.length()").value(equalTo(5))
		.jsonPath("$.name").value(equalTo("Kilburn")).jsonPath("$.capacity").value(equalTo(3000))
		.jsonPath("$._links.length()").value(equalTo(4)).jsonPath("$._links.venue.href").value(endsWith("/api/venues/1"))
		.jsonPath("$._links.events.href").value(endsWith("/api/venues/1/events"))
		.jsonPath("$._links.next3events.href").value(endsWith("/api/venues/1/next3events"));
	}
	
	@Test
	public void getVenueEvents() {
		client.get().uri("/venues/1/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
		.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
		.value(endsWith("/api/venues/1/events")).jsonPath("$._embedded.events.length()").value(equalTo(1));
	}
	
	@Test
	public void getVenueNotFoundEvents() {
		client.get().uri("/venues/99/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
		.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
		.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueNext3Events() {
		client.get().uri("/venues/1/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
		.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
		.value(endsWith("/api/venues/1/next3events")).jsonPath("$._embedded.events.length()").value(equalTo(1));
	}
	
	@Test
	public void getVenueNotFoundNext3Events() {
		client.get().uri("/venues/99/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
		.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
		.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
		.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
		.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getNewVenueNoUser() {
		client.get().uri("/venues/new").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();
	}
	
	@Test
	public void getNewVenueBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/venues/new")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getNewVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/new")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void getUpdateVenueNoUser() {
		client.get().uri("/venues/update").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();
	}
	
	@Test
	public void getUpdateVenueBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/venues/update")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getUpdateVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/update")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void updateVenueNoUser() {		
		// Attempt to PUT a valid venue.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isUnauthorized();
		
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("Engineering Building A")));
	}
	
	@Test
	public void updateVenueBadUser() {
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().put().uri("/venues/updateVenue/1")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isUnauthorized();
		
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("Engineering Building A")));
	}
	
	@Test
	@DirtiesContext
	public void updateVenueWithUser() {		
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/venues/updateVenue/1")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/venues")).expectBody().isEmpty();
		
		assertThat(venueFromDb(1), containsString("Engineering Building A"));
		assertThat(venueFromDb(1), not(containsString("Kilburn")));
	}
	
	@Test
	public void updateVenueNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/venues/updateVenue/99")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":99,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isNotFound().expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void updateVenueNoData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/venues/updateVenue/1")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{}")
			.exchange().expectStatus().isEqualTo(422);
	
		assertThat(venueFromDb(1), containsString("Kilburn"));
	}
	
	@Test
	public void updateVenueBadData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/venues/updateVenue/1")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{\"id\":1,"
					+ "\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\","
					+ "\"capacity\":0,\"postCode\":\"aaa\","
					+ "\"roadName\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha\",\"events\":null}")
			.exchange().expectStatus().isEqualTo(422);
		
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("aaa")));
	}
	
	private String venueFromDb(long id) {
		return client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/"+String.valueOf(id)).accept(MediaType.APPLICATION_JSON)
				.exchange().expectBody(String.class).returnResult().toString();
	}
	
	@Test
	public void createVenueNoUser() {		
		// Attempt to POST a valid venue.
		client.post().uri("/venues").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isUnauthorized();
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueBadUser() {
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isUnauthorized();
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	@DirtiesContext
	public void createVenueWithUser() {		
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/venues")).expectBody().isEmpty();
		
		assertThat(currentRows + 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueNoData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{}")
			.exchange().expectStatus().isEqualTo(422);
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueBadData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{\"id\":1,"
					+ "\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\","
					+ "\"capacity\":0,\"postCode\":\"aaa\","
					+ "\"roadName\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha\",\"events\":null}")
			.exchange().expectStatus().isEqualTo(422);
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueNoUser() {
		client.delete().uri("/venues/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueBadUser() {
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/venues/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	@DirtiesContext
	public void deleteVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/2")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent().expectBody().isEmpty();
		
		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueWithUserContainsEvents() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(409).expectBody()
				.jsonPath("$.error").value(containsString("venue 1")).jsonPath("$.id").isEqualTo("1");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/99")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound().expectBody()
				.jsonPath("$.error").value(containsString("venue 99")).jsonPath("$.id").isEqualTo("99");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
}
