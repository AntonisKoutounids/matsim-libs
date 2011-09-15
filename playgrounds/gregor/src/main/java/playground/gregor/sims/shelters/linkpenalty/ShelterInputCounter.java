/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterInputCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.gregor.sims.shelters.linkpenalty;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.network.NetworkImpl;


public class ShelterInputCounter implements LinkLeaveEventHandler, BeforeMobsimListener {

	private final HashMap<Id,Counter> counts = new HashMap<Id, Counter>();
	private final HashMap<Id, Building> shelterLinkMapping;
	private final HashMap<Id,LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	
	private EventsManager events ;

	public ShelterInputCounter(NetworkImpl network, HashMap<Id,Building> shelterLinkMapping, EventsManager events) {

		this.shelterLinkMapping = shelterLinkMapping;
		this.events = events ;

		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().contains("sl") && link.getId().toString().contains("a")) {
				this.counts.put(link.getId(), new Counter());
			}
		}
	}

	public void handleEvent(LinkLeaveEvent event) {
		Counter c = this.counts.get(event.getLinkId());
		if (c != null) {
			c.count++;
			Building b = this.shelterLinkMapping.get(event.getLinkId());
			c.count++;
			if (c.count > b.getShelterSpace()) {
//				AgentMoneyEvent e = new AgentMoneyEvent(event.getTime(),event.getPersonId(),-(c.count - b.getShelterSpace())/10.);
				AgentMoneyEventImpl e = new AgentMoneyEventImpl(event.getTime(),event.getPersonId(),-12*6);
//				QueueSimulation.getEvents().processEvent(e);
				events.processEvent(e);
				LinkInfo li = this.linkInfos.get(event.getLinkId());
				if (li == null) {
					li = new LinkInfo();
					this.linkInfos.put(event.getLinkId(), li);
					li.penalties.put(0.0, new LinkPenalty());
				}

				LinkPenalty p = new LinkPenalty();
				li.penalties.put(event.getTime(), p);
				p.penalty = 12*3600;

			}
		}
	}


	public void reset(int iteration) {
		int c = 0;
		double o = 0;
		for (Entry<Id, Counter> e : this.counts.entrySet()) {
			int count = e.getValue().count;
			int max = this.shelterLinkMapping.get(e.getKey()).getShelterSpace();
			double overload = (double)count / max;
			System.out.println("Link:" + e.getKey() + "  count:" + count + " should be at most:" + max + " overload:" + overload);
			e.getValue().count = 0;//		if (c.count <= 1.5 * b.getShelterSpace()) {
//			return MatsimRandom.getRandom().nextDouble() < 0.25;
//			}
	//
//			if (c.count <= 2 * b.getShelterSpace()) {
//				return MatsimRandom.getRandom().nextDouble() < 0.1;
//			}
//			if (c.count <= 3 * b.getShelterSpace()) {
//				return MatsimRandom.getRandom().nextDouble() < 0.01;
//			}
			c++;
			o+=overload;
		}
		System.err.println("avgOverload:" + o/c);
	}



	private static class Counter {
		int count = 0;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.linkInfos.clear();

	}

	public double getLinkPenalty(Link link, double time) {
		LinkInfo li = this.linkInfos.get(link.getId());
		if (li == null) {
			return 0.;
		}
		Entry<Double,LinkPenalty> e = li.penalties.floorEntry(time);
		return e.getValue().penalty;
	}
	private static class LinkInfo {
		TreeMap<Double,LinkPenalty> penalties = new TreeMap<Double, LinkPenalty>();
	}

	private static class LinkPenalty {
		double penalty = 0;
	}
}
