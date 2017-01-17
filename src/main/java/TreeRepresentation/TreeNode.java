package TreeRepresentation;

import java.util.List;
import java.util.Set;


public interface TreeNode {
	
	public double[] qos = new double[4];

	public String getName();
	
	public Set<String> getInputSet();
	
	public Set<String> getOutputSet();
	
	public void setInputSet(Set<String> inputSet);
	
	public void setOutputSet(Set<String> outputSet);
	
	public void setQos(double[] qos);
	
	public double[] getQos();

	public void addChild(TreeNode n);
	
	public void setParent(TreeNode n);

	public List<TreeNode> getChildren();
	
	public TreeNode getParent();

}
