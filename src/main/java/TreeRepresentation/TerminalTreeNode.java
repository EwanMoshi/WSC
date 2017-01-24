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

public class TerminalTreeNode extends GPNode implements TreeNode {

	private TreeNode parent;
	private String name;
	private List<TreeNode> ch = new ArrayList<TreeNode>();
	private boolean visited = false;

	public double[] qos;

	private Set<String> inputSet = new HashSet<String>();
	private Set<String> outputSet = new HashSet<String>();

	// 	public TreeNode(String name, TreeNode parent, List<TreeNode> children, List<String> inputs) old constructor

	// nullary constructor
	public TerminalTreeNode() { 
		//children = new GPNode[0];
	}
	
	public TerminalTreeNode(String name, TreeNode parent) {
		this.name = name;
		this.parent = parent;
		children = new GPNode[0];
	}


	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual, Problem problem) {
		WSCData rd = ((WSCData) (input));
		if (rd.seenServices == null) {
			rd.seenServices = new HashSet<TreeNode>();
		}
		rd.seenServices = new HashSet<TreeNode>();
		
		rd.inputSet = inputSet;
		rd.outputSet = outputSet;
		rd.seenServices.add(this);
		rd.maxTime += qos[0];

		// store the input and output information in this node? ::TODO Do I need this?
		inputSet = rd.inputSet;
		outputSet = rd.outputSet;
	}


	@Override
	public int expectedChildren() {
		return 0;
	}

	@Override
	public String toString() {
		return String.format("%d [label=\"%s\"]; ", hashCode(), name);
	}

	@Override
	public int hashCode() {
		if (name == null) {
			return "null".hashCode();
		}
		return super.hashCode();
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
	
	@Override
	public TreeNode clone() {
		TerminalTreeNode newNode = new TerminalTreeNode(name, parent);

		newNode.inputSet = inputSet;
		newNode.outputSet = outputSet;
		newNode.qos = qos;
		
		return newNode;
	}

	public GPNode[] getCh() {
		return children;
	}
}
