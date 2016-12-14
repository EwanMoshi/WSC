package TreeRepresentation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeNode {
	
	private TreeNode parent;
	private String name;
	private List<TreeNode> children = new ArrayList<TreeNode>();
	private List<String> inputs = new ArrayList<String>();
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
	
	public List<String> getInputs() {
		return inputs;
	}


	public void setName(String s) {
		name = s;
	}
	
	public String getName() {
		return name;
	}

	
}
