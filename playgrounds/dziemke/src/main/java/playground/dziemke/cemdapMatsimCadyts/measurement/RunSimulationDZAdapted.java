package playground.dziemke.cemdapMatsimCadyts.measurement;

import java.util.EnumMap;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;

public class RunSimulationDZAdapted {
	private static final Logger log = Logger.getLogger(RunSimulationDZAdapted.class ) ;

	enum HistogramBin {
		B89000, B89200, B89400, B89600, B89800, B90000, B90200, B90400, B90600, B90800, B91000;
	}	


	public static void main(String[] args) {
		String inputNetworkFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/network_diff_lengths2-inv.xml";
//		String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000.xml";
		String inputPlansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans1000_routes5.xml";
		String countsFileName = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/counts100-200_full.xml";
		String runId = "selectR+hist1000-nwInv";
		String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/" + runId + "";
		
		// Sigma for the randomized router; the higher the more randomness; 0.0 results in no randomness.)
//		final double sigma = 10.0;

		final double cadytsWeightLinks = 0.;
		final double cadytsWeightHistogram = 1000.;

		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(100);
		config.controler().setWritePlansInterval(10);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.counts().setCountsFileName(countsFileName);
		// new new
//		config.plans().setInputFile(inputPlansFile);
//		config.network().setInputFile(inputNetworkFile);
		// end new new
		
		// new new
		// Set monetary distance rate
//		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(-0.0002);
		//
//		config.planCalcScore().setPerforming_utils_hr(0.);
//		config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.);
		// end new new
		
		log.info("----- Car: MarginalUtilityOfTraveling = " + config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
		log.info("----- Performing_utils = " + config.planCalcScore().getPerforming_utils_hr());
		log.info("----- Car: MonetaryDistanceRate = " + config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate());
		log.info("----- MarginalUtilityOfMoney = " + config.planCalcScore().getMarginalUtilityOfMoney());
		log.info("----- BrainExpBeta = " + config.planCalcScore().getBrainExpBeta());
		
		{
            StrategySettings stratSets = new StrategySettings();
            stratSets.setStrategyName("ChangeExpBeta");
//            stratSets.setStrategyName("SelectRandom");
            stratSets.setWeight(0.8);
            config.strategy().addStrategySettings(stratSets);
        }
//		{
//            StrategySettings stratSets = new StrategySettings();
//            stratSets.setStrategyName("ReRoute");
//            stratSets.setWeight(0.2);
//            stratSets.setDisableAfter(70);
//            config.strategy().addStrategySettings(stratSets);
//        }


		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);
		new PopulationReader(scenario).readFile(inputPlansFile);


		final Counts<Link> someCounts = new Counts<>();
		new CountsReaderMatsimV1(someCounts).parse(countsFileName);

		scenario.addScenarioElement("calibrationCounts", someCounts);


		Controler controler = new Controler(scenario);
		
		// new -- Randomizing router
//		final RandomizingTimeDistanceTravelDisutilityFactory builder =
//				new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() );
//		builder.setSigma(sigma);
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindCarTravelDisutilityFactory().toInstance(builder);
//			}
//		});
		// end new 
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CadytsModule());
				install(new PersoDistHistoModule());

			}
		});

		// Add StartUpListener
		controler.addControlerListener((StartupListener) startupEvent -> {
			AnalyticalCalibrator<HistogramBin> calibrator = new AnalyticalCalibrator<>(startupEvent.getServices().getConfig().controler().getOutputDirectory() + "/cadyts-histogram.txt", MatsimRandom.getRandom().nextLong(), 24*60*60);
			calibrator.setStatisticsFile(startupEvent.getServices().getControlerIO().getOutputFilename("histogram-calibration-stats.txt"));
			calibrator.addMeasurement(HistogramBin.B89000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B89200, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B89400, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B89600, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B89800, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B90000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B90200, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B90400, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B90600, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B90800, 0, 24*60*60, 333, SingleLinkMeasurement.TYPE.COUNT_VEH);
			calibrator.addMeasurement(HistogramBin.B91000, 0, 24*60*60, 0, SingleLinkMeasurement.TYPE.COUNT_VEH);

			// Add BeforeMobsimListener
			startupEvent.getServices().addControlerListener((BeforeMobsimListener) beforeMobsimEvent -> {
				for (Person person : beforeMobsimEvent.getServices().getScenario().getPopulation().getPersons().values()) {
					PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
					double totalPlannedDistance = 0.0;
					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
						if (planElement instanceof Leg) {
							totalPlannedDistance += ((Leg) planElement).getRoute().getDistance();
						}
					}
//					HistogramBin bin = HistogramBin.values()[(int) (Math.min(totalPlannedDistance, 260000) / 20000.0)];
					HistogramBin bin = HistogramBin.values()[(int) ((Math.min(totalPlannedDistance, 91000) - 89000) / 200)];
					planBuilder.addTurn(bin, 0);
					calibrator.addToDemand(planBuilder.getResult());
				}
			});

			// Add AfterMobsimListener
			startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
				PersoDistHistogram distService = afterMobsimEvent.getServices().getInjector().getInstance(PersoDistHistogram.class);
				EnumMap<HistogramBin, Integer> frequencies = new EnumMap<>(HistogramBin.class);
				for (HistogramBin bin : HistogramBin.values()) {
					frequencies.put(bin, 0);
				}
				HashMap<Id<Person>, Double> distances = distService.getDistances();
				distances.values().forEach(v -> {
//					HistogramBin bin = HistogramBin.values()[(int) (Math.min(v, 260000) / 20000.0)];
					HistogramBin bin = HistogramBin.values()[(int) ((Math.min(v, 91000) - 89000)/ 200)];
					frequencies.put(bin, frequencies.get(bin) + 1);
				});
				calibrator.afterNetworkLoading(new SimResults<HistogramBin>() {
					@Override
					public double getSimValue(HistogramBin histogramBin, int startTime_s, int endTime_s, SingleLinkMeasurement.TYPE type) {
						return frequencies.get(histogramBin);
					}
				});
				distances.forEach((personId, v) -> {
					PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
//					planBuilder.addTurn(HistogramBin.values()[(int) (Math.min(v, 260000) / 20000.0)], 0);
					planBuilder.addTurn(HistogramBin.values()[(int) ((Math.min(v, 91000) - 89000) / 200)], 0);
					double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
//					System.out.println("########## Offset = " + offset + " -- personId = " + personId + " -- v = " + v);
					afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, cadytsWeightHistogram * offset));
				});
			});


		});


		CadytsAndCloneScoringFunctionFactory factory = new CadytsAndCloneScoringFunctionFactory();
		factory.setCadytsweight(cadytsWeightLinks);
		controler.setScoringFunctionFactory(factory);
		controler.run();
	}
}
