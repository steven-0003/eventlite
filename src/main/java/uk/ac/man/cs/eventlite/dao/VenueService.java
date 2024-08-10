package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueService {

	public long count();

	public Iterable<Venue> findAll();
	
	public Venue save(Venue ven);
	
	public boolean existsById(long id);
	
	public Optional<Venue> findOne(long id);
	

	public Iterable<Venue> findTop3Venues();

	public void delete(Venue venue);

	public void deleteById(long id);

	public void deleteAll();

	public void deleteAll(Iterable<Venue> venues);

	public void deleteAllById(Iterable<Long> ids);
	
	public boolean hasEvents(long id);

}
