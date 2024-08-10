package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.man.cs.eventlite.entities.Venue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


@Service
public class VenueServiceImpl implements VenueService {
	
	@Autowired
	private VenueRepository venueRepository;

	@Override
	public long count() {
		return venueRepository.count();
	}

	@Override
	public Iterable<Venue> findAll(){
		return venueRepository.findAll();
	}
	
	@Override
	public Venue save(Venue ven) {
		return venueRepository.save(ven);
	}

	@Override
	public boolean existsById(long id) {
		return venueRepository.existsById(id);
	}
	
	@Override
	public Optional<Venue> findOne(long id){
		return venueRepository.findById(id);
	}
	
	@Override
	public Iterable<Venue> findTop3Venues() {
		// Get all venues from the repository
        Iterable<Venue> allVenues = venueRepository.findAll();

        // Convert Iterable to List for sorting
        List<Venue> venueList = new ArrayList<>();
        allVenues.forEach(venueList::add);

        // Sort venues based on the number of events they have
        venueList.sort(Comparator.comparingInt(v -> -v.getEvents().size()));

        // If there are more than 3 venues, return only the top 3
        return venueList.subList(0, Math.min(venueList.size(), 3));
        
	}

	
	@Override
	public void delete(Venue venue) {
		venueRepository.delete(venue);
	}

	@Override
	public void deleteById(long id) {
		venueRepository.deleteById(id);
	}

	@Override
	public void deleteAll() {
		venueRepository.deleteAll();
	}

	@Override
	public void deleteAll(Iterable<Venue> venues) {
		venueRepository.deleteAll(venues);
	}

	@Override
	public void deleteAllById(Iterable<Long> ids) {
		venueRepository.deleteAllById(ids);
	}
	
	@Override
	public boolean hasEvents(long id) {
	    Optional<Venue> venueOptional = venueRepository.findById(id);
	    if (venueOptional.isPresent()) {
	        Venue venue = venueOptional.get();
	        return !venue.getEvents().isEmpty();
	    }
	    return false;
	}
}
