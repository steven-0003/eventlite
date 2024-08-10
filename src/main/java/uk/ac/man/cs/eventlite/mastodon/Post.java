package uk.ac.man.cs.eventlite.mastodon;

import java.time.LocalDate;
import java.time.LocalTime;

public class Post {
	private String uri;
	private String content;
	private LocalDate date;
	private LocalTime time;
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getUri() {
		return uri;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setDate(LocalDate date) {
		this.date = date;
	}
	
	public LocalDate getDate() {
		return date;
	}
	
	public void setTime(LocalTime time) {
		this.time = time;
	}
	
	public LocalTime getTime() {
		return time;
	}
}
