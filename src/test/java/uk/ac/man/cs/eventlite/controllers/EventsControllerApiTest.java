package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsControllerApi.class)
@Import({ Security.class, EventModelAssembler.class })
public class EventsControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private EventService eventService;
	
	@MockBean
	private VenueService venueService;

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")));

		verify(eventService).findAll();
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Venue v = new Venue();
		Event e = new Event();
		e.setId(0);
		e.setName("Event");
		e.setDate(LocalDate.now());
		e.setTime(LocalTime.now());
		e.setVenue(v);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAll();
	}
	
	@Test
	public void getEvent() throws Exception{
		long id = 1;
		long venId = 2;
		String name = "party";
		String venueName = "partyVenue";
		LocalDate date = LocalDate.now();
		LocalTime time = LocalTime.of(0,0);
		
		Event e = new Event();
		Venue v = new Venue();
		v.setName(venueName);
		v.setId(venId);
		
		e.setDate(date);
		e.setTime(time);
		e.setName(name);
		e.setId(id);
		e.setVenue(v);
		
		when(eventService.findOne(id)).thenReturn(Optional.of(e));
		
		mvc.perform(get("/api/events/{id}", id).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(handler().methodName("getEvent"))
			.andExpect(jsonPath("$.name", equalTo(name)))
			.andExpect(jsonPath("$.date", equalTo(date.toString())))
			.andExpect(jsonPath("$.time", startsWith(time.toString())))
			.andExpect(jsonPath("$._links.length()", equalTo(3)))    // self, venue, event
			.andExpect(jsonPath("$._links.self.href", endsWith("/events/" + id)))
			.andExpect(jsonPath("$._links.venue.href", endsWith(id + "/venue")))
			.andExpect(jsonPath("$._links.event.href", endsWith("/events/" + id)));
	
		verify(eventService).findOne(id);	
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEvent"));
	}
	

	@Test
	public void getUpdateEvent() throws Exception {
		mvc.perform(get("/api/events/update").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
				.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getUpdateEventNoAuth() throws Exception {
		mvc.perform(get("/api/events/update")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getUpdateEventBadAuth() throws Exception {
		mvc.perform(get("/api/events/update").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getUpdateEventBadRole() throws Exception {
		mvc.perform(get("/api/events/update").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
	}

	@Test
	public void getNewEvent() throws Exception {
		mvc.perform(get("/api/events/new").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable());
	}
	@Test
	public void getNewEventNoAuth() throws Exception {
		mvc.perform(get("/api/events/new")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getNewEventBadAuth() throws Exception {
		mvc.perform(get("/api/events/new").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}
	
	@Test
	public void getNewEventBadRole() throws Exception {
		mvc.perform(get("/api/events/new").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
	}
	
	@Test
	public void updateEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("updateEvent"));

		verify(eventService).save(arg.capture());
		assertThat("Party", equalTo(arg.getValue().getName()));
		assertThat(LocalDate.of(3004, 1, 1), equalTo(arg.getValue().getDate()));
		assertThat(LocalTime.parse("20:54:49.9953069"), equalTo(arg.getValue().getTime()));
		assertThat("This is a fun party", equalTo(arg.getValue().getDescription()));
	}
	
	@Test
	public void updateEventNotFound() throws Exception{
		mvc.perform(put("/api/events/updateEvent/99").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":99,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("updateEvent"));;
				
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventNoAuth() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadAuth() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(anonymous())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadRole() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadDate() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"2004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyDate() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":null,\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventLongName() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyName() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventLongDescription() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyVenue() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/api/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":1,\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("updateEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/api/events/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
				.andExpect(content().string("")).andExpect(handler().methodName("deleteEvent"));

		verify(eventService).deleteById(1);
	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/events/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventNoAuth() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/events/1")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).deleteById(1);
	}


	@Test
	public void deleteEventBadAuth() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);
		
		mvc.perform(delete("/api/events/1").with(anonymous())
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventBadRole() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/api/events/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(eventService, never()).deleteById(1);
	}

	@Test
	public void createEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("newEvent"));
		
		verify(eventService).save(arg.capture());
		assertThat("Party", equalTo(arg.getValue().getName()));
		assertThat(LocalDate.of(3004, 1, 1), equalTo(arg.getValue().getDate()));
		assertThat(LocalTime.parse("20:54:49.9953069"), equalTo(arg.getValue().getTime()));
		assertThat("This is a fun party", equalTo(arg.getValue().getDescription()));
	}

	@Test
	public void createEventNoAuth() throws Exception {
		mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventBadAuth() throws Exception {
		mvc.perform(post("/api/events").with(anonymous())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void createEventBadRole() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventBadDate() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"2004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyDate() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":null,\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventLongName() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyName() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"\",\"description\":\"This is a fun party\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventLongDescription() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh\",\"venue\":{\"id\":0,\"name\":null,\"capacity\":0,\"postCode\":null,\"roadName\":null,\"events\":null}}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyVenue() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"3004-01-01\",\"time\":\"20:54:49.9953069\",\"name\":\"Party\",\"description\":\"This is a fun party\",\"venue\":null}")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("newEvent"));;

		verify(eventService, never()).save(any(Event.class));
	}
}