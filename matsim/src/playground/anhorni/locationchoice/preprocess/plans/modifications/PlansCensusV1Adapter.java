package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.locationchoice.utils.QuadTreeRing;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;

public class PlansCensusV1Adapter {
	
	private QuadTreeRing<ActivityFacilityImpl> shopTree;
	private QuadTreeRing<ActivityFacilityImpl> leisureTree;
	private QuadTreeRing<ActivityFacilityImpl> homeTree;
	private QuadTreeRing<ActivityFacilityImpl> educationTree;
	private QuadTreeRing<ActivityFacilityImpl> workTree;
	
	private void init(ActivityFacilitiesImpl facilities) {
		FacilityQuadTreeBuilder builder = new FacilityQuadTreeBuilder();
		leisureTree = builder.builFacQuadTree(
				"leisure", (TreeMap<Id, ActivityFacilityImpl>) facilities.getFacilitiesForActivityType("leisure"));
		
		shopTree = builder.builFacQuadTree(
				"shop", (TreeMap<Id, ActivityFacilityImpl>) facilities.getFacilitiesForActivityType("shop"));
		
		homeTree = builder.builFacQuadTree(
				"home", (TreeMap<Id, ActivityFacilityImpl>) facilities.getFacilitiesForActivityType("home"));
		
		
		TreeMap<Id, ActivityFacilityImpl> educationTreeMap = 
			(TreeMap<Id, ActivityFacilityImpl>)facilities.getFacilitiesForActivityType("education_kindergarten");
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_primary"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_secondary"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_higher"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_other"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education"));
		educationTree = builder.builFacQuadTree(
				"education", (TreeMap<Id, ActivityFacilityImpl>) educationTreeMap);
		
		
		TreeMap<Id, ActivityFacilityImpl> workTreeMap = 
			(TreeMap<Id, ActivityFacilityImpl>)facilities.getFacilitiesForActivityType("work_sector2");
		workTreeMap.putAll(facilities.getFacilitiesForActivityType("work_sector3"));
		workTreeMap.putAll(facilities.getFacilitiesForActivityType("work"));
	
		workTree = builder.builFacQuadTree(
				"work", workTreeMap);
	}
	
	
	public void adapt(PopulationImpl plans, ActivityFacilitiesImpl facilities) {
		
		this.init(facilities);
				
		Iterator<PersonImpl> person_it = plans.getPersons().values().iterator();
		while (person_it.hasNext()) {
			PersonImpl person = person_it.next();
			Plan plan = person.getSelectedPlan();
			
			person.createDesires("adapted desire");
			
			List<? extends PlanElement> actslegs = plan.getPlanElements();			
			for (int j = 0; j < actslegs.size(); j=j+2) {			
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				this.adaptActivity(act, person);
			}
		}
	}
	
	private void adaptActivity(ActivityImpl act, PersonImpl person) {
		
		double desiredDuration = 0.0;
		if (act.getType().equals("tta")) {
			desiredDuration = 8.0 * 3600.0;
		}
		else {
			desiredDuration = 3600 * Double.parseDouble(act.getType().substring(1));
		}
		String fullType = "tta";
					
		if (act.getType().startsWith("h")) {
			fullType = "home";
			act.setFacility(this.homeTree.get(act.getCoord().getX(), act.getCoord().getY()));
		}
		else if (act.getType().startsWith("e")){
			fullType = "education";
			act.setFacility(this.educationTree.get(act.getCoord().getX(), act.getCoord().getY()));
		}
		else if (act.getType().startsWith("s")){
			fullType = "shop";
			act.setFacility(this.shopTree.get(act.getCoord().getX(), act.getCoord().getY()));
		}
		else if (act.getType().startsWith("l")){
			fullType = "leisure";
			act.setFacility(this.leisureTree.get(act.getCoord().getX(), act.getCoord().getY()));
		}	
		else if (act.getType().startsWith("w")){
			fullType = "work";
			act.setFacility(this.workTree.get(act.getCoord().getX(), act.getCoord().getY()));
		}
		if (person.getDesires().getActivityDuration(fullType) <= 0.0) {
			person.getDesires().putActivityDuration(fullType, desiredDuration);
		}
		act.setType(fullType);
	}
}
