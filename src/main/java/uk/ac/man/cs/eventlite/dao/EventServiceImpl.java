package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Event;

@Service
public class EventServiceImpl implements EventService {
	
	@Autowired
	private EventRepository eventRepository;

	@Override
	public long count() {
		return eventRepository.count();
	}

	@Override
	public Iterable<Event> findAll() {
		return eventRepository.findAllByOrderByDateAscTimeAsc();
	}
	
	@Override
	public Iterable<Event> findAllByDateGreaterThan(LocalDate date) {
		return eventRepository.findAllByDateGreaterThanOrderByDateAscNameAsc(date);
	}
	
	public Iterable<Event> findAllByDateLessThan(LocalDate date) {
		return eventRepository.findAllByDateLessThanOrderByDateAscNameAsc(date);
	}
	
	public Iterable<Event> findAllByVenueId(long id){
		return eventRepository.findAllByVenueIdOrderByDateAscNameAsc(id);
	}
	
	@Override
	public Event save(Event event) {
		return eventRepository.save(event);
	}

	@Override
	public void delete(Event event) {
		eventRepository.delete(event);
	}

	@Override
	public void deleteById(long id) {
		eventRepository.deleteById(id);
	}

	@Override
	public void deleteAll() {
		eventRepository.deleteAll();
	}

	@Override
	public void deleteAll(Iterable<Event> events) {
		eventRepository.deleteAll(events);
	}

	@Override
	public void deleteAllById(Iterable<Long> ids) {
		eventRepository.deleteAllById(ids);
	}

	
	@Override
	public Optional<Event> findOne(long id) {
		return eventRepository.findById(id);
	}
	
	@Override
	public boolean existsById(long id) {
		return eventRepository.existsById(id);
	}
	
	@Override
	public Iterable<Event> findTop3UpcomingEvents(){
		// Get all events from the repository
        Iterable<Event> allEvents = eventRepository.findAll();
  
        // Filter the events to get upcoming events after today
        LocalDate today = LocalDate.now();
        List<Event> upcomingEvents = new ArrayList<>();
        for (Event event : allEvents) {
            if (event.getDate().isAfter(today)) {
                upcomingEvents.add(event);
            }
        }

        // Sort the upcoming events by date
        upcomingEvents.sort((e1, e2) -> e1.getDate().compareTo(e2.getDate()));

        // If there are more than 3 events, return only the top 3
        return upcomingEvents.subList(0, Math.min(upcomingEvents.size(), 3));
	}
	
	@Override
	public Iterable<Event> findFirst3ByVenueIdAndDateGreaterThan(long venueId, LocalDate date){
		return eventRepository.findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(venueId, date);
	}
}

