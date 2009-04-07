package playground.andreas.bln;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;
import org.xml.sax.SAXException;

public class OSM2MATSim {

	// TODO [an] keep attributes like cycleway and pedestrian in mind, but block access to motorways for those users
	
	public static void main(final String[] args) {

		NetworkLayer network = new NetworkLayer();
//		OsmNetworkReader osmReader = new OsmNetworkReader(network, new WGS84toCH1903LV03());
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						TransformationFactory.DHDN_GK4), false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		
		String inputFile = "z:/osm_net/20090316_berlinbrandenburg.fused.gz";
		String outputFile = "z:/osm_net/_test3";
		
		// Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2, 80.0/3.6, 0.5, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  60.0/3.6, 0.5, 1500, true);
		// Pseudoautobahn
		osmReader.setHighwayDefaults(2, "trunk",         2,  50.0/3.6, 0.5, 2000);
		osmReader.setHighwayDefaults(2, "trunk_link",    1,  40.0/3.6, 0.5, 1500);
		// Durchgangsstrassen
		osmReader.setHighwayDefaults(3, "primary",       1,  35.0/3.6, 0.5, 1500);
		osmReader.setHighwayDefaults(3, "primary_link",  1,  30.0/3.6, 0.5, 1500);
		
		// Hauptstrassen
		osmReader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 0.5, 1000);
		// Weitere Hauptstrassen
		osmReader.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 0.5,  600); // ca wip
		
		// Nebenstrassen
		osmReader.setHighwayDefaults(6, "minor",         1,  25.0/3.6, 1.0,  600); // nix
		// Alles Moegliche, vor allem Nebenstrassen auf dem Land, meist keine 30er Zone 
		osmReader.setHighwayDefaults(6, "unclassified",  1,  25.0/3.6, 1.0,  600);
		// Nebenstrassen, meist 30er Zone
		osmReader.setHighwayDefaults(6, "residential",   1,  20.0/3.6, 1.0,  600);
		// Spielstrassen
		osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
		
		// Fahrrad
		osmReader.setHighwayDefaults(7, "cycleway", 1,  10.0/3.6, 1.0,  300);
		// Fussgaenger
		osmReader.setHighwayDefaults(8, "pedestrian", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "footway", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "service", 1,  3.0/3.6, 1.0,  300);
		osmReader.setHighwayDefaults(8, "steps", 1,  3.0/3.6, 1.0,  300);

		
		osmReader.setHierarchyLayer(52.742845, 12.905454, 52.206321, 13.414334, 2);
		osmReader.setHierarchyLayer(52.408424, 13.001725, 52.393787, 13.070721, 8);
		osmReader.setHierarchyLayer(52.410267, 13.028828, 52.379898, 13.086545, 5);
		
		osmReader.setHierarchyLayer(52.642299, 13.304882, 52.527397, 13.805398, 5);
		osmReader.setHierarchyLayer(52.537028, 13.410000, 52.520000, 13.443527, 8);
		
		
		
		try {
			osmReader.parse(inputFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new NetworkWriter(network, outputFile + ".xml.gz").write();
		new NetworkCleaner().run(new String[] {outputFile + ".xml.gz", outputFile + "_cl.xml.gz"});

	}

}
