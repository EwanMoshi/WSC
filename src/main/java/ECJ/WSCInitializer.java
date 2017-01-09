package ECJ;

import ec.EvolutionState;
import ec.gp.GPInitializer;
import ec.util.Parameter;

public class WSCInitializer extends GPInitializer {

	public static final int TIME = 0;
	public static final int COST = 1;
	public static final int AVAILABILITY = 2;
	public static final int RELIABILITY = 3;

	public final double minAvailability = 0.0;
	public double maxAvailability = -1.0;
	public final double minReliability = 0.0;
	public double maxReliability = -1.0;
	public double minTime = Double.MAX_VALUE;
	public double maxTime = -1.0;
	public double minCost = Double.MAX_VALUE;
	public double maxCost = -1.0;
	public double w1;
	public double w2;
	public double w3;
	public double w4;

	public GraphRandom random;
	
	@Override
	public void setup(EvolutionState state, Parameter base) {
		random = new GraphRandom(state.random[0]);
		super.setup(state, base);
		
		
	}
}
