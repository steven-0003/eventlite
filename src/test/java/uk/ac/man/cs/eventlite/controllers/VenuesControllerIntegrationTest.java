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
public class VenuesControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	private static Pattern CSRF = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*");
	private static String CSRF_HEADER = "X-CSRF-TOKEN";
	private static String SESSION_KEY = "JSESSIONID";
	
	@LocalServerPort
	private int port;
	
	private int currentRows;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		currentRows = countRowsInTable("venues");
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}
	
	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk();
	}
	
	@Test
	public void getVenue() {
		client.get().uri("/venues/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk()
		.expectBody(String.class).consumeWith(result -> {
			assertThat(result.getResponseBody(), containsString("Kilburn"));
		});
	}
	
	@Test
	public void getVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
		.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
			assertThat(result.getResponseBody(), containsString("99"));
		});
	}
	
	@Test
	public void deleteVenueNoUser() {

		// Should redirect to the sign-in page.
		client.delete().uri("/venues/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", containsString("/sign-in"));

		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	@DirtiesContext
	public void deleteVenueWithUser() {

		String[] tokens = login();

		// Attempt delete when venue does not contain events.
		client.delete().uri("/venues/2").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/venues"));
		
		// Check that 1 row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueBadUser() {

		String[] tokens = loginBadUser();

		// Attempt delete when venue does not contain events.
		client.delete().uri("/venues/2").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isForbidden();
		
		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueWithUserContainsEvents() {
		
		String[] tokens = login();

		// Attempt delete when venue contains events.
		client.delete().uri("/venues/1").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/venues"));
		
		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueNotFound() {
		
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/venues/99").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});;

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void getUpdateVenueNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/venues/update/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}
	
	@Test
	public void getUpdateVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/update/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateVenueBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/venues/update/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void getUpdateVenueNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/update/99")
			.accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectBody(String.class)
			.consumeWith(result -> {
				assertThat(result.getResponseBody(), containsString("99"));
			});
	}
	
	@Test
	public void getNewVenueNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/venues/new").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}
	
	@Test
	public void getNewVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/new")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getNewVenueBadUser() {
		client.mutate().filter(basicAuthentication("Naddy", "Gundeep's Birthday")).build().get().uri("/venues/new")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isForbidden();
	}
	
	@Test
	public void updateVenueNoUser() {
		String[] tokens = login();

		// Attempt to PUT a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", containsString("/sign-in"));
		
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("Engineering Building A")));
	}
	
	@Test
	@DirtiesContext
	public void updateVenueWithUser() {
		String[] tokens = login();

		// Attempt to PUT a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/venues"));
		
		assertThat(venueFromDb(1), containsString("Engineering Building A"));
		assertThat(venueFromDb(1), not(containsString("Kilburn")));
	}
	
	@Test
	public void updateVenueBadUser() {
		String[] tokens = loginBadUser();

		// Attempt to PUT a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isForbidden();
		
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("Engineering Building A")));
	}
	
	@Test
	public void updateVenueNotFound() {
		String[] tokens = login();

		// Attempt to PUT a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/venues/updateVenue/99").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isNotFound().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void updateVenueNoData() {
		String[] tokens = login();

		// Attempt to PUT an empty venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(venueFromDb(1), containsString("Kilburn"));
	}
	
	@Test
	public void updateVenueBadData() {
		String[] tokens = login();

		// Attempt to PUT an invalid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj");
		form.add("capacity", "0");
		form.add("postCode", "aaa");
		form.add("roadName", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha");
		
		// The session ID cookie holds our login credentials.
		client.put().uri("/venues/updateVenue/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(venueFromDb(1), containsString("Kilburn"));
		assertThat(venueFromDb(1), not(containsString("aaa")));
	}
	
	@Test
	public void createVenueNoUser() {
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", containsString("/sign-in"));
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	@DirtiesContext
	public void createVenueWithUser() {
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/venues"));
		
		assertThat(currentRows + 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueBadUser() {
		String[] tokens = loginBadUser();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Engineering Building A");
		form.add("capacity", "100");
		form.add("postCode", "M13 9PL");
		form.add("roadName", "Oxford Road");
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isForbidden();
		
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueNoData() {
		String[] tokens = login();

		// Attempt to POST an empty venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void createVenueBadData() {
		String[] tokens = login();

		// Attempt to POST an invalid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj");
		form.add("capacity", "0");
		form.add("postCode", "aaa");
		form.add("roadName", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha");
		
		// The session ID cookie holds our login credentials.
		client.post().uri("/venues").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
				
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
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
	
	private String venueFromDb(long id) {
		return client.get().uri("/venues/"+String.valueOf(id)).accept(MediaType.TEXT_HTML)
				.exchange().expectBody(String.class).returnResult().toString();
	}
}