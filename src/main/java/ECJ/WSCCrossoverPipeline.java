package ECJ;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

public class WSCCrossoverPipeline extends BreedingPipeline {

	@Override
	public Parameter defaultBase() {
		return new Parameter("wsccrossoverpipeline");
	}

	@Override
	public int numSources() {
		return 2;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;

		Individual[] inds1 = new Individual[inds.length];
		Individual[] inds2 = new Individual[inds.length];

		int n1 = sources[0].produce(min, max, 0, subpopulation, inds1, state, thread);
		int n2 = sources[1].produce(min, max, 0, subpopulation, inds2, state, thread);

		if (!(sources[0] instanceof BreedingPipeline)) {
			for (int i = 0; i < n1; i++) {
				inds1[i] = (Individual) (inds1[i].clone());
			}
		}

		if (!(sources[1] instanceof BreedingPipeline)) {
			for (int i = 0; i < n2; i++) {
				inds2[i] = (Individual) (inds2[i].clone());
			}
		}

		if (!(inds1[0] instanceof WSCIndividual)) {
            state.output.fatal("WSCCrossoverPipeline didn't get a WSCIndividual. The offending individual is in subpopulation " + subpopulation + " and it's: " + inds1[0]);
		}

		if (!(inds2[0] instanceof WSCIndividual)) {
            state.output.fatal("WSCCrossoverPipeline didn't get a WSCIndividual. The offending individual is in subpopulation " + subpopulation + " and it's: " + inds2[0]);
		}

		int nMin = Math.min(n1, n2);

		for (int i = start, j = 0; i < nMin + start; i++, j++) {
			WSCIndividual t1 = ((WSCIndividual) inds1[j]);
			WSCIndividual t2 = ((WSCIndividual) inds2[j]);
		}

		return 0;
	}

}
