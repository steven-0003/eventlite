package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import java.time.LocalDate;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventService {
	
	public Event save(Event event);

	public long count();

	public Iterable<Event> findAll();
	
	public Optional<Event> findOne(long id);
	
	public boolean existsById(long id);
	
	public void delete(Event event);

	public void deleteById(long id);

	public void deleteAll();

	public void deleteAll(Iterable<Event> events);

	public void deleteAllById(Iterable<Long> ids);
	

	public Iterable<Event> findTop3UpcomingEvents();
	
	public Iterable<Event> findAllByVenueId(long id);
	
	public Iterable<Event> findAllByDateGreaterThan(LocalDate date);
	
	public Iterable<Event> findAllByDateLessThan(LocalDate date);
	
	public Iterable<Event> findFirst3ByVenueIdAndDateGreaterThan(long venueId, LocalDate startDate);
}
