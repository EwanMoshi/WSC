package ECJ;

import ec.EvolutionState;
import ec.gp.GPInitializer;
import ec.util.Parameter;

public class WSCInitializer extends GPInitializer {

	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	
	
	public static double minAvailability = 0.0;
	public static double maxAvailability = -1.0;
	public static double minReliability = 0.0;
	public static double maxReliability = -1.0;
	public static double minTime = Double.MAX_VALUE;
	public static double maxTime = -1.0;
	public static double minCost = Double.MAX_VALUE;
	public static double maxCost = -1.0;
	public double w1;
	public double w2;
	public double w3;
	public double w4;

	public GraphRandom random;
	
	@Override
	public void setup(EvolutionState state, Parameter base) {
		random = new GraphRandom(state.random[0]);
		super.setup(state, base);
		
		Parameter weight1Param = new Parameter("fitness-weight1");
		Parameter weight2Param = new Parameter("fitness-weight2");
		Parameter weight3Param = new Parameter("fitness-weight3");
		Parameter weight4Param = new Parameter("fitness-weight4");

		w1 = state.parameters.getDouble(weight1Param, null);
		w2 = state.parameters.getDouble(weight2Param, null);
		w3 = state.parameters.getDouble(weight3Param, null);
		w4 = state.parameters.getDouble(weight4Param, null);
		
	}
}
