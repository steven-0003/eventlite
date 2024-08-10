package uk.ac.man.cs.eventlite.entities;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import org.thymeleaf.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "events")
@DynamicUpdate
public class Event {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long id;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Future
	@NotNull
	private LocalDate date;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime time;

	@NotBlank
	@Size(max = 256, message = "The name must have 256 characters or less.")
	private String name;

	@Size(max = 500, message = "The description must have 500 characters or less.")
	private String description;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "venue_id")
	@NotNull
	@JsonInclude(JsonInclude.Include.NON_NULL) 
	private Venue venue;

	public Event() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return this.description;
	}

	public Venue getVenue() {
		return venue;
	}

	public void setVenue(Venue venue) {
		this.venue = venue;
	}
	
	public String dateToString() {
		int n = date.getDayOfMonth();
		String ordinal = "";
		if (n >= 11 && n <= 13) {
	        ordinal = "th";
	    }else if(n%10 == 1) {
	    	ordinal = "st";
	    }else if(n%10 == 2) {
	    	ordinal = "nd";
	    }else if(n%10 == 3) {
	    	ordinal = "rd";
	    }else {
	    	ordinal = "th";
	    }
	    
	    String month = StringUtils.capitalize(date.getMonth().toString().toLowerCase());
	    
	    return String.valueOf(n) + ordinal + " " + month + " " + date.getYear();
	}
	
	public boolean searchEvent(String name) {
		return this.name.toLowerCase().matches(".*\\b"+name.toLowerCase()+"\\b.*");
	}
}
