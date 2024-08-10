package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest{
	@Autowired
	private MockMvc mvc;
	
	@Mock
	private Venue venue;
	
	@MockBean
	private VenueService venueService;
	
	@MockBean
	private EventService eventSerive;
	
	@Test
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());
		
		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
		.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));
		
		verify(venueService).findAll();
		verifyNoInteractions(venue);
	}
	
	@Test
	public void getIndexWithVenues() throws Exception { 
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
		
		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
		.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenues"));
		
		verify(venueService).findAll();
	}
	
	@Test
	public void getVenue() throws Exception{
		when(venueService.findOne(1)).thenReturn(Optional.of(venue));
		
		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
		.andExpect(view().name("venues/venue")).andExpect(handler().methodName("getVenue"));
		
		verify(venueService).findOne(1);
	}
	
	@Test
	public void getNewVenue() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/new").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("venues/new"))
				.andExpect(handler().methodName("createVenue"));
	}
	
	@Test
	public void getNewVenueBadRole() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/new").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
	}
	
	@Test
	public void getNewVenueNoAuth() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/new").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
		.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getNewVenueBadAuth() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/new").with(anonymous()).accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
		.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateVenueNoAuth() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/update/1").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateVenueBadAuth() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/update/1").with(anonymous()).accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateVenueBadRole() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(get("/venues/update/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
	}
	
	@Test
	public void getUpdateVenue() throws Exception{
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findOne(1)).thenReturn(Optional.of(venue));
		
		mvc.perform(get("/venues/update/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
			.andExpect(status().isOk()).andExpect(view().name("venues/update"))
			.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getUpdateVenueNotFound() throws Exception{
		mvc.perform(get("/venues/update/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
			.andExpect(status().isNotFound()).andExpect(view().name("venues/not_found"))
			.andExpect(handler().methodName("update"));
	}
	
	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/venues/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
		.andExpect(view().name("venues/not_found")).andExpect(handler().methodName("getVenue"));
	}
	
	@Test 
	public void createVenue() throws Exception{
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf()))
				.andExpect(status().isFound()).andExpect(view().name("redirect:/venues"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeExists("ok_message"));
		
		verify(venueService).save(arg.capture());
		assertThat("Engineering Building A", equalTo(arg.getValue().getName()));
		assertNotNull(arg.getValue().getLatitude());
		assertNotNull(arg.getValue().getLongitude());
	}
	
	@Test 
	public void createVenueNoAuth() throws Exception{
		mvc.perform(post("/venues")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueBadRole() throws Exception{
		mvc.perform(post("/venues").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueNoCsrf() throws Exception{
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueLongName() throws Exception{
		final String badName = "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj";
	
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", badName)
				.param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyName() throws Exception{
		final String badName = "";
	
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", badName)
				.param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	
	@Test 
	public void createVenueBadCapacity() throws Exception{
		final String badCapacity = "0";
		
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", badCapacity).param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyCapacity() throws Exception{
		final String badCapacity = "";
		
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", badCapacity).param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));	
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueBadPostCode() throws Exception{
		final String badPostCode = "not a postcode";
		
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", badPostCode).param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "postCode"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyPostCode() throws Exception{
		final String badPostCode = "";
	
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", badPostCode).param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "postCode"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueLongRoadName() throws Exception{
		final String badName = "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha";
	
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", badName)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "roadName"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test 
	public void createVenueEmptyRoadName() throws Exception{
		final String badName = "";
	
		mvc.perform(post("/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", badName)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/new")).andExpect(model().attributeHasFieldErrors("venue", "roadName"))
				.andExpect(handler().methodName("newVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void deleteVenue() throws Exception {
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		mvc.perform(delete("/venues/" + id).with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/venues")).andExpect(handler().methodName("deleteVenue"));

		verify(venueService).deleteById(id);
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
		int id = 1;
		when(venueService.existsById(id)).thenReturn(false);

		mvc.perform(delete("/venues/" + id).with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isNotFound()).andExpect(view().name("venues/not_found"))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService, never()).deleteById(id);
	}
	
	
	@Test
	public void deleteVenueWithEvents() throws Exception {
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		when(venueService.hasEvents(id)).thenReturn(true);

		mvc.perform(delete("/venues/" + id).with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isFound()).andExpect(view().name("redirect:/venues"))
				.andExpect(handler().methodName("deleteVenue"));

		verify(venueService, never()).deleteById(id);
	}
	
	
	@Test
	public void deleteVenueNoAuth() throws Exception{
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		
		mvc.perform(delete("/venues/" + id).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(venueService, never()).deleteById(id);
	}
	
	@Test
	public void deleteVenueBadAuth() throws Exception{
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		
		mvc.perform(delete("/venues/" + id).with(anonymous()).accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(venueService, never()).deleteById(id);
	}
	
	@Test
	public void deleteVenueBadRole() throws Exception{
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		
		mvc.perform(delete("/venues/" + id).with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.accept(MediaType.TEXT_HTML)
				.with(csrf())).andExpect(status().isForbidden());
		
		verify(venueService, never()).deleteById(id);
	}
	
	@Test
	public void deleteVenueNoCsrf() throws Exception{
		int id = 1;
		when(venueService.existsById(id)).thenReturn(true);
		
		mvc.perform(delete("/venues/" + id).with(user("Rob").roles(Security.ADMIN_ROLE))
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isForbidden());
		
		verify(venueService, never()).deleteById(id);
	}
	
	@Test
	public void updateVenue() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeExists("ok_message"));
	
		verify(venueService).save(arg.capture());
		assertThat("Engineering Building A", equalTo(arg.getValue().getName()));
		assertNotNull(arg.getValue().getLatitude());
		assertNotNull(arg.getValue().getLongitude());
	}
	
	@Test
	public void updateVenueNotFound() throws Exception {
		mvc.perform(put("/venues/updateVenue/99").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found")).andExpect(handler().methodName("updateVenue"));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueNoAuth() throws Exception {
		mvc.perform(put("/venues/updateVenue/1")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadAuth() throws Exception {
		mvc.perform(put("/venues/updateVenue/1").with(anonymous())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadRole() throws Exception {
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.EVENT_ATTENDEE_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueNoCsrf() throws Exception {
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueLongName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		final String badName = "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowj";
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", badName)
				.param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadCapacity() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "0").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	} 
	
	@Test
	public void updateVenueEmptyCapacity() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "").param("postCode", "M13 9PL").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueBadPostCode() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "aaa").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "postCode"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyPostCode() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "").param("roadName", "Oxford Road")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "postCode"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueLongRoadName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL")
				.param("roadName", "Partyahfoshidjdfbdjiwjuwhuehhjfhjghjgfjhgfhjgafahjgfajhgfjhgafhjadgfahgfhagfafhhhhhhhhhhhhujahdbhergyufghdudhfjkabfhjgyuqegyuewgwheeqgwbyugyfgfnjdhbfhjdsbahfgahgfhjdahbgvhjfnj fhgajkahfjabfhjagffreuhfuiewhrufhbhqfbhqhwyufhgsdgfbhjagfhjgfhjshdhfhfhjffkwhhsowjddjdhuveuwfiuwjifuhewuifhwfwmfklewjfieqefhu3hfjhbhf3hbufhuhfewhjfha")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "roadName"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void updateVenueEmptyRoadName() throws Exception {
		when(venueService.existsById(1)).thenReturn(true);
		
		mvc.perform(put("/venues/updateVenue/1").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "Engineering Building A").param("capacity", "100").param("postCode", "M13 9PL").param("roadName", "")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/update")).andExpect(model().attributeHasFieldErrors("venue", "roadName"))
				.andExpect(handler().methodName("updateVenue")).andExpect(flash().attributeCount(0));
		
		verify(venueService, never()).save(any(Venue.class));
	}
}