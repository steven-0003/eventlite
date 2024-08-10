package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.mastodon.MastodonService;

import uk.ac.man.cs.eventlite.mastodon.MastodonService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;
	
	@MockBean
	private MastodonService mastodonService;
	
	@Test
	public void mastodonPostSuccess() throws Exception{
		long id = 1;
		final String content = "new post";
		when(mastodonService.makePost(content)).thenReturn(true);
		
		mvc.perform(post("/events/" + id + "/mastodonPost").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("content", content))
				.andExpect(status().isFound())
				.andExpect(view().name("redirect:/events/" + id)).andExpect(handler().methodName("mastodonPost"))
				.andExpect(flash().attributeCount(1))
				.andExpect(flash().attribute("ok_message", containsString(content)));
		
		verify(mastodonService).makePost(content);
	}
	
	@Test
	public void mastodonPostFailiure() throws Exception{
		long id = 1;
		final String content = "new post";
		when(mastodonService.makePost(content)).thenReturn(false);
		
		mvc.perform(post("/events/" + id + "/mastodonPost").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("content", content))
				.andExpect(status().isFound())
				.andExpect(view().name("redirect:/events/" + id)).andExpect(handler().methodName("mastodonPost"))
				.andExpect(flash().attributeCount(0));
		
		verify(mastodonService).makePost(content);
	}
	
	@Test
	public void mastodonPostNoContent() throws Exception{
		long id = 1;
		
		mvc.perform(post("/events/" + id + "/mastodonPost").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isBadRequest());
		
		verify(mastodonService, never()).makePost(any(String.class));
	}
	
	
	@Test
	public void mastodonPostNoUser() throws Exception{
		long id = 1;
		final String content = "new post";
		
		mvc.perform(post("/events/" + id + "/mastodonPost")
				.accept(MediaType.TEXT_HTML).with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("content", content))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(mastodonService, never()).makePost(any(String.class));
	}
	

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAll()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
		verify(mastodonService).lastThreePosts();
		verifyNoInteractions(event);
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		when(event.getVenue()).thenReturn(venue);
		when(eventService.findAll()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findAll();
		verify(mastodonService).lastThreePosts();
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/events/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getEvent() throws Exception {
		int id = 1;
		when(eventService.findOne(id)).thenReturn(Optional.of(event));
		when(event.getVenue()).thenReturn(venue);   // otherwise thymeleaf evaluation of event.venue fails (not the purpose of this test)

		mvc.perform(get("/events/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/event")).andExpect(handler().methodName("getEvent"));
		
		verify(eventService).findOne(id);
	}
	
	@Test
	public void getUpdateEventNoAuth() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/update/1").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateEventBadAuth() throws Exception{
		mvc.perform(get("/events/update/1").with(anonymous())
				.accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateEventBadRole() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/update/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
	}
	
	@Test
	public void getUpdateEvent() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		when(eventService.findOne(1)).thenReturn(Optional.of(event));
		
		mvc.perform(get("/events/update/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("events/update"))
				.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getUpdateEventNotFound() throws Exception {
		mvc.perform(get("/events/update/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
			.andExpect(status().isNotFound()).andExpect(view().name("events/not_found"))
			.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getNewEvent() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/new").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("events/new"))
				.andExpect(handler().methodName("createEvent"));
	}
	
	@Test
	public void getNewEventBadRole() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/new").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
	}
	
	@Test
	public void getNewEventNoAuth() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/new").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
		.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getNewEventBadAuth() throws Exception{
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/events/new").with(anonymous()).accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
		.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void updateEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01").param("time", "00:00").param("description", "This is a fun party")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
		assertThat("Party", equalTo(arg.getValue().getName()));
	}
	
	@Test
	public void updateEventNotFound() throws Exception {
		mvc.perform(put("/events/updateEvent/99").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01").param("time", "00:00").param("description", "This is a fun party")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("updateEvent"));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventNoAuth() throws Exception {
		mvc.perform(put("/events/updateEvent/1")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Party").param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadRole() throws Exception {
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadAuth() throws Exception {
		mvc.perform(put("/events/updateEvent/1").with(anonymous())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventNoCsrf() throws Exception {
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadDate() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "2004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyDate() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventBadTime() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01").param("time", "25:00")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "time"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventLongName() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj")
				.param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyName() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "").param("venue.id", "1").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventLongDescription() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Party").param("venue.id", "1").param("date", "3004-08-01")
				.param("description", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjfhjffjkf Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhfhwbfhbwhdjdhuewhudh")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void updateEventEmptyVenue() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/events/updateEvent/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "").param("venue.id", "").param("date", "3004-08-01")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/update")).andExpect(model().attributeHasFieldErrors("event", "venue.id"))
				.andExpect(handler().methodName("updateEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void createEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeExists("ok_message"));
		
		verify(eventService).save(arg.capture());
		assertThat("gome jim", equalTo(arg.getValue().getName()));
		assertThat(LocalDate.parse("2025-04-17"), equalTo(arg.getValue().getDate()));
		assertThat(LocalTime.parse("14:30"), equalTo(arg.getValue().getTime()));
	}
	
	@Test
	public void createEventNoAuth() throws Exception {
		mvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventBadAuth() throws Exception {
		mvc.perform(post("/events").with(anonymous())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void createEventBadRole() throws Exception {
		mvc.perform(post("/events").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventNoCsrf() throws Exception {
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventBadDate() throws Exception {
		final String badDate = "0001-01-01";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", badDate).param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyDate() throws Exception {
		final String badDate = "";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", badDate).param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventBadTime() throws Exception {
		final String badTime = "0001-01-01";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", badTime)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "time"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventLongName() throws Exception {
		final String badName = "gome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jimgome jim";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", badName)
				.param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyName() throws Exception {
		final String badName = "";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", badName)
				.param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventLongDescription() throws Exception {
		final String badDescription = "make a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a gamemake a game";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "gome jim").param("venue.id", "1").param("date", "2025-04-17").param("time", "14:30")
				.param("description", badDescription)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void createEventEmptyVenue() throws Exception {
		final String badVenue = "";
		
		mvc.perform(post("/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "gome jim")
				.param("venue.id", badVenue).param("date", "2025-04-17").param("time", "14:30")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/new")).andExpect(model().attributeHasFieldErrors("event", "venue.id"))
				.andExpect(handler().methodName("newEvent")).andExpect(flash().attributeCount(0));
		
		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/events/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/events")).andExpect(handler().methodName("deleteEvent"))
				.andExpect(flash().attributeExists("ok_message"));

		verify(eventService).deleteById(1);
	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);

		mvc.perform(delete("/events/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isNotFound()).andExpect(view().name("events/not_found"))
				.andExpect(handler().methodName("deleteEvent"));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventNoAuth() throws Exception {
		mvc.perform(delete("/events/1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventBadAuth() throws Exception {
		mvc.perform(delete("/events/1").with(anonymous())
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventBadRole() throws Exception {
		mvc.perform(delete("/events/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(eventService, never()).deleteById(1);
	}
	
	@Test
	public void deleteEventNoCsrf() throws Exception {
		mvc.perform(delete("/events/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(eventService, never()).deleteById(1);
	}
}
