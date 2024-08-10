package uk.ac.man.cs.eventlite.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventTest {
	private Event one;
	
	@BeforeEach
	public void setUp() {
		one = new Event();
		Venue v = new Venue();
		one.setId(1);
		one.setName("Party");
		one.setDescription("This event is fun");
		one.setDate(LocalDate.of(3004, 8, 1));
		one.setTime(LocalTime.of(0, 0));
		one.setVenue(v);
	}
	
	@Test
	public void dateToString() throws Exception {
		assertThat(one.dateToString(), equalTo("1st August 3004"));
		
		one.setDate(LocalDate.of(3004, 8, 2));
		assertThat(one.dateToString(), equalTo("2nd August 3004"));
		
		one.setDate(LocalDate.of(3004, 8, 3));
		assertThat(one.dateToString(), equalTo("3rd August 3004"));
		
		one.setDate(LocalDate.of(3004, 8, 4));
		assertThat(one.dateToString(), equalTo("4th August 3004"));
		
		one.setDate(LocalDate.of(3004, 8, 11));
		assertThat(one.dateToString(), equalTo("11th August 3004"));
	}
	
	@Test
	public void searchEvent() throws Exception {
		assertThat(one.searchEvent("Party"), is(true));
		assertThat(one.searchEvent("Par"), is(false));
	}
	
	@Test
	public void getId() throws Exception {
		assertThat(one.getId(), equalTo(1L));
	}
	
	@Test
	public void getName() throws Exception {
		assertThat(one.getName(), equalTo("Party"));
	}
	
	@Test
	public void getDescription() throws Exception {
		assertThat(one.getDescription(), equalTo("This event is fun"));
	}
	
	@Test
	public void getDate() throws Exception {
		assertThat(one.getDate(), equalTo(LocalDate.of(3004, 8, 1)));
	}
	
	@Test
	public void getTime() throws Exception {
		assertThat(one.getTime(), equalTo(LocalTime.of(0, 0)));
	}
	
	@Test
	public void getVenue() throws Exception {
		assertThat(one.getVenue().getClass(), equalTo(Venue.class));
	}
}
