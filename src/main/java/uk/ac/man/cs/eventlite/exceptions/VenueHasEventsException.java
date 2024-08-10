package uk.ac.man.cs.eventlite.exceptions;

public class VenueHasEventsException extends RuntimeException {

	private static final long serialVersionUID = 5016812401135889608L;

	private long id;

	public VenueHasEventsException(long id) {
		super("Could not delete venue " + id + " because it has one or more events.");

		this.id = id;
	}

	public long getId() {
		return id;
	}
}
