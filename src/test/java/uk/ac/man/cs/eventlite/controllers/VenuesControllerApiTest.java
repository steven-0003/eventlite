package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesControllerApi.class)
@Import({ Security.class, VenueModelAssembler.class, EventModelAssembler.class })
public class VenuesControllerApiTest {
	@Autowired
	private MockMvc mvc;
	
	@MockBean
	private VenueService venueService;
	
	@MockBean
	private EventService eventService;
	
	@Test
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());
		
		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
		.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(1)))
		.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")))
		.andExpect(jsonPath("$._links.profile.href", endsWith("/api/profile/venues")));
		
		verify(venueService).findAll();	
	}
	
	@Test
	public void getIndexWithVenues() throws Exception {
		Venue v = new Venue();
		v.setCapacity(1000);
		v.setName("Kilburn");
		v.setPostCode("M13 9PL");
		v.setRoadName("Oxford Road");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(v));
		
		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
		.andExpect(handler().methodName("getAllVenues")).andExpect(jsonPath("$.length()", equalTo(2)))
		.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")))
		.andExpect(jsonPath("$._links.profile.href", endsWith("/api/profile/venues")))
		.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));
	 
		verify(venueService).findAll();	
	}
	
	@Test
	public void getVenue() throws Exception{
		int id = 1;
		int capacity = 1000;
		String name = "Kilburn";
		
		Venue v = new Venue();
		v.setCapacity(capacity);
		v.setName(name);
		v.setPostCode("M13 9PL");
		v.setRoadName("Oxford Road");
		v.setId(id);
		
		when(venueService.findOne(id)).thenReturn(Optional.of(v));
		mvc.perform(get("/api/venues/{id}", id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getVenue"))
			.andExpect(jsonPath("$.length()", equalTo(5)))   // name, capacity, _links, latitude, longitude
			.andExpect(jsonPath("$.name", equalTo(name)))
			.andExpect(jsonPath("$.capacity", equalTo(capacity)))
			.andExpect(jsonPath("$._links.length()", equalTo(4)))    // self, venue, events, next3
			.andExpect(jsonPath("$._links.self.href", endsWith("/venues/" + id)))
			.andExpect(jsonPath("$._links.venue.href", endsWith("/venues/" + id)))
			.andExpect(jsonPath("$._links.events.href", endsWith("/venues/" + id + "/events")))
			.andExpect(jsonPath("$._links.next3events.href", endsWith("/venues/" + id + "/next3events")));
		
		verify(venueService).findOne(id);	
	}
	
	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
		.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
		.andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void getIndexWhenNoVenueEvents() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		when(eventService.findAllByVenueId(1)).thenReturn(Collections.<Event>emptyList());
		
		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
			.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/events")));
		
		verify(eventService).findAllByVenueId(1);	
	}
	
	@Test
	public void getIndexWithVenueEvents() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		Venue v = new Venue();
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(v);
		when(eventService.findAllByVenueId(1)).thenReturn(Collections.<Event>singletonList(e));
		
		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
			.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/events")))
			.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));
		
		verify(eventService).findAllByVenueId(1);	
	}
	
	@Test
	public void getVenueNotFoundEvents() throws Exception{
		mvc.perform(get("/api/venues/99/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
			.andExpect(handler().methodName("getEvents"));
	}
	
	@Test
	public void getIndexWhenNoVenueNext3Events() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		when(eventService.findFirst3ByVenueIdAndDateGreaterThan(1, LocalDate.now())).thenReturn(Collections.<Event>emptyList());
		
		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getNextThreeEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
			.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/next3events")));
		
		verify(eventService).findFirst3ByVenueIdAndDateGreaterThan(1, LocalDate.now());	
	}
	
	@Test
	public void getVenueNotFoundNext3Events() throws Exception {
		mvc.perform(get("/api/venues/99/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
			.andExpect(handler().methodName("getNextThreeEvents"));
	}
	
	@Test
	public void getIndexWithVenueNext3Events() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		Venue v = new Venue();
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.of(3000, 1, 1));
		e.setTime(LocalTime.now());
		e.setVenue(v);
		when(eventService.findFirst3ByVenueIdAndDateGreaterThan(1, LocalDate.now())).thenReturn(Collections.<Event>singletonList(e));
		
		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getNextThreeEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
			.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/next3events")))
			.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));
		
		verify(eventService).findFirst3ByVenueIdAndDateGreaterThan(1, LocalDate.now());	
	}
	
	@Test
	public void getNewVenue() throws Exception {
		mvc.perform(get("/api/venues/new").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
				.andExpect(handler().methodName("create"));
	}
	
	@Test
	public void getNewVenueNoAuth() throws Exception {
		mvc.perform(get("/api/venues/new")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getNewVenueBadAuth() throws Exception {
		mvc.perform(get("/api/venues/new").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getNewVenueBadRole() throws Exception {
		mvc.perform(get("/api/venues/new").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
	}
	
	@Test
	public void getUpdateVenue() throws Exception {
		mvc.perform(get("/api/venues/update").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
				.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getUpdateVenueNoAuth() throws Exception {
		mvc.perform(get("/api/venues/update")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getUpdateVenueBadAuth() throws Exception {
		mvc.perform(get("/api/venues/update").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getUpdateVenueBadRole() throws Exception {
		mvc.perform(get("/api/venues/update").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
	}
	
	@Test
	public void deleteVenue() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/api/venues/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
				.andExpect(content().string("")).andExpect(handler().methodName("deleteVenue"));

		verify(venueService).deleteById(1);
	}
	
	@Test
	public void deleteVenueNoAuth() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);

		mvc.perform(delete("/api/venues/1")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueBadAuth() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);

		mvc.perform(delete("/api/venues/1").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueBadRole() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);

		mvc.perform(delete("/api/venues/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueWithEvents() throws Exception {
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		when(venueService.hasEvents(id)).thenReturn(true);
		
		mvc.perform(delete("/api/venues/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error", containsString("venue 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteVenue"));
		
		verify(venueService, never()).deleteById(id);
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
		when(venueService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/venues/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void updateVenue() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("updateVenue"));
		
		verify(venueService).save(arg.capture());
		assertThat("Engineering Building A", equalTo(arg.getValue().getName()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
		assertThat("M13 9PL", equalTo(arg.getValue().getPostCode()));
		assertThat("Oxford Road", equalTo(arg.getValue().getRoadName()));
	}
	
	@Test
	public void updateVenueNotFound() throws Exception {
		mvc.perform(put("/api/venues/updateVenue/99").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":99,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueNoAuth() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadAuth() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(anonymous())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadRole() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueLongName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadCapacity() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":0,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyCapacity() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":null,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadPostCode() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"aaa\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyPostCode() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueLongRoadName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyRoadName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void createVenue() throws Exception{
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""));
		
		verify(venueService).save(arg.capture());
		assertThat("Engineering Building A", equalTo(arg.getValue().getName()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
		assertThat("M13 9PL", equalTo(arg.getValue().getPostCode()));
		assertThat("Oxford Road", equalTo(arg.getValue().getRoadName()));
		assertNotNull(arg.getValue().getLatitude());
		assertNotNull(arg.getValue().getLongitude());
	}
	
	@Test
	public void createVenueNoAuth() throws Exception {		
		mvc.perform(post("/api/venues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void createVenueBadAuth() throws Exception {		
		mvc.perform(post("/api/venues").with(anonymous())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void createVenueBadRole() throws Exception {		
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueLongName() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyName() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":null,\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueBadCapacity() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":0,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyCapacity() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":null,\"postCode\":\"M13 9PL\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueBadPostCode() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"aaaaa\",\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueBadEmptyPostCode() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":null,\"roadName\":\"Oxford Road\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueLongRoadName() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha\",\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyRoadName() throws Exception{
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Engineering Building A\",\"capacity\":100,\"postCode\":\"M13 9PL\",\"roadName\":null,\"events\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity()).andExpect(content().string(""))
				.andExpect(handler().methodName("newVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
}