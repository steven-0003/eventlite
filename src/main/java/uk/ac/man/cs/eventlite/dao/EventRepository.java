package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;

import org.springframework.data.repository.CrudRepository;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventRepository extends CrudRepository<Event, Long>{
	public Iterable<Event> findAllByOrderByDateAscTimeAsc();
	
	//public Iterable<Event> findAllByDateBetweenOrderByDateAscTimeAsc(LocalDate startDate, LocalDate endDate);
	
	public Iterable<Event> findAllByDateGreaterThanOrderByDateAscNameAsc(LocalDate date);
	public Iterable<Event> findAllByDateLessThanOrderByDateAscNameAsc(LocalDate date);
	public Iterable<Event> findFirst3ByVenueIdAndDateGreaterThanOrderByDateAscNameAsc(long venueId, LocalDate date);
	public Iterable<Event> findAllByVenueIdOrderByDateAscNameAsc(long venueId);
}
