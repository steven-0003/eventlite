package uk.ac.man.cs.eventlite.dao;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;


@ExtendWith(SpringExtension.class)
@DataJpaTest
public class EventRepositoryTest {
	@Autowired
	private TestEntityManager entityManager;
	
	@Autowired
	private EventRepository eventRepository;
	
	private Venue v;
	private LocalDate date;
	
	@BeforeEach
	public void initiateVenue() {
		v = new Venue();
		v.setName("Venue");
		v.setPostCode("M14 9PL");
		v.setRoadName("Oxford Road");
		v.setCapacity(10);
		entityManager.persist(v);
		
		/* We must assign dates relative to time of test as Event requires @Future, 
		 * and setting to some random future date would eventually introduce failures 
		 *  - in short this is to ensure these aren't flaky tests. */
		date = LocalDate.now();
	}
	
	
	@Test
	void TestFindAllByDateGreaterThanOrderByDateAscTimeAsc_WithValidEvents() {
		
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		Event e1 = new Event();
		e1.setName("e1");
		e1.setVenue(v);
		e1.setDate(date.plusDays(10));
		e1.setTime(LocalTime.of(20, 20));
		entityManager.persist(e1);
		targetOutput.add(e1);
		
		Event e2 = new Event();
		e2.setName("e2");
		e2.setVenue(v);
		e2.setDate(date.plusDays(10));
		e2.setTime(LocalTime.of(20, 32));  // test time order is obeyed
		entityManager.persist(e2);
		targetOutput.add(e2);
		
		Event e3 = new Event();
		e3.setName("e3");
		e3.setVenue(v);
		e3.setDate(date.plusDays(12));
		e3.setTime(LocalTime.of(10, 12));
		entityManager.persist(e3);
		targetOutput.add(e3);
		
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findAllByDateGreaterThanOrderByDateAscNameAsc(date);
		
		
		assertEquals(output, targetOutput);
	}

	
	@Test
	void TestFindAllByDateGreaterThanOrderByDateAscTimeAsc_WithInvalidEvents() {
		
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		Event e1 = new Event();
		e1.setName("e1");
		e1.setDate(date.plusDays(1));
		e1.setTime(LocalTime.of(20, 20));
		e1.setVenue(v);
		entityManager.persist(e1);
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findAllByDateGreaterThanOrderByDateAscNameAsc(date.plusDays(2));
		
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindAllByDateLessThanOrderByDateAscTimeAsc_WithValidEvents() {
		
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		Event e1 = new Event();
		e1.setName("e1");
		e1.setVenue(v);
		e1.setDate(date.plusDays(1));
		e1.setTime(LocalTime.of(20, 20));
		entityManager.persist(e1);
		targetOutput.add(e1);
		
		Event e2 = new Event();
		e2.setName("e2");
		e2.setVenue(v);
		e2.setDate(date.plusDays(1));
		e2.setTime(LocalTime.of(20, 32));  // test time order is obeyed
		entityManager.persist(e2);
		targetOutput.add(e2);
		
		Event e3 = new Event();
		e3.setName("e3");
		e3.setVenue(v);
		e3.setDate(date.plusDays(2));
		e3.setTime(LocalTime.of(10, 12));
		entityManager.persist(e3);
		targetOutput.add(e3);
		
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findAllByDateLessThanOrderByDateAscNameAsc(date.plusDays(10));
		
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindAllByDateLessThanOrderByDateAscTimeAsc_WithInvalidEvents() {
		
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		Event e1 = new Event();
		e1.setName("e1");
		e1.setDate(date.plusDays(10));
		e1.setTime(LocalTime.of(20, 20));
		e1.setVenue(v);
		entityManager.persist(e1);
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findAllByDateLessThanOrderByDateAscNameAsc(date);
		
		
		assertEquals(output, targetOutput);
	}
	
	
	@Test
	void TestFindFirst3ByVenueIdAndDateGreaterThanOrderByDateAscTimeAsc_WithValidEvents() {
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		
		Event e1 = new Event();
		e1.setName("e1");
		e1.setDate(date.plusDays(10));
		e1.setTime(LocalTime.of(20, 20));
		e1.setVenue(v);
		entityManager.persist(e1);
		targetOutput.add(e1);
		
		Event e2 = new Event();
		e2.setName("e2");
		e2.setDate(date.plusDays(10));
		e2.setTime(LocalTime.of(20, 32));
		e2.setVenue(v);
		entityManager.persist(e2);
		targetOutput.add(e2);
		
		Event e3 = new Event();
		e3.setName("e3");
		e3.setDate(date.plusDays(11));
		e3.setTime(LocalTime.of(10, 12));
		e3.setVenue(v);
		entityManager.persist(e3);
		targetOutput.add(e3);
		
		Event e4 = new Event();
		e4.setName("e4");
		e4.setDate(date.plusDays(12));
		e4.setTime(LocalTime.of(10, 12));
		e4.setVenue(v);
		entityManager.persist(e4);
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(v.getId(), date);
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindFirst3ByVenueIdAndDateGreaterThanOrderByDateAscTimeAsc_WithInvalidVenueEvents() {
		ArrayList<Event> targetOutput = new ArrayList<Event>();

		Venue v2 = new Venue();
		v2.setName("Venue");
		v2.setPostCode("M14 9PL");
		v2.setRoadName("Oxford Road");
		v2.setCapacity(10);
		entityManager.persist(v2);
		
		Event event = new Event();
		event.setName("e1");
		event.setDate(date.plusDays(1));
		event.setTime(LocalTime.of(20, 20));
		event.setVenue(v2);
		entityManager.persist(event);
		

		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(v.getId(), date);
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindFirst3ByVenueIdAndDateGreaterThanOrderByDateAscTimeAsc_WithInvalidDateEvents() {
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		
		Event event = new Event();
		event.setName("e1");
		event.setDate(date.plusDays(1));
		event.setTime(LocalTime.of(20, 20));
		event.setVenue(v);
		entityManager.persist(event);
		
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(v.getId(), date.plusDays(1));
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindAllByVenueIdOrderByDateAscTimeAsc_WithValidEvents() {
		ArrayList<Event> targetOutput = new ArrayList<Event>();
		
		Event e1 = new Event();
		e1.setName("e1");
		e1.setDate(date.plusDays(1));
		e1.setTime(LocalTime.of(20, 20));
		e1.setVenue(v);
		entityManager.persist(e1);
		targetOutput.add(e1);
		
		Event e2 = new Event();
		e2.setName("e2");
		e2.setDate(date.plusDays(1));
		e2.setTime(LocalTime.of(20, 32));
		e2.setVenue(v);
		entityManager.persist(e2);
		targetOutput.add(e2);
		
		Event e3 = new Event();
		e3.setName("e3");
		e3.setDate(date.plusDays(10));
		e3.setTime(LocalTime.of(10, 12));
		e3.setVenue(v);
		entityManager.persist(e3);
		targetOutput.add(e3);
		
		Event e4 = new Event();
		e4.setName("e4");
		e4.setDate(date.plusDays(12));
		e4.setTime(LocalTime.of(10, 12));
		e4.setVenue(v);
		entityManager.persist(e4);
		targetOutput.add(e4);
		
		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findAllByVenueIdOrderByDateAscNameAsc(v.getId());
		
		assertEquals(output, targetOutput);
	}
	
	@Test
	void TestFindAllByVenueIdOrderByDateAscTimeAsc_WithInvalidEvents() {
		ArrayList<Event> targetOutput = new ArrayList<Event>();

		ArrayList<Event> output = 
				(ArrayList<Event>)eventRepository.findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(v.getId(), date);
		
		assertEquals(output, targetOutput);
	}
	
}

	
