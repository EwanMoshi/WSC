package ECJ;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import ec.gp.GPIndividual;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.simple.SimpleFitness;
import ec.util.Parameter;

public class WSCIndividual extends GPIndividual {

	public WSCIndividual() {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
	}


	public WSCIndividual(GPNode root) {
		super();
		super.fitness = new SimpleFitness();
		super.species = new WSCSpecies();
		super.trees = new GPTree[1];
		GPTree t = new GPTree();
		super.trees[0] = t;
		t.child = root;
	}

	@Override
	public Parameter defaultBase() {
		return new Parameter("wscindividual");
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WSCIndividual) {
			return toString().equals(other.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("digraph tree { ");
		b.append(trees[0].child.toString());
		b.append("}");
		return b.toString();
	}

	@Override
	public WSCIndividual clone() {
		WSCIndividual wsci = new WSCIndividual((GPNode) super.trees[0].child.clone());
		wsci.fitness = (SimpleFitness) fitness.clone();
		wsci.species = species;
		return wsci;
	}

	
	// TODO: Does this method really return all the tree nodes or just the first layer of children from trees[0].child?
	public List<GPNode> getAllTreeNodes() {
		List<GPNode> allNodes = new ArrayList<GPNode>();
		Queue<GPNode> queue = new LinkedList<GPNode>();
		
		queue.offer(trees[0].child);
		
		while(!queue.isEmpty()) {
			GPNode current = queue.poll();
			allNodes.add(current);
			if (current.children != null) {
				for (GPNode child : current.children) {
					allNodes.add(child);
				}
			}
		}
		return allNodes;
	}

	public void replaceNode(GPNode node, GPNode replacement) {
		if (node != null && replacement != null) {
			replacement = (GPNode) replacement.clone();
			GPNode parentNode = (GPNode) node.parent;
			
			if (parentNode == null) {
				super.trees[0].child = replacement;
			}
			else {
				replacement.parent = node.parent;
				for (int i = 0; i < parentNode.children.length; i++) {
					if (parentNode.children[i] == node) {
						parentNode.children[i] = replacement;
						break;
					}
				}
			}
		}
	}


}
