package org.celllife.idart.events;

import java.util.HashSet;
import java.util.Set;

import org.celllife.idart.integration.eKapa.EkapaEventListener;

import com.adamtaft.eb.EventBusService;

public class EventManager {

	private final Set<Object> participants = new HashSet<Object>();

	public void register() {
		participants.add(new EkapaEventListener());
		for (Object participant : participants) {
			EventBusService.subscribe(participant);
		}
	}

	public void deRegister() {
		for (int i = 0; i < 3 && EventBusService.hasPendingEvents(); i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore and re-check
			}
		}
		for (Object participant : participants) {
			EventBusService.unsubscribe(participant);
		}
	}

}
