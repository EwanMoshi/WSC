package ECJ;

import TreeRepresentation.TreeNode;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import ec.util.Parameter;

public class WSCProblem extends GPProblem implements SimpleProblemForm {

	@Override
	public void setup(final EvolutionState state, final Parameter base) {
		super.setup(state, base);

		if (!(input instanceof WSCData)) {
			state.output.fatal("GPData class must be a subclass of " + WSCData.class, base.push(P_DATA), null);
		}
	}

	@Override
	public void evaluate(EvolutionState state, Individual ind, int subpopulation, int threadnum) {
		if (!ind.evaluated) {
			WSCInitializer init = (WSCInitializer) state.initializer;
			WSCData input = (WSCData) (this.input);

			GPIndividual gpInd = (GPIndividual) ind;
			gpInd.trees[0].child.eval(state, threadnum, input, stack, ((GPIndividual) ind), this);
			double[] qos = new double[4];
			qos[WSCInitializer.TIME] = input.maxTime;
			qos[WSCInitializer.AVAILABILITY] = 1.0;
			qos[WSCInitializer.RELIABILITY] = 1.0;

			for (TreeNode n : input.seenServices) {
				qos[WSCInitializer.COST] += n.qos[WSCInitializer.COST];
				qos[WSCInitializer.AVAILABILITY] *= n.qos[WSCInitializer.AVAILABILITY];
				qos[WSCInitializer.RELIABILITY] *= n.qos[WSCInitializer.RELIABILITY];
			}

			double fitness = calculateFitness(qos[WSCInitializer.AVAILABILITY], qos[WSCInitializer.RELIABILITY], qos[WSCInitializer.TIME], qos[WSCInitializer.COST], init);

			SimpleFitness f = ((SimpleFitness) ind.fitness);
			f.setFitness(state, fitness, false);

			ind.evaluated = true;
		}
	}

	private double calculateFitness(double a, double r, double t, double c, WSCInitializer init) {
		a = normaliseAvailability(a, init);
		r = normaliseReliability(r, init);
		t = normaliseTime(t, init);
		c = normaliseCost(c, init);

		double fitness = ((init.w1 * a) + (init.w2 * r) + (init.w3 * t) + (init.w4 * c));

		return fitness;

	}


	private double normaliseAvailability(double availability, WSCInitializer init) {
		if (init.maxAvailability - init.minAvailability == 0.0) {
			return 1.0;
		}
		else {
			return (availability - init.minAvailability)/(init.maxAvailability - init.minAvailability);
		}
	}

	private double normaliseReliability(double reliability, WSCInitializer init) {
		if (init.maxReliability - init.minReliability == 0.0) {
			return 1.0;
		}
		else {
			return (reliability - init.minReliability)/(init.maxReliability - init.minReliability);
		}
	}

	private double normaliseTime(double time, WSCInitializer init) {
		// If the time happens to go beyond the normalization bound, set it to the normalization bound
		if (time > init.maxTime) {
			time = init.maxTime;
		}

		if (init.maxTime - init.minTime == 0.0) {
			return 1.0;
		}
		else {
			return (init.maxTime - time)/(init.maxTime - init.minTime);
		}
	}

	private double normaliseCost(double cost, WSCInitializer init) {
		// If the cost happens to go beyond the normalization bound, set it to the normalization bound
		if (cost > init.maxCost) {
			cost = init.maxCost;
		}

		if (init.maxCost - init.minCost == 0.0) {
			return 1.0;
		}
		else {
			return (init.maxCost - cost)/(init.maxCost - init.minCost);
		}
	}
}
