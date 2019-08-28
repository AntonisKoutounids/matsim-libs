package org.matsim.contrib.freight.carrier;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.*;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.xml.sax.Attributes;

/**
 * Reader reading carrierVehicleTypes from an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeReader extends MatsimXmlParser {
	
	private static Logger logger = Logger.getLogger(CarrierVehicleTypeReader.class);
	
	private CarrierVehicleTypes carrierVehicleTypes;

	private Id<org.matsim.vehicles.VehicleType> currentTypeId;

	private String currentDescription;

	

//	private Integer currentCap;

	private CostInformation currentVehicleCosts;

	private EngineInformation currentEngineInfo;

	private String currentCapacity;
	
	private String maxVelo;

	public CarrierVehicleTypeReader(CarrierVehicleTypes carrierVehicleTypes) {
		super();
		this.carrierVehicleTypes = carrierVehicleTypes;
		this.setValidating(false);
	}
	
//	/**
//	 * Reads types from xml-file.
//	 * 
//	 * @param xml-filename containing vehicleTypes
//	 */
//	/* This is somewhat problematic for me (JWJoubert, Nov '13). The MatsimXmlParser
//	 * has a parse method, yet when calling it, it results in an XML error. Maybe 
//	 * it would be better to 
//	 * a) use a dtd file, and
//	 * b) rather use the infrastructure provided by the MatsimXmlParser, and 
//	 *    override it if required.
//	 */
//	public void read(String filename){
//		logger.info("read vehicle types");
//		this.setValidating(false);
//		read(filename);
//		logger.info("done");
//	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals("vehicleType")){
			this.currentTypeId = Id.create(atts.getValue("id"), org.matsim.vehicles.VehicleType.class );
		}
		if(name.equals("allowableWeight")){
			String weight = atts.getValue("weight");
			parseDouble(weight);
		}
		if(name.equals("engineInformation")){
			String fuelType = atts.getValue("fuelType");
			String gasConsumption = atts.getValue("gasConsumption");
			EngineInformation engineInfo = new EngineInformation(parseFuelType(fuelType ), parseDouble(gasConsumption ));
			this.currentEngineInfo = engineInfo;
		}
		
		if(name.equals("costInformation")){
			String fix = atts.getValue("fix");
			// yyyyyy shouldn't this be "perDay"??? kai, aug'19
			String perMeter = atts.getValue("perMeter");
			String perSecond = atts.getValue("perSecond");
			if(fix == null || perMeter == null || perSecond == null) throw new IllegalStateException("cannot read costInformation correctly. probably the paramName was written wrongly");
			CostInformation vehicleCosts = new CostInformation(parseDouble(fix ), parseDouble(perMeter ), parseDouble(perSecond ));
			this.currentVehicleCosts = vehicleCosts;
		}
	}

	private FuelType parseFuelType(String fuelType) {
		if(fuelType.equals(FuelType.diesel.toString())){
			return FuelType.diesel;
		}
		else if(fuelType.equals(FuelType.electricity.toString())){
			return FuelType.electricity;
		}
		else if(fuelType.equals(FuelType.gasoline.toString())){
			return FuelType.gasoline;
		}
		throw new IllegalStateException("fuelType " + fuelType + " is not supported");
	}

	private double parseDouble(String weight) {
		return Double.parseDouble(weight);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("description")){
			this.currentDescription = content;
		}
		if(name.equals("capacity")){
			this.currentCapacity = content;
		}
		if(name.equals("maxVelocity")){
			this.maxVelo = content;
		}
		if(name.equals("vehicleType")){
			CarrierUtils.CarrierVehicleTypeBuilder typeBuilder = CarrierUtils.CarrierVehicleTypeBuilder.newInstance(currentTypeId );
			if(currentDescription != null) typeBuilder.setDescription(currentDescription);
//			if(currentWeight != null) vehType.setAllowableTotalWeight(currentWeight);
//			if(currentCap != null) vehType.setFreightCapacity(currentCap);
			if(currentVehicleCosts != null) typeBuilder.setVehicleCostInformation(currentVehicleCosts);
			if(currentEngineInfo != null) typeBuilder.setEngineInformation(currentEngineInfo);
			if(currentCapacity != null) typeBuilder.setCapacityWeightInTons(Double.parseDouble(currentCapacity ) );
			if(maxVelo != null) typeBuilder.setMaxVelocity(Double.parseDouble(maxVelo));
			VehicleType vehType = typeBuilder.build();
			carrierVehicleTypes.getVehicleTypes().put(vehType.getId(), vehType);
			reset();
		}
		
	}

	private void reset() {
		currentTypeId = null;
		currentDescription = null;
		currentVehicleCosts = null;
		currentEngineInfo = null;
	}

}
