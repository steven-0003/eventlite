package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HomeController.class)
@Import(Security.class)
public class HomeControllerTest {
	
	@Autowired
	MockMvc mvc;
	
	@Mock 
	private Event event1;
	
	@Mock
	private Event event2;
	
	@Mock
	private Event event3;
	
	@Mock
	private Venue venue1;
	
	@Mock
	private Venue venue2;
	
	@Mock
	private Venue venue3;
	
	@MockBean
    private EventService eventService;

    @MockBean
    private VenueService venueService;
    
    @Test
    public void testGetTop3UpcomingEvents() throws Exception {
    	
    	// Create some dummy data for upcoming events
        Event event1 = new Event();
        event1.setName("Event 1");
        event1.setDate(LocalDate.now().plusDays(1));
        event1.setTime(LocalTime.NOON);

        Event event2 = new Event();
        event2.setName("Event 2");
        event2.setDate(LocalDate.now().plusDays(2));
        event2.setTime(LocalTime.of(14, 30));

        Event event3 = new Event();
        event3.setName("Event 3");
        event3.setDate(LocalDate.now().plusDays(3));
        event3.setTime(LocalTime.of(18, 0));

        List<Event> upcomingEvents = Arrays.asList(event1, event2, event3);

        // Create some dummy data for venues
        Venue venue1 = new Venue();
        venue1.setName("Venue 1");

        Venue venue2 = new Venue();
        venue2.setName("Venue 2");

        Venue venue3 = new Venue();
        venue3.setName("Venue 3");

        // Associate venues with events
        event1.setVenue(venue1);
        event2.setVenue(venue2);
        event3.setVenue(venue3);

        // Mock the behaviour of eventService
        when(eventService.findTop3UpcomingEvents()).thenReturn(upcomingEvents);

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home"))
                .andExpect(model().attribute("upcomingEvents", upcomingEvents));

        // Verify that the eventService method was called
        verify(eventService).findTop3UpcomingEvents();
    }
    
    @Test
    public void testGetTop3Venues() throws Exception {
    	
    	// Create some dummy data for upcoming events
        Event event1 = new Event();
        event1.setName("Event 1");
        event1.setDate(LocalDate.now().plusDays(1));
        event1.setTime(LocalTime.NOON);

        Event event2 = new Event();
        event2.setName("Event 2");
        event2.setDate(LocalDate.now().plusDays(2));
        event2.setTime(LocalTime.of(14, 30));

        Event event3 = new Event();
        event3.setName("Event 3");
        event3.setDate(LocalDate.now().plusDays(3));
        event3.setTime(LocalTime.of(18, 0));

        // Create some dummy data for venues
        Venue venue1 = new Venue();
        venue1.setName("Venue 1");

        Venue venue2 = new Venue();
        venue2.setName("Venue 2");

        Venue venue3 = new Venue();
        venue3.setName("Venue 3");

        // Associate venues with events
        event1.setVenue(venue1);
        event2.setVenue(venue2);
        event3.setVenue(venue3);

        List<Venue> topVenues = Arrays.asList(venue1, venue2, venue3);

        // Mock the behaviour of  venueService
        when(venueService.findTop3Venues()).thenReturn(topVenues);

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home"))
                .andExpect(model().attribute("topVenues", topVenues));

        // Verify that the venueService method was called
        verify(venueService).findTop3Venues();
    	
    }
    
    @Test
    public void testGetTop3UpcomingEvents_emptyList() throws Exception {
        when(eventService.findTop3UpcomingEvents()).thenReturn(Collections.emptyList());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home"))
                .andExpect(model().attribute("upcomingEvents", Collections.emptyList()));

        verify(eventService).findTop3UpcomingEvents();
    }
    
    @Test
    public void testGetTop3Venues_emptyList() throws Exception {
        when(venueService.findTop3Venues()).thenReturn(Collections.emptyList());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home"))
                .andExpect(model().attribute("topVenues", Collections.emptyList()));

        verify(venueService).findTop3Venues();
    }

    @Test
    public void testGetTop3UpcomingEvents_Error() throws Exception {
        when(eventService.findTop3UpcomingEvents()).thenThrow(new RuntimeException("Error"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home_error"));

        verify(eventService).findTop3UpcomingEvents();
    }

    @Test
    public void testGetTop3Venues_Error() throws Exception {
        when(venueService.findTop3Venues()).thenThrow(new RuntimeException("Error"));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/home_error"));

        verify(venueService).findTop3Venues();
    }

    
}