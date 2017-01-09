package ECJ;

import java.util.Collections;
import java.util.List;

import TreeRepresentation.TreeNode;
import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
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

		// perform cross over
		for (int i = start, j = 0; i < nMin + start; i++, j++) {
			WSCIndividual t1 = ((WSCIndividual) inds1[j]);
			WSCIndividual t2 = ((WSCIndividual) inds2[j]);
			
			// find all nodes from both candidates
			List<GPNode> allt1Nodes = t1.getAllTreeNodes();
			List<GPNode> allt2Nodes = t2.getAllTreeNodes();
			
			// shuffle all nodes so crossover is random
			Collections.shuffle(allt1Nodes, init.random);
			Collections.shuffle(allt2Nodes, init.random);
			
			// For all t1 nodes, check if it can be replaced by a t2 node
			GPNode[] nodes = findReplacement(allt1Nodes, allt2Nodes);
			GPNode nodeT1 = nodes[0];
			GPNode replacementT2 = nodes[1];
			
			// For all t2 nodes, check if it can be replaced by a t1 node
			nodes = findReplacement(allt2Nodes, allt1Nodes);
			GPNode nodeT2 = nodes[0];
			GPNode replacementT1 = nodes[1];
			
			// perform replacement in both individuals
			t1.replaceNode(nodeT1, replacementT2);
			t2.replaceNode(nodeT2, replacementT1);
			
			inds[i] = t1;
			inds[i].evaluated = false;
			
			if (i+1 < inds.length) {
				inds[i+1] = t2;
				inds[i+1].evaluated = false;
			}
		}

		return n1;
	}

	public GPNode[] findReplacement(List<GPNode> nodes, List<GPNode> replacements) {
		GPNode[] result = new GPNode[2];
		
		for (GPNode node : nodes) {
			for (GPNode replacement: replacements) {
				TreeNode tNode = (TreeNode) node;
				TreeNode rNode = (TreeNode) replacement;
				
				// TODO: Make sure the input/output are the right way around - I think they are correct at the moment
				if (tNode.getInputSet().containsAll(rNode.getInputSet()) && rNode.getOutputSet().containsAll(tNode.getOutputSet())) {
					result[0] = node;
	                result[1] = replacement;
	                break;
				}
			}
		}
		
		return result;
	}
	
}
