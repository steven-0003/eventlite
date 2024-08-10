package uk.ac.man.cs.eventlite.geocoder;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoCoder {
	private MapboxGeocoding mapboxGeocoding;
	private String APIKEY = "mapbox-API";
	
	private final static Logger Log = LoggerFactory.getLogger(GeoCoder.class);
	
	private Double lat;
	private Double lng;
	
	public MapboxGeocoding mapboxBuilder(String query) {
		return MapboxGeocoding.builder()
				.accessToken(APIKEY)
				.query(query)
				.build();
	}
	
	
	public void setLatLng(String query) {
		mapboxGeocoding = mapboxBuilder(query);
		
		mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
			@Override
			public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

				List<CarmenFeature> results = response.body().features();

				if (results.size() > 0) {

				  // Log the first results Point.
				  Point firstResultPoint = results.get(0).center();
				  lat = firstResultPoint.latitude();
				  lng = firstResultPoint.longitude();
				  Log.info("onResponse: " + firstResultPoint.toString());

				} else {

				  // No result for your request were found.
				  Log.info("onResponse: No result found");

				}
			}

			@Override
			public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
				throwable.printStackTrace();
			}
		});
	}
	
	public Double getLatitude() {
		return this.lat;
	}
	
	public Double getLongitude() {
		return this.lng;
	}
	
}
