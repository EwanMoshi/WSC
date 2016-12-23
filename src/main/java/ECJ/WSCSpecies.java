package ECJ;

import Main.Main;
import ec.EvolutionState;
import ec.Individual;
import ec.Species;
import ec.util.Parameter;

public class WSCSpecies extends Species {

	@Override
	public Parameter defaultBase() {
		return null;
	}

	@Override
	public Individual newIndividual(EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;


		// TODO: I need to figure out how to modify the main class so that instead of looping and creating 30 or 50 (whatever it's set to) graphs and converting them to trees,
		// it will simply return 1 tree and then I use the following:
		// GPNode treeRoot = retrieve Root Of Converted Tree From Main Class
		// WSCIndividual tree = new WSCIndividual(treeRoot);

		WSCIndividual tree = null;

		Main main = new Main();
		if (main.rootNode == null) { // this won't stop this method from returning null but it will tell me what's causing the problem
			System.out.println("dun goof'd");
		}
		else {
			tree = new WSCIndividual(main.rootNode);
		}

		return tree;
	}

}
