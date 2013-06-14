/* *********************************************************************** *
 * project: org.matsim.*
 * DgFigure9ToKoehlerStrehler2010ModelConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signalsystems.data.SignalsData;
import org.xml.sax.SAXException;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010SimpleDemandConverter;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;


public class DgFigure9ToKoehlerStrehler2010ModelConverter {
	
	private static final Logger log = Logger
			.getLogger(DgFigure9ToKoehlerStrehler2010ModelConverter.class);
	
	/**
	 * @param args
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws TransformerConfigurationException 
	 */
	public static void main(String[] args) throws SAXException, TransformerConfigurationException, IOException {
		ScenarioImpl sc = new DgFigure9ScenarioGenerator().loadScenario();
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);

		DgMatsim2KoehlerStrehler2010NetworkConverter converter = new DgMatsim2KoehlerStrehler2010NetworkConverter(idConverter);
		log.warn("Check times of demand!");
		DgKSNetwork net = converter.convertNetworkLanesAndSignals(sc.getNetwork(), sc.getScenarioElement(LaneDefinitions20.class), 
				sc.getScenarioElement(SignalsData.class), 0.0, 3600.0);
		
		DgMatsim2KoehlerStrehler2010SimpleDemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010SimpleDemandConverter();
		DgCommodities coms = demandConverter.convert(sc, net);
		
		DgKoehlerStrehler2010ModelWriter writer = new DgKoehlerStrehler2010ModelWriter();
		writer.write(net, coms, "Figure9Scenario", "", DgPaths.STUDIESDG + "koehlerStrehler2010/cplex_scenario_population_800_agents.xml");

		log.warn("Id conversions are not written, yet!");
		
	}

}
