package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@Configuration
@Profile("test")
public class TestDataLoader {

	private final static Logger log = LoggerFactory.getLogger(TestDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			// Build and save test events and venues here.
			// The test database is configured to reside in memory, so must be initialized
			// every time.
			Venue venue = new Venue();
			venue.setCapacity(3000);
			venue.setName("Kilburn");
			venue.setPostCode("M13 9PL");
			venue.setRoadName("Oxford Road");
			venue.setLngLat();
			venueService.save(venue);
			Event event = new Event();
			event.setName("Gundeep's Birthday");
			event.setDate(LocalDate.of(3004, 8, 1));
			event.setTime(LocalTime.of(0, 0));
			event.setVenue(venue);
			eventService.save(event);
			
			Venue venue2 = new Venue();
			venue.setCapacity(3000);
			venue.setName("Blank");
			venue.setPostCode("M13 9PL");
			venue.setRoadName("Oxford Road");
			venueService.save(venue2);
		};
	}
}
