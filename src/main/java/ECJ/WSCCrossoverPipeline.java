package ECJ;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import component.TaxonomyNode;
import Main.Main;
import Main.Neo4jConnection;
import TreeRepresentation.TreeNode;
import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.gp.GPNode;
import ec.util.Parameter;

public class WSCCrossoverPipeline extends BreedingPipeline {

	private static int fileCounter = 0;
	private static long startTime;
	private static long endTime;
	
	
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
		startTime = System.currentTimeMillis();

		
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

		
		
		endTime = System.currentTimeMillis();

		 try {
			FileWriter writer2 = new FileWriter(new File("crossoverTimeDebug/file"+fileCounter+".txt"));
			
		
			long totalTime = endTime - startTime;
			
			writer2.append("\n Time Taken for crossover:   "+totalTime);

			writer2.close();
			fileCounter++;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return n1;
	}

	public GPNode[] findReplacement(List<GPNode> nodes, List<GPNode> replacements) {
		GPNode[] result = new GPNode[2];
		
		for (GPNode node : nodes) {
			for (GPNode replacement: replacements) {
				TreeNode tNode = (TreeNode) node;
				TreeNode rNode = (TreeNode) replacement;
				
				
				List<String> rNodeAllOutputs = new ArrayList<String>();
				List<String> tNodeAllInputs = new ArrayList<String>();
				
				/******* Jacky's code of finding all outputs or something - not entirely sure how it works *********/
				for(String s: rNode.getOutputSet()){
					TaxonomyNode taxonomyNode = Neo4jConnection.taxonomyMap.get(s);
					rNodeAllOutputs.addAll(Main.getTNodeParentsString(taxonomyNode));
				}
				
				for(String s: tNode.getInputSet()){
					TaxonomyNode taxonomyNode = Neo4jConnection.taxonomyMap.get(s);
					tNodeAllInputs.addAll(Main.getTNodeParentsString(taxonomyNode));
				}
				
				/*************** End of Jacky's Code ****************************/
				
				// TODO: Make sure the input/output are the right way around - I think they are correct at the moment
				// if (tNode.getInputSet().containsAll(rNode.getInputSet()) && rNode.getOutputSet().containsAll(tNode.getOutputSet())) { // ORIGINAL
				if (tNodeAllInputs.containsAll(rNode.getInputSet()) && rNodeAllOutputs.containsAll(tNode.getOutputSet())) {
					System.out.println("SWAPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP");
					result[0] = node;
	                result[1] = replacement;
	                break;
				}
			}
		}
		
		return result;
	}
	
}
