package TreeRepresentation;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
	
	private TreeNode parent;
	private String name;
	private List<TreeNode> children = new ArrayList<TreeNode>();
	private List<String> inputs = new ArrayList<String>();
	private boolean visited = false;
	
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
	
	
	public TreeNode getParent() {
		return parent;
	}
	
	public List<TreeNode> getChildren() {
		return children;
	}
	
	public List<String> getInputs() {
		return inputs;
	}


	public String getName() {
		return name;
	}

}
