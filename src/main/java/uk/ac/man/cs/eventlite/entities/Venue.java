package uk.ac.man.cs.eventlite.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uk.ac.man.cs.eventlite.geocoder.GeoCoder;

@Entity
@Table(name = "venues")
public class Venue {

	@Id
	@GeneratedValue
	private long id;

	@NotBlank
	@Size(max = 256)
	private String name;

	@NotNull
	@Min(value = 1, message = "Capacity must be a positive integer")
	private int capacity;
	
	@NotBlank
	@Pattern(regexp = "^(([A-Z][A-HJ-Y]?\\d[A-Z\\d]?|ASCN|STHL|TDCU|BBND|[BFS]IQQ|PCRN|TKCA) ?\\d[A-Z]{2}|BFPO ?\\d{1,4}|(KY\\d|MSR|VG|AI)[ -]?\\d{4}|[A-Z]{2} ?\\d{2}|GE ?CX|GIR ?0A{2}|SAN ?TA1)$", message = "Invalid post code")
	private String postCode;
	
	@NotBlank
	@Size(max = 300)
	private String roadName;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "venue", cascade = CascadeType.ALL)
    private List<Event> eventList;
	
	private Double longitude;
	
	private Double latitude;

	public Venue() {
	}

	@JsonIgnore
	public long getId() {
		return id;
	}

	@JsonProperty
	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	@JsonIgnore
	public List<Event> getEvents(){
		return this.eventList;
	}
	
	@JsonIgnore
	public String getPostCode() {
		return this.postCode;
	}
	
	@JsonProperty
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}
	
	@JsonIgnore
	public String getRoadName() {
		return this.roadName;
	}
	
	@JsonProperty
	public void setRoadName(String road) {
		this.roadName = road;
	}
	
	public Double getLongitude() {
		return this.longitude;
	}
	
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	public Double getLatitude() {
		return this.latitude;
	}
	
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	public void setLngLat() {
		GeoCoder geoCoder = new GeoCoder();
		try {
			geoCoder.setLatLng(this.roadName + " " + this.postCode);
			Thread.sleep(1000L);
			setLongitude(geoCoder.getLongitude());
			setLatitude(this.latitude = geoCoder.getLatitude());
		}catch(Exception e) {}
	}
	
	public boolean searchVenue(String name) {
		return this.name.toLowerCase().matches(".*\\b"+name.toLowerCase()+"\\b.*");
	}
}
