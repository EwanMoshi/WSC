package TreeRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeNode {
	
	private TreeNode parent;
	private String name;
	private List<TreeNode> children = new ArrayList<TreeNode>();
	private boolean visited = false;
	
	private Set<String> inputSet = new HashSet<String>();
	private Set<String> outputSet = new HashSet<String>();
	
	// 	public TreeNode(String name, TreeNode parent, List<TreeNode> children, List<String> inputs) old consutrctor

	public TreeNode(String name, TreeNode parent) {
		this.name = name;
		this.parent = parent;
	}
	
	public boolean getVisited() {
		return this.visited;
	}
	
	public void setVisited(boolean b) {
		this.visited = b;
	}
	
	public void addChild(TreeNode n) {
		this.children.add(n);
	}
	
	
	public void setParent(TreeNode n) {
		parent = n;
	}
	
	public TreeNode getParent() {
		return parent;
	}
	
	public List<TreeNode> getChildren() {
		return children;
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
