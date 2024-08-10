package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.time.LocalDate;
import java.time.LocalTime;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventsControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@LocalServerPort
	private int port;

	private WebTestClient client;
	
	private int currentRows;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("events");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}

	@Test
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/events")).jsonPath("$._embedded.events.length()").value(equalTo(currentRows))
				.jsonPath("$.length()").value(equalTo(2));
	}

	@Test
	public void getEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("event 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getEvent() {
		client.get().uri("/events/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
			.value(endsWith("/api/events/1")).jsonPath("$._links.length()").value(equalTo(3))
			.jsonPath("$._links.venue.href").value(endsWith("/api/events/1/venue"))
			.jsonPath("$._links.event.href").value(endsWith("/api/events/1"))
			.jsonPath("$.name").value(equalTo("Gundeep's Birthday")).jsonPath("$.date").value(equalTo("3004-08-01"))
			.jsonPath("$.time").value(equalTo("00:00:00"));
	}
	
	@Test
	public void getNewEventNoUser() {
		client.get().uri("/events/new").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();
	}
	
	@Test
	public void getNewEventBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/events/new")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getNewEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/new")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void getUpdateEventNoUser() {
		client.get().uri("/events/update").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();
	}
	
	@Test
	public void getUpdateEventBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/events/update")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isForbidden();
	}
	
	
	@Test
	public void getUpdateEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/update")
		.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void updateEventNoUser() {
		// Attempt to PUT a valid event.
		client.put().uri("/events/updateEvent/1").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isUnauthorized();

		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("Party")));
	}
	
	@Test
	public void updateEventBadUser() {		
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().put().uri("/events/updateEvent/1")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isUnauthorized();
	
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("Party")));
	}
	
	@Test
	@DirtiesContext
	public void updateEventWithUser(){
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/events/updateEvent/1")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/events")).expectBody().isEmpty();
		
		assertThat(eventFromDb(1), containsString("Party"));
		assertThat(eventFromDb(1), not(containsString("Gundeep's Birthday")));
	}
	
	@Test
	public void updateEventNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/events/updateEvent/99")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"id\":99,\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isNotFound().expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("event 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void updateEventNoData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/events/updateEvent/1")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{}").exchange().expectStatus().isEqualTo(422).expectBody().isEmpty();
		
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
	}
	
	@Test
	public void updateEventBadData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().put().uri("/events/updateEvent/1")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{\"id\":1,\"date\":\"2004-01-01\",\"time\":\"\","
					+ "\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\","
					+ "\"description\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh\","
					+ "\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
			.exchange().expectStatus().isEqualTo(422).expectBody().isEmpty();
		
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("2004-01-01")));
	}
	
	@Test
	public void createEventNoUser() {
		// Attempt to POST a valid event.
		client.post().uri("/events").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isUnauthorized();

		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventBadUser() {		
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isUnauthorized();
	
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	@DirtiesContext
	public void createEventWithUser(){
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"date\":\"3004-01-01\",\"time\":\"20:46:12.1873878\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.exchange().expectStatus().isCreated().expectHeader()
				.value("Location", containsString("/api/events")).expectBody().isEmpty();
		
		assertThat(currentRows + 1, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventNoData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{}").exchange().expectStatus().isEqualTo(422).expectBody().isEmpty();
		
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventBadData() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
			.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
			.bodyValue("{\"id\":1,\"date\":\"2004-01-01\",\"time\":\"\","
					+ "\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\","
					+ "\"description\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh\","
					+ "\"venue\":{\"id\":1,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
			.exchange().expectStatus().isEqualTo(422).expectBody().isEmpty();
		
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	private String eventFromDb(long id) {
		return client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/"+String.valueOf(id)).accept(MediaType.APPLICATION_JSON)
			.exchange().expectBody(String.class).returnResult().toString();
	}
	
	@Test
	public void deleteEventNoUser() {

		client.delete().uri("/events/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventBadUser() {

		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/events/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void deleteEventWithUser() {

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/events/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent().expectBody().isEmpty();

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventNotFound() {

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/events/99")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound().expectBody()
				.jsonPath("$.error").value(containsString("event 99")).jsonPath("$.id").isEqualTo("99");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

}
