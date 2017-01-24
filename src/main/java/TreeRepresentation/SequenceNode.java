package TreeRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ECJ.WSCData;
import ec.EvolutionState;
import ec.Problem;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;
import ec.gp.GPNode;

public class SequenceNode extends GPNode implements TreeNode {


	private TreeNode parent;
	private String name;
	private List<TreeNode> ch = new ArrayList<TreeNode>();
	private boolean visited = false;

	public double[] qos;

	private Set<String> inputSet = new HashSet<String>();
	private Set<String> outputSet = new HashSet<String>();
	
	// 	public TreeNode(String name, TreeNode parent, List<TreeNode> children, List<String> inputs) old constructor

	// nullary constructor
	public SequenceNode() {
		//children = new GPNode[2];
	}
	
	public SequenceNode(String name, TreeNode parent) {
		this.name = name;
		this.parent = parent;
		children = new GPNode[2];
	}


	@Override
	public void eval(EvolutionState state, int thread, GPData input, ADFStack stack, GPIndividual individual, Problem problem) {
		WSCData rd = ((WSCData) (input));
		Set<TreeNode> seenServices = new HashSet<TreeNode>();
		
		for (TreeNode n : ch) {
			if ((n.getName().equals("Sequence")) || (n.getName().equals("start"))) { // place sequence nodes in the right child and others (parallel/service nodes) as left
				children[1] = (GPNode) n;
			}
			else {
				children[0] = (GPNode) n;
			}
		}

		rd.inputSet = inputSet;
		rd.outputSet = outputSet;

		children[0].eval(state, thread, input, stack, individual, problem);
		seenServices = rd.seenServices;
		Set<String> in = rd.inputSet; // I think this only works if child[0] is the left child/the child that isn't the sequence (could be right depending on representation)
		
		children[1].eval(state, thread, input, stack, individual, problem);
		rd.seenServices.addAll(seenServices);

		rd.inputSet = in;
		


		// store the input and output information in this node? ::TODO Do I need this?
		inputSet = rd.inputSet;
		outputSet = rd.outputSet;
		//rd.seenServices = seenServices;
	}

	@Override
	public TreeNode clone() {
		// this should work for all three kinds of nodes (sequence, parallel, and service)
		SequenceNode newNode = new SequenceNode(name, parent);
		GPNode[] newChildren = new GPNode[children.length];

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				newChildren[i] = (GPNode) children[i].clone();
				newChildren[i].parent = newNode;
			}
			else {
				// this is a hack but I have no clue why children[i] is sometimes null which means the above can't be used
				// I think it's due to a race condition.. In debug mode the children array has nodes (non null) but if I use a print
				// statement, they print null, and get a null pointer exception. 
				// Setting a delay didn't work
				String name = ch.get(i).getName();
				TreeNode parent = ch.get(i).getParent();
				TerminalTreeNode n = new TerminalTreeNode (name, parent);
				n.setInputSet(ch.get(i).getInputSet());
				n.setOutputSet(ch.get(i).getOutputSet());
				n.qos = ch.get(i).getQos();		
				newChildren[i] = (GPNode) n;
			}
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
		return 2;
	}

	@Override
	public String toString() {
		for (TreeNode n : ch) {
			if ((n.getName().equals("Sequence")) || (n.getName().equals("start"))) { // place sequence nodes in the right child and others (parallel/service nodes) as left
				children[1] = (GPNode) n;
			}
			else {
				children[0] = (GPNode) n;
			}
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("%d [label=\"Sequence\"]; ", hashCode()));
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
