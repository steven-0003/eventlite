package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.ac.man.cs.eventlite.EventLite;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventsControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	private static Pattern CSRF = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*");
	private static String CSRF_HEADER = "X-CSRF-TOKEN";
	private static String SESSION_KEY = "JSESSIONID";
	
	@LocalServerPort
	private int port;
	
	private int currentRows;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("events");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk();
	}

	@Test
	public void getEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void getEvent() {
		client.get().uri("/events/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
		.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
			assertThat(result.getResponseBody(), containsString("Gundeep"));
		});
	}
	
	@Test
	public void getUpdateEventNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/events/update/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}
	
	@Test
	public void getUpdateEventBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/events/update/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getUpdateEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/update/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateEventNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/update/99")
		.accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
				assertThat(result.getResponseBody(), containsString("99"));
			});
	}
	
	@Test
	public void getNewEventNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/events/new").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}
	
	@Test
	public void getNewEventBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/events/new")
			.accept(MediaType.TEXT_HTML).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getNewEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/new")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void mastodonPostNoUser() {
		String[] tokens = login();
		
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("content", "New post");
		
		client.post().uri("/events/1/mastodonPost").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
			.value("Location", containsString("/sign-in"));
	}
	
	@Test
	@DirtiesContext
	public void mastodonPostWithUser() {
		String[] tokens = login();
		
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("content", "New post");
		
		client.post().uri("/events/1/mastodonPost").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(form).cookies(cookies -> {
				cookies.add(SESSION_KEY, tokens[1]);
			}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/events/1"));
	}
	
	
	@Test
	public void updateEventNoUser() {
		String[] tokens = login();

		// Attempt to PUT a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.put().uri("/events/updateEvent/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
			.value("Location", containsString("/sign-in"));


		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("Party")));
	}
	
	@Test
	@DirtiesContext
	public void updateEventWithUser() {
		String[] tokens = login();

		// Attempt to PUT a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/events/updateEvent/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/events"));
		
		assertThat(eventFromDb(1), containsString("Party"));
		assertThat(eventFromDb(1), not(containsString("Gundeep's Birthday")));
	}
	
	@Test
	public void updateEventBadUser() {
		String[] tokens = loginBadUser();

		// Attempt to PUT a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/events/updateEvent/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isForbidden();
		
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("Party")));
	}
	
	@Test
	public void updateEventNotFound() {
		String[] tokens = login();

		// Attempt to PUT a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/events/updateEvent/99").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isNotFound()
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
		
		assertThat(eventFromDb(99), not(containsString("Party")));
	}
	
	@Test
	public void updateEventNoData() {
		String[] tokens = login();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		// The session ID cookie holds our login credentials.
		client.put().uri("/events/updateEvent/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("Party")));
	}
	
	@Test
	public void updateEventBadData() {
		String[] tokens = login();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj");
		form.add("venue.id", "0");
		form.add("date", "2004-08-01");
		form.add("time", "25:00");
		form.add("description", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh");
		// The session ID cookie holds our login credentials.
		client.put().uri("/events/updateEvent/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(eventFromDb(1), containsString("Gundeep's Birthday"));
		assertThat(eventFromDb(1), not(containsString("25:00")));
	}
	
	@Test
	public void createEventNoUser() {
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.post().uri("/events").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", containsString("/sign-in"));

		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	@DirtiesContext
	public void createEventWithUser() {
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/events").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/events"));
	
		assertThat(currentRows + 1, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventBadUser() {
		String[] tokens = loginBadUser();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Party");
		form.add("venue.id", "1");
		form.add("date", "3004-08-01");
		form.add("time", "00:00");
		form.add("description", "This is a fun party");
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/events").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isForbidden();
	
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventNoData() {
		String[] tokens = login();

		// Attempt to POST an empty event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/events").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
			
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void createEventBadData() {
		String[] tokens = login();

		// Attempt to POST an invalid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("_csrf", tokens[0]);
		form.add("name", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj");
		form.add("venue.id", "0");
		form.add("date", "2004-08-01");
		form.add("time", "25:00");
		form.add("description", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh");
		// The session ID cookie holds our login credentials.
		client.post().uri("/events").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
			
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void deleteEventNoUser() {

		// Should redirect to the sign-in page.
		client.delete().uri("/events/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));

		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void deleteEventWithUser() {
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/events/1").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/events"));

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void deleteEventBadUser() {
		
		String[] tokens = loginBadUser();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/events/1").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isForbidden();

		// Check that one row is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventNotFound() {
		
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/events/99").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isNotFound()
				.expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	private String[] login() {
		String[] tokens = new String[2];

		// Although this doesn't POST the log in form it effectively logs us in.
		// If we provide the correct credentials here, we get a session ID back which
		// keeps us logged in.
		EntityExchangeResult<String> result = client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get()
				.uri("/sign-in").accept(MediaType.TEXT_HTML).exchange().expectBody(String.class).returnResult();
		tokens[0] = getCsrfToken(result.getResponseBody());
		tokens[1] = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		return tokens;
	}
	
	private String[] loginBadUser() {
		String[] tokens = new String[2];

		// Although this doesn't POST the log in form it effectively logs us in.
		// If we provide the correct credentials here, we get a session ID back which
		// keeps us logged in.
		EntityExchangeResult<String> result = client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get()
				.uri("/sign-in").accept(MediaType.TEXT_HTML).exchange().expectBody(String.class).returnResult();
		tokens[0] = getCsrfToken(result.getResponseBody());
		tokens[1] = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		return tokens;
	}
	
	private String getCsrfToken(String body) {
		Matcher matcher = CSRF.matcher(body);

		assertThat(matcher.matches(), equalTo(true));

		return matcher.group(1);
	}
	
	private String eventFromDb(long id) {
		return client.get().uri("/events/"+String.valueOf(id)).accept(MediaType.TEXT_HTML)
			.exchange().expectBody(String.class).returnResult().toString();
	}

}
