package uk.ac.man.cs.eventlite.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;

import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueHasEventsException;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@Controller
@RequestMapping(value = "/venues", produces = {MediaType.TEXT_HTML_VALUE})
public class VenuesController{
	@Autowired
	private VenueService venueService;
	
	@Autowired
	private EventService eventService;
	
	@ExceptionHandler(VenueNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String venueNotFoundHandler(VenueNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "venues/not_found";
	}
	
	@ExceptionHandler(VenueHasEventsException.class)
	@ResponseStatus(HttpStatus.FOUND)
	public String venueHasEventsHandler(VenueHasEventsException ex, Model model, 
			RedirectAttributes redirectAttrs) {
		redirectAttrs.addFlashAttribute("error_message", "Venue cannot be deleted if it has one or more events.");
		return "redirect:/venues";
	}
	
	@GetMapping("/{id}")
	public String getVenue(@PathVariable("id") long id, Model model) {
		model.addAttribute("venue", venueService.findOne(id).orElseThrow(() -> new VenueNotFoundException(id)));    // set 'venue'
		model.addAttribute("v_events", eventService.findAllByVenueId(id));
		
		return "venues/venue";    // redirect to 'venue' page to display details 
	}
	
	@GetMapping
	public String getAllVenues(Model model) {
		model.addAttribute("venues", venueService.findAll());
		
		return "venues/index";
	}
	
	@DeleteMapping("/{id}")
	public String deleteVenue(Model model, @PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		 if (venueService.hasEvents(id)) {
			 throw new VenueHasEventsException(id);
		}
			 
		venueService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Venue successfully deleted.");
		return "redirect:/venues";
	}

	@GetMapping("/update/{id}")
	public String update(Model model, @PathVariable("id") long id) {
		if(!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if(!model.containsAttribute("venue")) {
			model.addAttribute("venue", venueService.findOne(id).get());
		}
		
		return "venues/update";
	}
	
	@PutMapping(path = "/updateVenue/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors, @PathVariable("id") long id,
			Model model, RedirectAttributes redirectAttrs) {
		if(!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if(errors.hasErrors()) {
			model.addAttribute(venue);
			return "venues/update";
		}
		
		venue.setLngLat();
		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "Venue successfully updated.");
		return "redirect:/venues";
	}

	@GetMapping("/new")
	public String createVenue(Model model) {
		if(!model.containsAttribute("venue")) {
			model.addAttribute("venue", new Venue());
		}
		return "venues/new";
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String newVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors, Model model, RedirectAttributes redirectAttrs) {
		
		if(errors.hasErrors()) {
			model.addAttribute("event", venue);
			return "venues/new";
		}
		
		venue.setLngLat();
		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "Venue successfully added.");
		
		return "redirect:/venues";
	}

}