package org.celllife.idart.events;

import org.celllife.idart.database.hibernate.Packages;

public class PackageEvent {

	public enum Type {
		RETURN, PICKUP_ONLY, PACKAGE_AND_PICKUP, PACKAGE_FOR_LATER
	}

	private final Type type;
	private final Packages pack;

	public PackageEvent(Type type, Packages pack) {
		this.type = type;
		this.pack = pack;
	}

	public Type getType() {
		return type;
	}

	public Packages getPack() {
		return pack;
	}

}
