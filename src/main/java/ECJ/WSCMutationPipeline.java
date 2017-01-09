package ECJ;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import Main.Main;
import TreeRepresentation.TreeNode;
import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;

public class WSCMutationPipeline extends BreedingPipeline {

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscmutationpipeline");
	}

	@Override
	public int numSources() {
		return 1;
	}

	@Override
	public int produce(int min, int max, int start, int subpopulation, Individual[] inds, EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;
		
		int n = sources[0].produce(min, max, start, subpopulation, inds, state, thread);
		
		if (!(sources[0] instanceof BreedingPipeline)) {
			for (int i = start; i < n + start; i++) {
				inds[i] = (Individual) (inds[i].clone());
			}
		}
		
		if (!(inds[start] instanceof WSCIndividual)) {
			state.output.fatal("WSCMutationPipeline didn't get a WSCIndividual. Offending individual is in subpopulation "+subpopulation + "and is "+inds[start]);
		}
		
		// perform mutation
		for (int i = start; i < n + start; i++) {
			WSCIndividual tree = (WSCIndividual) inds[i];
			WSCSpecies species = (WSCSpecies) tree.species;
			
			// Select random node in tree for mutation
			List<GPNode> allNodes = new ArrayList<GPNode>();
			Queue<GPNode> queue = new LinkedList<GPNode>();
			
			while(!queue.isEmpty()) {
				GPNode current = queue.poll();
				allNodes.add(current);
				if (current.children != null) {
					for (GPNode child : current.children) {
						allNodes.add(child);
					}
				}
			}
			
			int selectedIndex = init.random.nextInt(allNodes.size());
			GPNode selectedNode = allNodes.get(selectedIndex);
			TreeNode tNode = (TreeNode) selectedNode;
			
			// TODO: this should create a new tree based on input/output of current node
			Main main = new Main();
			GPNode newNode = main.rootNode;
			
			tree.replaceNode(selectedNode, newNode);
			tree.evaluated = false;
			
		}
		
		return n;
	}

}
