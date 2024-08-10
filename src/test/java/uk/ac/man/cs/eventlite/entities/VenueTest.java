package uk.ac.man.cs.eventlite.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VenueTest {
	private Venue one;
	
	@BeforeEach
	public void setUp() {
		one = new Venue();
		one.setId(1);
		one.setCapacity(100);
		one.setName("Kilburn");
		one.setRoadName("Oxford Road");
		one.setPostCode("M13 9PL");
	}
	
	@Test
	public void setLngLat() throws Exception{
		assertNull(one.getLatitude());
		assertNull(one.getLongitude());
		one.setLngLat();
		assertNotNull(one.getLatitude());
		assertNotNull(one.getLongitude());
	}
	
	@Test
	public void setLngLatEmptyAddress() throws Exception{
		Venue two = new Venue();
		assertNull(two.getLatitude());
		assertNull(two.getLongitude());
		two.setPostCode("");
		two.setRoadName("");
		two.setLngLat();
		assertNull(two.getLatitude());
		assertNull(two.getLongitude());
	}
	
	@Test
	public void searchVenue() throws Exception {
		assertThat(one.searchVenue("Kilburn"), is(true));
		assertThat(one.searchVenue("Kil"), is(false));
	}
	
	@Test
	public void getId() throws Exception{
		assertThat(one.getId(), equalTo(1L));
	}
	
	@Test
	public void getCapacity() throws Exception {
		assertThat(one.getCapacity(), equalTo(100));
	}
	
	@Test
	public void getName() throws Exception {
		assertThat(one.getName(), equalTo("Kilburn"));
	}
	
	@Test
	public void getRoadName() throws Exception {
		assertThat(one.getRoadName(), equalTo("Oxford Road"));
	}
	
	@Test
	public void getPostCode() throws Exception {
		assertThat(one.getPostCode(), equalTo("M13 9PL"));
	}
	
	@Test
	public void getEvents() throws Exception {
		assertNull(one.getEvents());
	}

}
