package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import jakarta.validation.Valid;
import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;

@RestController
@RequestMapping(value = "/api/events", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class EventsControllerApi {

	private static final String NOT_FOUND_MSG = "{ \"error\": \"%s\", \"id\": %d }";

	@Autowired
	private EventService eventService;

	@Autowired
	private EventModelAssembler eventAssembler;

	@ExceptionHandler(EventNotFoundException.class)
	public ResponseEntity<?> eventNotFoundHandler(EventNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(String.format(NOT_FOUND_MSG, ex.getMessage(), ex.getId()));
	}

	@GetMapping("/{id}")
	public EntityModel<Event> getEvent(@PathVariable("id") long id) {
		return eventAssembler.toModel(eventService.findOne(id).orElseThrow(() -> new EventNotFoundException(id)));
	}

	@GetMapping
	public CollectionModel<EntityModel<Event>> getAllEvents() {
		return eventAssembler.toCollectionModel(eventService.findAll())
				.add(linkTo(methodOn(EventsControllerApi.class).getAllEvents()).withSelfRel());
	}

	@GetMapping("/update")
	public ResponseEntity<?> update(){
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}
	
	@PutMapping(path = "/updateEvent/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateEvent(@PathVariable("id") long id, @RequestBody @Valid Event event, 
			BindingResult result){	
		if(!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		
		if(result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		
		Event newEvent = eventService.save(event);
		EntityModel<Event> entity = eventAssembler.toModel(newEvent);
		
		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteEvent(@PathVariable("id") long id) {
		if (!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		
		eventService.deleteById(id);

		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@GetMapping("/new")
	public ResponseEntity<?> create(){
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> newEvent(@RequestBody @Valid Event event, 
			BindingResult result){	
		
		
		if(result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}
		Event newEvent = eventService.save(event);
		EntityModel<Event> entity = eventAssembler.toModel(newEvent);
		
		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}
}
