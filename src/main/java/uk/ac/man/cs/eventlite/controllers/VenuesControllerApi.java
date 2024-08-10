package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueHasEventsException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@RestController
@RequestMapping(value = "/api/venues", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class VenuesControllerApi{
	private static final String ERROR_MSG = "{ \"error\": \"%s\", \"id\": %d }";
	
	@Autowired
	private VenueService venueService;
	
	@Autowired
	private VenueModelAssembler venueAssembler;
	
	@Autowired
	private EventService eventService;
	
	@Autowired
	private EventModelAssembler eventAssembler;
	
	@ExceptionHandler(VenueNotFoundException.class)
	public ResponseEntity<?> venueNotFoundHandler(VenueNotFoundException ex){
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(String.format(ERROR_MSG, ex.getMessage(), ex.getId()));
	}
	
	@ExceptionHandler(VenueHasEventsException.class)
	public ResponseEntity<?> venueHasEventsHandler(VenueHasEventsException ex){
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(String.format(ERROR_MSG, ex.getMessage(), ex.getId()));
	}
	
	@GetMapping("/{id}")
	public EntityModel<Venue> getVenue(@PathVariable("id") long id){
		return venueAssembler.toModel(venueService.findOne(id).orElseThrow(() -> new VenueNotFoundException(id)));
	}
	
	@GetMapping
	public CollectionModel<EntityModel<Venue>> getAllVenues(){
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		
		return venueAssembler.toCollectionModel(venueService.findAll())
				.add(linkTo(methodOn(VenuesControllerApi.class).getAllVenues()).withSelfRel())
				.add(Link.of(baseUrl + "/api/profile/venues", IanaLinkRelations.PROFILE));
	}
	

	@GetMapping("/update")
	public ResponseEntity<?> update(){
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}
	
	
	@GetMapping("/{id}/events")
	public CollectionModel<EntityModel<Event>> getEvents(@PathVariable("id") long id){
		if(!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		return eventAssembler.toCollectionModel(eventService.findAllByVenueId(id))
				.add(linkTo(methodOn(VenuesControllerApi.class).getEvents(id)).withSelfRel());
	}
	
	@GetMapping("{id}/next3events")
	public CollectionModel<EntityModel<Event>> getNextThreeEvents(@PathVariable("id") long id){
		if(!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		return eventAssembler.toCollectionModel(eventService.findFirst3ByVenueIdAndDateGreaterThan(id, LocalDate.now()))
				.add(linkTo(methodOn(VenuesControllerApi.class).getNextThreeEvents(id)).withSelfRel());
	}
	
	@PutMapping(path = "/updateVenue/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateVenue(@PathVariable("id") long id, @RequestBody @Valid Venue venue,
			BindingResult result){
		if(!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if(result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		
		Venue newVenue = venueService.save(venue);
		EntityModel<Venue> entity = venueAssembler.toModel(newVenue);
		
		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}

	@GetMapping("/new")
	public ResponseEntity<?> create(){
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> newVenue(@RequestBody @Valid Venue venue, 
			BindingResult result){	

		if(result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		
		venue.setLngLat();
		Venue newVenue = venueService.save(venue);
		EntityModel<Venue> entity = venueAssembler.toModel(newVenue);
		
		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteVenue(@PathVariable("id") long id) {
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if (venueService.hasEvents(id)) {
	        throw new VenueHasEventsException(id);
	    }
		
		venueService.deleteById(id);

		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

}