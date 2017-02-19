package Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import Main.Neo4jConnection;
import component.ServiceNode;

public class Service {

	
	public String name;
	public double[] qos;
	public String[] inputs;
	public String[] outputs;
	public String[] inputServices;
	public String[] outputServices;
	
	public int distanceToStart = 0;
	
	public Set<Service> inputServicesList = new HashSet<Service>();
	public Set<Service> outputServicesList = new HashSet<Service>();

	public boolean visited = false;
	
	public String[] allTaxonomyOutputs;

	
	public Service(String name, double[] qos, String[] inputs, String[] outputs, String[] inputsArray, String[] outputsArray) {
		this.name = name;
		this.qos = qos;
		this.inputs = inputs;
		this.outputs = outputs;
		this.inputServices = inputsArray;
		this.outputServices = outputsArray;
	}
	
	public Service(String name) {
		this.name = name;
	}

}
