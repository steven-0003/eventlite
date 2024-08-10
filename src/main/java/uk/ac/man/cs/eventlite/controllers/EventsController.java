package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.bind.annotation.DeleteMapping;

import jakarta.validation.Valid;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.mastodon.MastodonService;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;
	
	@Autowired
	private MastodonService mastodonService;
	

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "events/not_found";
	}

	@GetMapping("/{id}")
	public String getEvent(@PathVariable("id") long id, Model model) {
		model.addAttribute("event", eventService.findOne(id).orElseThrow(() -> new EventNotFoundException(id)));    // set 'event'
	
		return "events/event";    // redirect to 'event' page to display details 
	}

	@GetMapping
	public String getAllEvents(Model model) {
		model.addAttribute("events", eventService.findAll());

		model.addAttribute("pastEvents", eventService.findAllByDateLessThan(LocalDate.now()));
		model.addAttribute("futureEvents", eventService.findAllByDateGreaterThan(LocalDate.now()));
		model.addAttribute("socialFeedPosts", mastodonService.lastThreePosts());
		return "events/index";
		
	}

	@GetMapping("/update/{id}")
	public String update(Model model, @PathVariable("id") long id) {
		if(!eventService.existsById(id)) { 
			throw new EventNotFoundException(id);
		}
		
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", eventService.findOne(id).get());
		}
		
		if(!model.containsAttribute("venues")) {
			model.addAttribute("venues", venueService.findAll());
		}
		
		return "events/update";
	}
	
	
	@PutMapping(path = "/updateEvent/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String updateEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors, @PathVariable("id") long id,
			Model model, RedirectAttributes redirectAttrs) {
		
		if(!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		
		if(errors.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			return "events/update";
		}
		
		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "Event successfully updated.");
		
		return "redirect:/events";
	}
	
	@DeleteMapping("/{id}")
	public String deleteEvent(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		if (!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}

		eventService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Event successfully deleted.");
		return "redirect:/events";
	}

	@GetMapping("/new")
	public String createEvent(Model model) {
		if(!model.containsAttribute("event")) {
			model.addAttribute("event",new Event());
		}
		if(!model.containsAttribute("venues")) {
			model.addAttribute("venues", venueService.findAll());
		}
		return "events/new";
	}
	
	
	@PostMapping("/{id}/mastodonPost")
	public String mastodonPost(@PathVariable("id") long id, @RequestParam("content") String content, RedirectAttributes redirectAttrs) {
		
		boolean successful = mastodonService.makePost(content);
		if(successful) 
			redirectAttrs.addFlashAttribute("ok_message", "Your Post: '"+content+"' was posted.");	
		return "redirect:/events/" + id;
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String newEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors, Model model, RedirectAttributes redirectAttrs) {
		
		if(errors.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			return "events/new";
		}
		
		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "Event successfully added.");
		
		return "redirect:/events";
	}
}
