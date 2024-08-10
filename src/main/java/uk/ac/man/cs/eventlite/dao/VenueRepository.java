package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.repository.CrudRepository;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueRepository extends CrudRepository<Venue, Long>{
	public Iterable<Venue> findAllByName(String name);
	public Iterable<Venue> findAllByNameOrderByNameAsc(String name);
	public Venue findFirstByNameOrderByNameAsc(String name);
	
	public Venue findByNameContainingAndCapacity(String nameSearch, int capacity);
	public Iterable<Venue> findAllByCapacityBetween(int min, int max);
}
