package org.celllife.idart.events;

import java.util.Set;

import org.celllife.idart.database.hibernate.PillCount;

public class AdherenceEvent {

	private final Set<PillCount> pillCounts;

	public AdherenceEvent(Set<PillCount> pillCounts) {
		this.pillCounts = pillCounts;
	}

	public Set<PillCount> getPillCounts() {
		return pillCounts;
	}

}
