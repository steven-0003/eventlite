package uk.ac.man.cs.eventlite.config.data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.entities.Event;

@Configuration
@Profile("default")
public class InitialDataLoader {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			if (venueService.count() > 0) {
				log.info("Database already populated with venues. Skipping venue initialization.");
			} else {
				// Initial venues
				Venue engBuildingA = new Venue();
				engBuildingA.setCapacity(10000);
				engBuildingA.setName("Engineering Building A");
				engBuildingA.setPostCode("M13 9SS");
				engBuildingA.setRoadName("Booth St E");
				engBuildingA.setLngLat();
				venueService.save(engBuildingA);
				
				Venue kilburnMegaLab = new Venue();
				kilburnMegaLab.setCapacity(100);
				kilburnMegaLab.setName("Kilburn Mega Lab");
				kilburnMegaLab.setPostCode("M13 9PY");
				kilburnMegaLab.setRoadName("Oxford Road");
				kilburnMegaLab.setLngLat();
				venueService.save(kilburnMegaLab);
				
				Venue SimonBuilding = new Venue();
				SimonBuilding.setCapacity(1000000000);
				SimonBuilding.setName("Simon Building");
				SimonBuilding.setPostCode("M13 9PS");
				SimonBuilding.setRoadName("Brunswick St");
				SimonBuilding.setLngLat();
				venueService.save(SimonBuilding);
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				// Build and save initial events here.
				Iterator<Venue> iterator = venueService.findAll().iterator();

				
				Event event = new Event();
				event.setName("Birthday");
				event.setDate(LocalDate.of(2026, 8, 1));
				event.setTime(LocalTime.of(0, 0));
				event.setVenue(iterator.next());
				eventService.save(event);
				
				Event event2 = new Event();
				event2.setName("Party 2");
				event2.setDate(LocalDate.of(2026, 5, 17));
				event2.setTime(LocalTime.of(19, 0));
				event2.setVenue(iterator.next());
				eventService.save(event2);
				
				Event event3 = new Event();
				event3.setName("Party 1");
				event3.setDate(LocalDate.of(2025, 2, 17));
				event3.setTime(LocalTime.of(22, 0));
				event3.setVenue(iterator.next());
				eventService.save(event3);
			}
		};
	}
}
