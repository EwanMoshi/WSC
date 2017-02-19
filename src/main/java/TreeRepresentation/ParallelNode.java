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

// 9th Jan Update
// TODO: I need to create a terminal (class with 0 children)


public class ParallelNode extends GPNode implements TreeNode {

	private TreeNode parent;
	private String name;
	private List<TreeNode> ch = new ArrayList<TreeNode>();
	private boolean visited = false;

	public double[] qos;

	private Set<String> inputSet = new HashSet<String>();
	private Set<String> outputSet = new HashSet<String>();

	// 	public TreeNode(String name, TreeNode parent, List<TreeNode> children, List<String> inputs) old constructor

	// nullary constructor
	public ParallelNode() { 
		//children = new GPNode[0];
	}
	
	public ParallelNode(String name, TreeNode parent) {
		this.name = name;
		this.parent = parent;
	}


	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual, Problem problem) {
		WSCData rd = ((WSCData) (input));
		Set<TreeNode> seenServices = new HashSet<TreeNode>();
		Set<String> overallInputs = new HashSet<String>();
		Set<String> overallOutputs = new HashSet<String>();
		
		double maxTime = 0.0;
		
		// populate the children array
		children = new GPNode[ch.size()];
		for (int i = 0; i < children.length; i++) {
			children[i] = (GPNode) ch.get(i);
			children[i].parent = this;
		}
		
		for (GPNode child : children) { // UPDATE 10th JAN - children is null
			child.eval(state, thread, input, stack, individual, problem);
			
			if (rd.maxTime > maxTime) {
				maxTime = rd.maxTime;
			}
			
			seenServices.addAll(rd.seenServices);
			
			// Update overall inputs and outputs
			overallInputs.addAll(rd.inputSet);
			overallOutputs.addAll(rd.outputSet);
		}

		// store the input and output information in this node? ::TODO Do I need this?
		inputSet = rd.inputSet;
		outputSet = rd.outputSet;
		rd.seenServices = seenServices;
		rd.maxTime = maxTime;
		
		inputSet = overallInputs;
		outputSet = overallOutputs;
	}

	@Override
	public TreeNode clone() {
		// this should work for all three kinds of nodes (sequence, parallel, and service)
		ParallelNode newNode = new ParallelNode(name, parent);
		GPNode[] newChildren = new GPNode[children.length];
		for (int i = 0; i < children.length; i++) {
			newChildren[i] = (GPNode) children[i].clone();
			newChildren[i].parent = newNode;
		}
		newNode.children = newChildren;
		newNode.ch = ch;
		newNode.inputSet = inputSet;
		newNode.outputSet = outputSet;
		newNode.qos = qos;
		return newNode;
	}


	@Override
	public int expectedChildren() {
		return -1; // negative number means "any number of children"
	}

	@Override
	public String toString() {
		// populate the children array
		// this seems stupid but my children array is populated during the eval method but printing to dot format
		// is done in WSCSpecies before eval so the children array has nulls which means I need to do this here also.
		// NOTE: Perhaps it can be removed from eval() since the children array will be populated here but I can't be bothered with potential bugs
		children = new GPNode[ch.size()];
		for (int i = 0; i < children.length; i++) {
			children[i] = (GPNode) ch.get(i);
			children[i].parent = this;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("%d [label=\"Parallel\"]; ", hashCode()));
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				GPNode child = children[i];
				if (child != null) {
					builder.append(String.format("%d -> %d [dir=back]; ", hashCode(), children[i].hashCode()));
					builder.append(children[i].toString());
				}
			}
		}
		return builder.toString();
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

	public double[] getQos() {
		return qos;
	}

	public void setQos(double[] qos) {
		this.qos = qos;
	}

	public GPNode[] getCh() {
		return children;
	}
	
}
