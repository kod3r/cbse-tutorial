package example1;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import tutorial.environment.MATSimDataProviderReceiver;
import tutorial.matsim.PopulationAgentSource;
import cz.cuni.mff.d3s.deeco.annotations.processor.AnnotationProcessor;
import cz.cuni.mff.d3s.deeco.annotations.processor.AnnotationProcessorException;
import cz.cuni.mff.d3s.deeco.logging.Log;
import cz.cuni.mff.d3s.deeco.model.runtime.api.RuntimeMetadata;
import cz.cuni.mff.d3s.deeco.model.runtime.custom.RuntimeMetadataFactoryExt;
import cz.cuni.mff.d3s.deeco.network.NetworkProvider;
import cz.cuni.mff.d3s.deeco.runtime.RuntimeFramework;
import cz.cuni.mff.d3s.deeco.simulation.DirectConnectionsProvider;
import cz.cuni.mff.d3s.deeco.simulation.SimulationHost;
import cz.cuni.mff.d3s.deeco.simulation.SimulationRuntimeBuilder;
import cz.cuni.mff.d3s.deeco.simulation.matsim.JDEECoAgent;
import cz.cuni.mff.d3s.deeco.simulation.matsim.JDEECoAgentSource;
import cz.cuni.mff.d3s.deeco.simulation.matsim.MATSimRouter;
import cz.cuni.mff.d3s.deeco.simulation.matsim.MATSimSimulation;

/**
 * jDEECo-MATSim integration demo based on the Sioux Falls scenario of the
 * MATSim.
 * 
 * @author Michal Kit <kit@d3s.mff.cuni.cz>
 * 
 */
public class Main {

	private static final String MATSIM_CONFIG_CUSTOM = "input/config.xml";
	
	private static JDEECoAgentSource jdeecoAgentSource;
	private static MATSimSimulation simulation;
	private static MATSimRouter router;
	private static MATSimDataProviderReceiver matSimProviderReceiver;
	
	private static AnnotationProcessor processor;
	private static SimulationRuntimeBuilder builder;

	public static void main(String[] args) throws AnnotationProcessorException,	IOException {
		MATSimRouter.DEFAULT_LINK_PARKING_CAPACITY = 2;
		
		Log.i("Preparing simulation");

		jdeecoAgentSource = new JDEECoAgentSource();
		PopulationAgentSource populationAgentSource = new PopulationAgentSource();
		
		NetworkProvider np = new DirectConnectionsProvider();
		
		matSimProviderReceiver = new MATSimDataProviderReceiver();
		simulation = new MATSimSimulation(np, matSimProviderReceiver, matSimProviderReceiver, Arrays.asList(jdeecoAgentSource, populationAgentSource), MATSIM_CONFIG_CUSTOM);
		populationAgentSource.setPopulation(simulation.getControler().getPopulation());
		
		router = new MATSimRouter(simulation.getControler(), simulation.getTravelTime());

		Log.i("Creating components");

		processor = new AnnotationProcessor(RuntimeMetadataFactoryExt.eINSTANCE);
		builder = new SimulationRuntimeBuilder();

//		createAndDeployVehicleComponent(1, "1_1");
//		createAndDeployVehicleComponent(2, "50_2");
		createAndDeployVehicleComponent(3, "22_3");
//		createAndDeployVehicleComponent(4, "59_3");
		
		simulation.run();
		Log.i("Simulation Finished");

	}
	
	private static void createAndDeployVehicleComponent(int idx, String sourceLinkIdString) throws AnnotationProcessorException {
		String compIdString = "V" + idx;
		Id compId = new IdImpl(compIdString);
		Id sourceLinkId = new IdImpl(sourceLinkIdString);

		jdeecoAgentSource.addAgent(new JDEECoAgent(compId, sourceLinkId));

		VehicleComponent component = new VehicleComponent(compIdString, 
				matSimProviderReceiver.getActuatorProvider(compId), matSimProviderReceiver.getSensorProvider(compId), router, simulation);
		
		RuntimeMetadata model = RuntimeMetadataFactoryExt.eINSTANCE.createRuntimeMetadata();
		processor.process(model, component);
		
		SimulationHost host = simulation.getHost(compIdString, "node[" + idx + "]", false, false);
		RuntimeFramework runtime = builder.build(host, simulation, model, null, null);
		runtime.start();		
	}


}