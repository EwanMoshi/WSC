package TreeRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ECJ.WSCData;
import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class TreeNode extends GPNode {
	
	private TreeNode parent;
	private String name;
	private List<TreeNode> ch = new ArrayList<TreeNode>();
	private boolean visited = false;
	
	private Set<String> inputSet = new HashSet<String>();
	private Set<String> outputSet = new HashSet<String>();
	
	// 	public TreeNode(String name, TreeNode parent, List<TreeNode> children, List<String> inputs) old constructor

	public TreeNode(String name, TreeNode parent) {
		this.name = name;
		this.parent = parent;
	}
	
	
	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual, Problem problem) {
		WSCData rd = ((WSCData) (input));
		
		if (name.equals("Sequence")) { // sequence node
			for (TreeNode n : ch) {
				if (n.getName().equals("Sequence")) { // place sequence nodes in the right child and others (parallel/service nodes) as left
					children[1] = n;
				}
				else {
					children[0] = n;
				}
			}
			
			rd.inputSet = inputSet;
			rd.outputSet = outputSet;
			
			children[0].eval(state, thread, input, stack, individual, problem);
			Set<String> in = rd.inputSet; // I think this only works if child[0] is the left child/the child that isn't the sequence (could be right depending on representation)
			
			children[1].eval(state, thread, input, stack, individual, problem);

			rd.inputSet = in;
		}
		else if (name.equals("Parallel")) { //parallel node
			for (GPNode child : children) {
				child.eval(state, thread, input, stack, individual, problem);
			}
		}
		else { // service node
			rd.inputSet = inputSet;
			rd.outputSet = outputSet;
		}
		
		// store the input and output information in this node? ::TODO Do I need this?
		inputSet = rd.inputSet;
		outputSet = rd.outputSet;
	}
	
	@Override
	public TreeNode clone() {
		// this should work for all three kinds of nodes (sequence, parallel, and service)
		TreeNode newNode = new TreeNode(name, parent);
		GPNode[] newChildren = new GPNode[children.length];
		for (int i = 0; i < children.length; i++) {
			newChildren[i] = (GPNode) children[i].clone();
			newChildren[i].parent = newNode;
		}
		newNode.children = newChildren;
		newNode.inputSet = inputSet;
		newNode.outputSet = outputSet;
		return newNode;
	}
	
	
	@Override
	public int expectedChildren() {
		if (name.equals("Sequence")) {
			return 2;
		}
		else if (name.equals("Parallel")) {
			return -1; // negative number means "any number of children"
		}
		else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return null;
	}
	
	public boolean getVisited() {
		return this.visited;
	}
	
	public void setVisited(boolean b) {
		this.visited = b;
	}
	
	public void addChild(TreeNode n) {
		this.ch.add(n);
	}
	
	
	public void setParent(TreeNode n) {
		parent = n;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public List<TreeNode> getChildren() {
		return ch;
	}
	
	public void setName(String s) {
		name = s;
	}
	
	public String getName() {
		return name;
	}

	public Set<String> getInputSet() {
		return inputSet;
	}

	public void setInputSet(Set<String> inputSet) {
		this.inputSet = inputSet;
	}

	public Set<String> getOutputSet() {
		return outputSet;
	}

	public void setOutputSet(Set<String> outputSet) {
		this.outputSet = outputSet;
	}
	
	public void addAllInputs(Set<String> set) {
		for (String s : set) {
			if (!(inputSet.contains(s))) {
				inputSet.add(s);
			}
		}
	}

	public void addAllOutputs(Set<String> set) {
		for (String s : set) {
			if (!(outputSet.contains(s))) {
				outputSet.add(s);
			}
		}
	}

}
