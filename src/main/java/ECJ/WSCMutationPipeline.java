package ECJ;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import component.TaxonomyNode;
import task.OuchException;
import Main.Main;
import Main.Neo4jConnection;
import TreeRepresentation.TreeNode;
import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;

public class WSCMutationPipeline extends BreedingPipeline {

	private static int fileCounter = 0;
	private static long startTime;
	private static long endTime;
	
	
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
			List<TreeNode> allNodes = new ArrayList<TreeNode>();
			
			Queue<TreeNode> queue = new LinkedList<TreeNode>();
			
			queue.offer((TreeNode) tree.trees[0].child);
			
			while(!queue.isEmpty()) {
				TreeNode current = queue.poll();
				allNodes.add(current);
				if (current.getChildren() != null) {
					for (TreeNode child : current.getChildren()) {
						//allNodes.add(child); // this is in original
						queue.offer(child);// this isn't in the original ECJTree repo
					}
				}
			}
			
			int selectedIndex = init.random.nextInt(allNodes.size());
			TreeNode selectedNode = allNodes.get(selectedIndex);
			GPNode selectedNodeCasted = (GPNode) selectedNode;
			
			selectedNodeCasted.children = selectedNode.getCh();

			TreeNode tNode = (TreeNode) selectedNode;
			
			//Set<String> inputs = tNode.getInputSet();
			//Set<String> outputs = tNode.getOutputSet();
			
			
			/******* Jacky's code of finding all outputs or something - not entirely sure how it works *********/
			Set<String> tNodeAllOutputs = new HashSet<String>();
			Set<String> tNodeAllInputs = new HashSet<String>();
			
			for(String s: tNode.getOutputSet()){
				TaxonomyNode taxonomyNode = Neo4jConnection.taxonomyMap.get(s);
				tNodeAllOutputs.addAll(Main.getTNodeParentsString(taxonomyNode));
			}
			
			for(String s: tNode.getInputSet()){
				TaxonomyNode taxonomyNode = Neo4jConnection.taxonomyMap.get(s);
				tNodeAllInputs.addAll(Main.getTNodeParentsString(taxonomyNode));
			}
			
			/*************** End of Jacky's Code ****************************/
			
			
			
			Neo4jConnection.loadFiles.taskInputs = tNodeAllInputs;
			Neo4jConnection.loadFiles.taskOutputs = tNodeAllOutputs;
			
			
			// this should create a new tree based on input/output of currently selected node (tNode from above)
			//Main main = new Main();
			
			startTime = System.currentTimeMillis();

			
			try {
				Main.main(new String[0]);
			} catch (IOException | OuchException e) {
				e.printStackTrace();
			}
			
			endTime = System.currentTimeMillis();

			 try {
				FileWriter writer2 = new FileWriter(new File("mutationTimeDebug/file"+fileCounter+".txt"));
				
			
				long totalTime = endTime - startTime;
				
				if (totalTime > 0) {
					writer2.append("\n Time Taken for mutation:   "+totalTime);
				}

				writer2.close();
				fileCounter++;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			GPNode newNode = (GPNode) Main.rootNode;
					
			tree.replaceNode(selectedNodeCasted, newNode); // 19th JAN START FROM HERE - NULL POINTER sometimes - figure out when and why
			tree.evaluated = false;
	
			
		}
		
		return n;
	}

}
