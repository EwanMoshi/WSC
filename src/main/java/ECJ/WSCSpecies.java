package ECJ;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import task.OuchException;
import Main.Main;
import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.gp.GPNode;
import ec.util.Parameter;

public class WSCSpecies extends Species {

	private static int treeCounter = 0;
	
	private static long startTime;
	private static long endTime;

	
	@Override
	public Parameter defaultBase() {
		return new Parameter("wscspecies");
	}

	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
		startTime = 0;
		endTime = 0;
		
		startTime = System.currentTimeMillis();
		
		System.out.println("Load files Total execution time: " + (endTime - startTime) );
		
		WSCInitializer init = (WSCInitializer) state.initializer;


		// TODO: I need to figure out how to modify the main class so that instead of looping and creating 30 or 50 (whatever it's set to) graphs and converting them to trees,
		// it will simply return 1 tree and then I use the following:
		// GPNode treeRoot = retrieve Root Of Converted Tree From Main Class
		// WSCIndividual tree = new WSCIndividual(treeRoot);

		WSCIndividual tree = null;

		Main main = new Main();
		
		try {
			main.main(null);
		} catch (IOException | OuchException e) {
			e.printStackTrace();
		}
		
		if (main.rootNode == null) { // this won't stop this method from returning null but it will tell me what's causing the problem
			System.out.println("dun goof'd");
		}
		else {
			tree = new WSCIndividual((GPNode) main.rootNode);
		}
		
		endTime = System.currentTimeMillis();

		System.out.println("Printing TREE");
	    try {
			FileWriter writer2 = new FileWriter(new File("debug-tree"+treeCounter+".dot"));
			writer2.append(tree.toString());
			long totalTime = endTime - startTime;
			writer2.append("\n\n Time Taken:  "+totalTime);
			writer2.close();
			treeCounter++;
			//System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return tree;
	}

}
