package ECJ;

import java.util.Set;

import TreeRepresentation.TreeNode;
import ec.gp.GPData;

/**
 * This object is responsible for holding the return value from a node and passing it onto
 * another.
 *
 */
public class WSCData extends GPData {

	public Set<TreeNode> seenServices;

	public double maxTime;

	public Set<String> inputSet;
	public Set<String> outputSet;

	public void copyTo(final GPData gpd) {
		WSCData wscd = (WSCData) gpd;

		wscd.inputSet = inputSet;
		wscd.outputSet = outputSet;

		wscd.maxTime = maxTime;

		wscd.seenServices = seenServices;
	}
}
