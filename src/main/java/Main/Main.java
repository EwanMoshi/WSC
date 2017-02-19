package Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.Traversal;

import Graph.Service;
import TreeRepresentation.ParallelNode;
import TreeRepresentation.SequenceNode;
import TreeRepresentation.TerminalTreeNode;
import TreeRepresentation.TreeNode;
import component.ServiceNode;
import component.TaxonomyNode;
import generateDatabase.GenerateDatabase;
import modellingServices.LoadFiles;
import modellingServices.PopulateTaxonomyTree;
import task.FindCompositions;
import task.OuchException;
import task.ReduceGraphDb;
import task.RunTask;


//public class Main {
public class Main {
	
	private final static String Neo4j_testServicesDBPath = "database/test_services";
	private final static String Neo4j_ServicesDBPath = "database/";

	private static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	@SuppressWarnings("unused")
	private static Index<Node> services;
	@SuppressWarnings("unused")
	private static Index<Node> tempServices;
	private static String databaseName = "";
	public static LoadFiles loadFiles = null;
	//For setup == file location, composition size, and run test file or not
	//******************************************************//
	private final boolean runTestFiles = false;
	private final static String year = "2008";
	private final static String dataSet = "01";
	private final static int individuleNodeSize = 40; // 2nd Feb: Why is this set to 12?
	public static int candidateSize = 50;
	private final boolean runQosDataset = true;
	private final boolean runMultipileTime = false;

	// original values
	// private final static double m_a = 0.15;
	// private final static double m_r = 0.05;
	// private final static double m_c = 0.05;
	// private final static double m_t = 0.75;
	
	private final static double m_a = 0.2;
	private final static double m_r = 0.3;
	private final static double m_c = 0.3;
	private final static double m_t = 0.2;
		
	
	private static int fileCounter = 0;

	//******************************************************//

	public static TreeNode rootNode;

	public static Neo4jConnection globalConnection; 
	
	
	public static void main( String[] args ) throws IOException, OuchException {
		setupDatabase();
	}


    private static TreeNode setupDatabase() throws IOException, OuchException {
		Main neo4jwsc = new Main();
		globalConnection = Neo4jConnection.getInstance(); // set up the neo4j connection

		//dbCounter++;


		//		Thread t = new Thread(neo4jwsc,"Neo4jThread");
		//		t.start();
		databaseName = "wsc"+year+"dataset"+dataSet;
		String path;
		if(!neo4jwsc.runTestFiles){
			if(!neo4jwsc.runQosDataset){
				globalConnection.serviceFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/services.xml";
				globalConnection.taxonomyFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/taxonomy.xml";
				globalConnection.taskFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/problem.xml";
			}else{
				globalConnection.serviceFileName = "dataset/Dataset/Set"+dataSet+"MetaData/services-output.xml";
				globalConnection.taxonomyFileName = "dataset/Dataset/Set"+dataSet+"MetaData/taxonomy.xml";
				globalConnection.taskFileName = "dataset/Dataset/Set"+dataSet+"MetaData/problem.xml";
			}
		}else{
			globalConnection.serviceFileName = "dataset/test/test_serv.xml";
			globalConnection.taxonomyFileName = "dataset/test/test_taxonomy.xml";
			globalConnection.taskFileName = "dataset/test/test_problem.xml";
		}
		//load files
		long startTime = System.currentTimeMillis();
		
		//loadFiles();
		globalConnection.loadFiles();
		
		long endTime = System.currentTimeMillis();
		System.out.println("Load files Total execution time: " + (endTime - startTime) );


		//populateTaxonomytree
		startTime = System.currentTimeMillis();
		
		//populateTaxonomytree(); // Algorithm 1
		globalConnection.populateTaxonomyTree();
		
		endTime = System.currentTimeMillis();

		System.out.println("Populate Taxonomy Tree total execution time: " + (endTime - startTime) );


		//Identify run test file or not
		//if database exist read database
		//else if database not exist create database
		if(!neo4jwsc.runTestFiles){
			startTime = System.currentTimeMillis();
				
			//neo4jwsc.generateDB(null,Neo4j_ServicesDBPath,"original",databaseName+dbCounter);
			globalConnection.generateDB(null,Neo4j_ServicesDBPath,"original",databaseName);
						
			endTime = System.currentTimeMillis();
			System.out.println("Create new db Total execution time: " + (endTime - startTime) );
			
			path = Neo4j_ServicesDBPath+""+databaseName;
		}
		else{
			startTime = System.currentTimeMillis();
			try {
				FileUtils.deleteRecursively(new File(Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//neo4jwsc.generateDB(null,Neo4j_testServicesDBPath,"original test", null);
			globalConnection.generateDB(null,Neo4j_ServicesDBPath,"original",databaseName);
						
			endTime = System.currentTimeMillis();
			System.out.println("Create new test db Total execution time: " + (endTime - startTime) );

			path = Neo4j_testServicesDBPath;
		}



		//run task
		//1: copy database and call->tempServiceDatabase
		//2: connect to tempServiceDatabase
		//3: use task inputs outputs create start and end nodes and link to tempservicedatabase
		startTime = System.currentTimeMillis();
		
		//runTask(path); 
		globalConnection.runTask(path);
		//globalConnection.dbCounter = dbCounter;
		
		endTime = System.currentTimeMillis();
		System.out.println("run task: copied db, create temp db, add start and end nodes Total execution time: " + (endTime - startTime) );



		//reduce database use copied database
		startTime = System.currentTimeMillis();
		
		//reduceDB(); // Algorithm 4: Reduce Graph Database
		globalConnection.reduceDB();
		
		endTime = System.currentTimeMillis();
		System.out.println("reduce graph db Total execution time: " + (endTime - startTime) );


		// FIND COMPOSITIONS - Algorithm 5?
		if(!neo4jwsc.runMultipileTime) {
			//find compositions
			long startTimeComp = System.currentTimeMillis();

			
			startTime = System.currentTimeMillis();
			Map<List<Node>, Map<String,Map<String, Double>>> resultWithQos = findCompositions();
			endTime = System.currentTimeMillis();
			System.out.println();
			System.out.println("generate candidates Total execution time: " + (endTime - startTime) );
			System.out.println();
			System.out.println();
			System.out.println();

			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : resultWithQos.entrySet()) {
					GenerateDatabase gdb = new GenerateDatabase(entry.getKey(), Neo4jConnection.subGraphDatabaseService, "");
					gdb.setServiceMap(Neo4jConnection.serviceMap);
					gdb.setTaxonomyMap(Neo4jConnection.taxonomyMap);
					gdb.createServicesDatabase2();
					removeRedundantRel2();

					Map<Integer, List<Service>> nodeLayers = assignLayers();
					rootNode = graphToTreeNEW(nodeLayers);				
			}

			endTime = System.currentTimeMillis();
			System.out.println("create new result Total execution time: " + (endTime - startTimeComp) );
			
			 try {
					FileWriter writer2 = new FileWriter(new File("originalResultTime/file"+fileCounter+".txt"));
				
			
				long totalTime = endTime - startTime;
				
				if (totalTime > 0) {
					writer2.append("\n Time Taken for first:   "+totalTime);
				}
				fileCounter++;
				writer2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
		return rootNode;
	}

    /**
     * Breadth-First Traversal through tree updating the current node's
     * inputs and outputs.
     *
     * @param treeNode
     */
	private static void updateTreeInputOutput(TreeNode root) {
        List<TreeNode> toVisit = new ArrayList<TreeNode>();
        Stack<TreeNode> reversalStack = new Stack<TreeNode>();

		toVisit.add(root);

        // breadth-first traversal
        while (!(toVisit.isEmpty())) {

            // while we still have nodes to visit
            TreeNode currentNode = toVisit.get(0);
            toVisit.remove(0);

            if (currentNode.getName().equals("Parallel")) {
            	reversalStack.push(currentNode);

    			List<TreeNode> children = currentNode.getChildren();

    			for (TreeNode child : children) {
    				toVisit.add(child);
    				currentNode.getInputSet().addAll(child.getInputSet()); // add all the children's inputs to current node's input set
    				///currentNode.getOutputSet().addAll(child.getOutputSet()); // add all the children's outputs to the current node's output set
    			}
            }
            else if (currentNode.getName().equals("Sequence")) {
            	reversalStack.push(currentNode);

    			List<TreeNode> children = currentNode.getChildren();

    			// add the current node's parent input set to current node's input set
    			if (currentNode.getParent() != null) {
    				currentNode.getInputSet().addAll(currentNode.getParent().getInputSet());
    			}

    			for (TreeNode child : children) {
    				toVisit.add(child);
    				//if (child.getName().equals("Parallel") || child.getName().equals("Sequence")) { // we only want the left child's inputs
    				if (child.getName().equals("Sequence")) { // we only want the left child's inputs (parallel and web services)
    					continue;
    				}
    				currentNode.getInputSet().addAll(child.getInputSet());
    			}

/*    			// add left and right child's output set to current node's output set
    			for (TreeNode child : children) {
    				currentNode.getOutputSet().addAll(child.getOutputSet());
    			}*/
            }

        }

        while (!(reversalStack.isEmpty())) {
        	TreeNode currentNode = reversalStack.pop();

        	 if (currentNode.getName().equals("Parallel")) {

     			List<TreeNode> children = currentNode.getChildren();

     			for (TreeNode child : children) {
     				currentNode.getOutputSet().addAll(child.getOutputSet()); // add all the children's outputs to the current node's output set
     			}
             }
             else if (currentNode.getName().equals("Sequence")) {
     			List<TreeNode> children = currentNode.getChildren();

     			// add left and right child's output set to current node's output set
     			for (TreeNode child : children) {
    				currentNode.getOutputSet().addAll(child.getOutputSet());
     			}
             }
        }

	}


	/**
	 * Helper Method to check whether the tree was correctly converted from a graph.
	 *
	 * @param root
	 */
	public static void printTree(TreeNode root) {
        List<TreeNode> toVisit = new ArrayList<TreeNode>();

        toVisit.add(root);
        while (!(toVisit.isEmpty())) {
            // while we still have nodes to visit
            TreeNode currentNode = toVisit.get(0);
            toVisit.remove(0);

			System.out.println("================="+currentNode.getName()+"============");
			List<TreeNode> children = currentNode.getChildren();
			if (currentNode.getParent() != null) {
				System.out.println("PARENT Node: "+currentNode.getParent().getName());
			}

			for (String i : currentNode.getInputSet()) {
				System.out.println("Input Node: "+i);
			}

			for (String i : currentNode.getOutputSet()) {
				System.out.println("Output Node: "+i);
			}

			for (TreeNode child : children) {
				System.out.println("Child Node: "+child.getName());
				toVisit.add(child);
			}

/*       for (TreeNode n : tree) {
			System.out.println("================="+n.getName()+"============");
			List<TreeNode> children = n.getChildren();
			for (TreeNode child : children) {
				System.out.println("Child Node: "+child.getName());
			}
		}*/
        }
		System.out.println("+++++++++++++++++++++++++++++++++++++++DONE+++++++++++++++++++++++++++++++");

	}


	/**
	 * Returns the corresponding node for the current TreeNode
	 * @param n
	 * @param nodeToTreeNodeMap
	 * @return
	 */
	private static Node findCorrespondingNode(TreeNode n, Map<Node, TreeNode> map) {
	    for (Entry<Node, TreeNode> e : map.entrySet()) {
	        if (e.getValue().getName().equals(n.getName())) {
	            return e.getKey();
	        }
	    }
	    return null;
	}

	private static String[] getOutputArray(Object obj) {
			String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
			String[] tempInputs = ips.split("\\s*,\\s*");
			String[] array = new String[0];
			for (String s: tempInputs) {
				if (s.length() > 0) {
					array = increaseArray(array);
					array[array.length-1] = s;
				}
			}
			return array;
	}


	private static void printResult(Map<List<Node>, Map<String, Map<String, Double>>> resultWithQos) {
		System.out.println("Best result: ");
		Transaction tx = Neo4jConnection.subGraphDatabaseService.beginTx();


		try{
			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry2 : resultWithQos.entrySet()){
				for(Node n: entry2.getKey()){
					System.out.print(n.getProperty("name")+"--"+n.getId()+"   ");
				}
				System.out.println();
				System.out.print("QOS:  ");
				for (Map.Entry<String,Map<String, Double>> entry3 : entry2.getValue().entrySet()){
					if(entry3.getKey().equals("normalized")){
						System.out.println("normalized: ");
						for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
							System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");

						}
						System.out.println();
						double fitnessOfBest = m_a*entry3.getValue().get("A") + m_r*entry3.getValue().get("R") + m_c*entry3.getValue().get("C") + m_t*entry3.getValue().get("T");
						System.out.println("fitnessOfBest:" +fitnessOfBest);
					}
					else if(entry3.getKey().equals("non_normalized")){
						System.out.println("non_normalized: ");
						for (Map.Entry<String, Double> entry4 : entry3.getValue().entrySet()){
							System.out.print(entry4.getKey()+": "+entry4.getValue()+"     ");

						}
					}


				}
			}
			System.out.println();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("print populations error..");
		} finally {
			tx.close();
		}
	}


	private static Map<List<Node>, Map<String, Map<String, Double>>> findCompositions() {
		FindCompositions findCompositions = new FindCompositions(candidateSize, individuleNodeSize, Neo4jConnection.subGraphDatabaseService);
		findCompositions.setStartNode(Neo4jConnection.startNode);
		findCompositions.setEndNode(Neo4jConnection.endNode);
		findCompositions.setNeo4jServNodes(Neo4jConnection.neo4jServNodes);
		findCompositions.setTaxonomyMap(Neo4jConnection.taxonomyMap);
		findCompositions.setSubGraphNodesMap(Neo4jConnection.subGraphNodesMap);
		findCompositions.setM_a(m_a);
		findCompositions.setM_r(m_r);
		findCompositions.setM_c(m_c);
		findCompositions.setM_t(m_t);
		Map<List<Node>, Map<String, Map<String, Double>>> candidates = null;
		try {
			candidates = findCompositions.run(); // size 50
		} catch (OuchException e1) {
			e1.printStackTrace();
		}

		//Transaction transaction = subGraphDatabaseService.beginTx();
		try{
			System.out.println("candidates: ");
			int i = 0;
/*			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : candidates.entrySet()){

				System.out.println();
				System.out.println();
				System.out.print("candidate "+ ++i+": ");

				// remove these helpers later?
				for(Node n: entry.getKey()){
					System.out.print(n.getProperty("name")+"  ");
				}
				System.out.println();
				System.out.println("Total service nodes:"+entry.getKey().size());
				for (Map.Entry<String,Map<String, Double>> entry2 : entry.getValue().entrySet()){
					System.out.println(entry2.getKey()+": ");
					for (Map.Entry<String, Double> entry3 : entry2.getValue().entrySet()){
						System.out.print("    "+entry3.getKey()+": "+entry3.getValue()+";   ");
					}
					System.out.println();
				}
				System.out.println();
			}*/

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("print populations error..");
		} finally {
			//transaction.close();
		}
		 return findCompositions.getResult(candidates); //::TODO this returns the best candidate but maybe I just want all the candidates to perform GP on
		// TODO: maybe I should only return 1 (i.e. the above)? but then is that a good thing? maybe takes more time to find best, maybe I just want to return a random one?
		//return candidates;
	}



	private static boolean isNodeFulfilled(Node node, Set<Node> nodes, GraphDatabaseService graphDatabaseService) {

		Transaction transaction = graphDatabaseService.beginTx();

		Set<String> inputs = new HashSet<String>();
		Set<String> nodeInputs = new HashSet<String>();
		nodeInputs.addAll(Arrays.asList(getNodePropertyArray(node,"inputs")));

		Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
		for(Relationship r: rels){
			//				Node n = neo4jServNodes.get(r.getProperty("From"));

			if(contains((String) r.getProperty("From"),nodes,graphDatabaseService)){
				inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs")));
			}
		}
		if(!equalLists(inputs, nodeInputs)){

			transaction.close();
			return false;
		}else{
			transaction.close();
			return true;
		}



	}
	private static boolean contains(String property, Set<Node> result, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		for(Node n: result){
			if(n.getProperty("name").equals(property)){
				transaction.close();
				return true;
			}
		}
		transaction.close();
		return false;
	}
	


	private static void removeRedundantRel(GraphDatabaseService graphDatabaseService, GenerateDatabase generateDatabase2) {
		List<Relationship>toRemove = new ArrayList<Relationship>();
		Transaction transaction = graphDatabaseService.beginTx();
		try{
			Iterable<Node> nodes = graphDatabaseService.getAllNodes();
			for(Node node: nodes){
				if(!node.getProperty("name").equals("start")){
					Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
					for(Relationship r: rels){
						Transaction tt = graphDatabaseService.beginTx();
						r.setProperty("removeable", true);
						tt.success();
						tt.close();
						if(isAllNodesFulfilled(node, nodes,graphDatabaseService)){
							//							Transaction t = graphDatabaseService.beginTx();
							//							r.delete();
							//							System.out.println("eeee: "+r.getId());
							//							t.success();
							//							t.close();
							toRemove.add(r);
/*							String[] outputs = generateDatabase2.getNodePropertyArray(node, "outputServices");
							for (int i = 0; i < outputs.length; i ++) {
								System.out.println("+++++++++++++++++++++++++++++           "+outputs[i]);
							}*/
						}else{
							Transaction ttt = graphDatabaseService.beginTx();
							r.setProperty("removeable", false);
							ttt.success();
							ttt.close();
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Main removeRedundantRel");
		} finally {
			transaction.close();
		}
		for(Relationship r: toRemove){
			Transaction t = graphDatabaseService.beginTx();
			System.out.println("remove: "+r.getId());
			//System.out.println("remove: "+r.);

			r.delete();
			t.success();
			t.close();
		}
	}


	private static boolean isAllNodesFulfilled(Node node, Iterable<Node> nodes, GraphDatabaseService graphDatabaseService) {

		Transaction transaction = graphDatabaseService.beginTx();
		Set<String> inputs = new HashSet<String>();
		Set<String> nodeInputs = new HashSet<String>();
		nodeInputs.addAll(Arrays.asList(getNodePropertyArray(node,"inputs")));

		Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
		for(Relationship r: rels){
			if(!(boolean) r.getProperty("removeable")){
				inputs.addAll(Arrays.asList(getNodeRelationshipPropertyArray(r, "outputs")));
			}
		}
		for(Node n: nodes){
			if(!n.getProperty("name").equals("start") &&!n.getProperty("name").equals("end") ){
				Iterable<Relationship> relationships = n.getRelationships(Direction.OUTGOING);
				int i = 0;
				for(Relationship r: relationships){
					if(!(boolean) r.getProperty("removeable"))
						i++;
				}
				if(i==0){
					transaction.close();
					//node not start node
					//node has no output relationship
					return false;
				}
			}

		}
		if(equalLists(inputs, nodeInputs)){
			transaction.close();
			return true;
		}
		return false;

	}
	public static boolean equalLists(Set<String> one, Set<String> two){

		List<String>one1 = new ArrayList<String>(one);
		List<String>two2 = new ArrayList<String>(two);
		List<String>one11 = new ArrayList<String>(one);
		List<String>two22 = new ArrayList<String>(two);
		one1.retainAll(two2);
		two22.retainAll(one11);

		if (one1.size()>0 && two22.size()>0 && two22.size()==two.size() && one1.size()==one.size() && one11.size()>0 && two2.size()>0){
			return true;
		}
		else return false;
	}



	private static void populateTaxonomytree() {
		PopulateTaxonomyTree populateTaxonomyTree = new PopulateTaxonomyTree();
		populateTaxonomyTree.setTaxonomyMap(taxonomyMap);
		populateTaxonomyTree.setServiceMap(serviceMap);
		populateTaxonomyTree.populateTaxonomyTree();
		//		TaxonomyNode t = taxonomyMap.get("inst958190119");
		//		System.out.println("==========================================");
		//		System.out.println(t);
		//
		//		for(TaxonomyNode tn: t.parents_notGrandparents){
		//			System.out.println(tn.value);
		//		}
	}




	private static void signNodesToField(Map<String, Node> neo4jServNodes, GraphDatabaseService graphDatabaseService) {
		Transaction transaction = graphDatabaseService.beginTx();
		@SuppressWarnings("deprecation")
		Iterable<Node> nodes = graphDatabaseService.getAllNodes();
		neo4jServNodes.clear();
		int i = 0;
		for(Node n: nodes){
			i++;
			neo4jServNodes.put((String)n.getProperty("name"), n);

		}
		System.out.println("total service nodes: "+i);
		transaction.success();
		transaction.close();
	}



	private static String[] getNodeRelationshipPropertyArray(Relationship relationship, String property){
		Object obj =relationship.getProperty(property);
		//    		//remove the "[" and "]" from string
		String string = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempOutputs = string.split("\\s*,\\s*");
		String[] array = new String[0];
		for(String s: tempOutputs){
			if(s.length()>0){
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}
	private static String[] getNodePropertyArray(Node sNode, String property){
		Object obj =sNode.getProperty(property);
		//    		//remove the "[" and "]" from string
		String[] array = new String[0];

		String ips = Arrays.toString((String[]) obj).substring(1, Arrays.toString((String[]) obj).length()-1);
		String[] tempInputs = ips.split("\\s*,\\s*");

		for(String s: tempInputs){
			if(s.length()>0){
				array =increaseArray(array);
				array[array.length-1] = s;
			}
		}
		return array;
	}
	private static String[] increaseArray(String[] theArray)
	{
		int i = theArray.length;
		int n = ++i;
		String[] newArray = new String[n];
		if(theArray.length==0){
			return new String[1];
		}
		else{
			for(int cnt=0;cnt<theArray.length;cnt++)
			{
				newArray[cnt] = theArray[cnt];
			}
		}

		return newArray;
	}

		
	
    private static TreeNode graphToTreeNEW(Map<Integer, List<Service>> nodeLayers) {
        List<TreeNode> tree = new ArrayList<TreeNode>();

        TreeNode previous = null; // a node to store the sequence node from previous iteration

        // iterate over the layers in reverse
        for (int i = nodeLayers.size()-1; i >= 0; i--) {
        	if (nodeLayers.get(i) == null) {
        		continue;
        	}

        	TreeNode sequenceCurrent = new SequenceNode("Sequence", null);
        	if (previous != null) {
        		if (nodeLayers.get(i).get(0).name.equals("start")) {
        			TreeNode startNode = new TerminalTreeNode("start", previous);

        			Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).inputs));
        			Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).outputs));
        			startNode.setInputSet(inputSet);
        			startNode.setOutputSet(outputSet);

        			double[] qos = nodeLayers.get(i).get(0).qos;
        			/*		                qos[0] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightTime").toString());
		                qos[1] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightCost").toString());
		                qos[2] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightAvailibility").toString());
		                qos[3] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightReliability").toString());*/

        			startNode.setQos(qos);

        			previous.addChild(startNode);
        			break;
        		}
        		else {
        			sequenceCurrent.setParent(previous);
        			previous.addChild(sequenceCurrent);
        		}
        	}

        	// if the number of nodes in current layer = 1
        	// create a single child with the current sequence node as its parent
        	if (nodeLayers.get(i).size() == 1) {
        		TreeNode n = new TerminalTreeNode(nodeLayers.get(i).get(0).name, sequenceCurrent);

        		Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).inputs));
        		Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).outputs));
        		n.setInputSet(inputSet);
        		n.setOutputSet(outputSet);

        		double[] qos = nodeLayers.get(i).get(0).qos;
        		/*	                qos[0] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightTime").toString());
	                qos[1] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightCost").toString());
	                qos[2] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightAvailibility").toString());
	                qos[3] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightReliability").toString());*/

        		n.setQos(qos);

        		sequenceCurrent.addChild(n);
        	}
        	// else if the number of nodes in current layer > 1
        	// create a parallel node with all the nodes in current layer as its children
        	else if (nodeLayers.get(i).size() > 1) {
        		TreeNode parallel = new ParallelNode("Parallel", sequenceCurrent);
        		int childCounter = 0; // counter to retrieve all the children in the List<Service> from NodeLayers map

        		for (Service ch : nodeLayers.get(i)) {
        			TreeNode child = new TerminalTreeNode(ch.name, parallel);

        			Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) ch.inputs));
        			Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) ch.outputs));
        			child.setInputSet(inputSet);
        			child.setOutputSet(outputSet);

        			double[] qos = nodeLayers.get(i).get(childCounter).qos;
        			/*		                qos[0] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightTime").toString());
		                qos[1] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightCost").toString());
		                qos[2] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightAvailibility").toString());
		                qos[3] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightReliability").toString());*/

        			child.setQos(qos);

        			parallel.getInputSet().addAll(inputSet);
        			parallel.getOutputSet().addAll(outputSet);

        			parallel.addChild(child);
        			childCounter++;
        		}
        		sequenceCurrent.addChild(parallel);

        	}
        	tree.add(sequenceCurrent);
        	previous = sequenceCurrent; // set previous to current (for next iteration)

        }

        updateTreeInputOutput(tree.get(0));
        //printTree(tree.get(0));


    	return tree.get(0);
	}


    /**
     * Go through each node and assign the appropriate layer. The layer that a node/service falls in
     * is determined by it's maximum distance to the starting node.
     * e.g. a node/service whose maximum distance (number of edges) to the start node is 4,
     * will be placed in layer 4 along with any other nodes/services that may also be 4 edges away
     * @return
     */
	private static Map<Integer, List<Service>> assignLayers() {

		List<Service> toVisit = new ArrayList<Service>();
        Service startNode2 = null;
        
        // find the starting node
		for(Service s : GenerateDatabase.servicesList) {
			if (s.name.equals("start")) {
				startNode2 = s;
				break;
			}
		}
		
		
        assignLongestPaths(startNode2, 0);
        // testOutput(startNode2);
		
        
        Map<Integer, List<Service>> nodeLayers = new TreeMap<Integer, List<Service>>();

        // initialize the map and create an empty list for each potential layer
        for (int i = 0; i < GenerateDatabase.servicesList.size(); i++) {
        	nodeLayers.put(new Integer(i), new ArrayList<Service>());
        }

        int maxLength = 0;
        for (Service s : GenerateDatabase.servicesList) {
        	if (s.distanceToStart > maxLength) {
        		maxLength = s.distanceToStart; // keep track of the longest path
        	}
        	
        	if (s.name.equals("end")) { // don't add the end node yet (because it will be added to layer 0 which is its default value)
        		continue;
        	}

        	nodeLayers.get(s.distanceToStart).add(s);
        }
        
        // go through and look for end node and assign it maxLength + 1
        // NOTE: This is a hack since the way we generate our relationships differs to Jacky's
        for (Service s : GenerateDatabase.servicesList) {
        	if (s.name.equals("end")) {
            	nodeLayers.get(maxLength + 1).add(s);
        		s.distanceToStart = maxLength + 1; // probably don't need this but it's nice
        		break;
        	}
        }

        
		List<Integer> layersToRemove = new ArrayList<Integer>();
		
		// find and mark empty layers (if they exist)
		// 14th FEB Problem: This is now causing problems because of recursive web services
		// so we may end up with layers looking like: 0, 2, 3, 4, 5, 6, 9, 10, 11
		// so the problem is that there are missing layers, e.g. 1, 7, and 8 (in the example on the line above)
		for (Entry<Integer, List<Service>> e : nodeLayers.entrySet()) {
			if (e.getValue().isEmpty()) {
				layersToRemove.add(e.getKey());
			}
		}

		// remove the empty layers
		for (Integer layer : layersToRemove) {
			nodeLayers.remove(layer);
		}
		   
		// iterate over the nodelayers and shuffle layers down to fill up missing space - because of the problem described in the comments on lines above ("14th Feb Problem")
		int i = 0;
		for (Entry<Integer, List<Service>> e : nodeLayers.entrySet()) {
			nodeLayers.put(new Integer(i), e.getValue());
			i++;
		}



		for (Entry<Integer, List<Service>> e : nodeLayers.entrySet()) {
			if (e.getKey().equals(new Integer(0)) && e.getValue().size() > 1) {
				System.out.println("asd");
			}
		}

			System.out.println("--------------------DEBUGGING-------------------");
		    for (Map.Entry<Integer, List<Service>> e : nodeLayers.entrySet()) {
			    System.out.println("LAYER   "+e.getKey());
			    for (Service n : e.getValue()) {
			    	System.out.println(n.name);
			    }
		    }

        return nodeLayers;

    }

	private static void testOutput(Service startNode) {
		List<Service> toVisit = new ArrayList<Service>();
		
		toVisit.add(startNode);
		
        while (!(toVisit.isEmpty())) {
            // while we still have nodes to visit
            Service currentNode = toVisit.get(0);
            toVisit.remove(0);
           
            for (Service child : currentNode.outputServicesList) {
            	toVisit.add(child);
            }
            
        }
        
	}


	/**
	 * Recursive method which goes through each node using brute force to find all paths and 
	 * keeping track of the longest. Once complete, each node/service in the graph
	 * should be assigned the length of the longest (number of edges) path to the start
	 * @param node
	 * @param currentSum
	 */
	public static void assignLongestPaths(Service node, int currentSum) {
		if (node.visited) {
			return;
		}
		node.visited = true;
		
		if (node.distanceToStart < currentSum) {
			node.distanceToStart = currentSum;
		}
		
        // get longest path for each of the children
		for (Service child : node.outputServicesList) {
			assignLongestPaths(child, currentSum + 1);
		}
		
        node.visited = false;
	}



	private static void removeRedundantRel2() {
		Map<Service, Service> selfRemoval = new HashMap<Service, Service>();

		// iterate over all nodes
		for (Service currentService : GenerateDatabase.servicesList) {
			
			if (!currentService.name.equals("start")) {
				List<Service> parentsToRemove = findRemovableInputRelationships(currentService);
				//List<Service> outputsToRemove = findRemovableOutputRelationships(currentService);

				for (Service s : parentsToRemove) {
					currentService.inputServicesList.remove(s);
					s.outputServicesList.remove(currentService); // also remove it from the removed service's outputs
				}
								
				for (Service s : currentService.outputServicesList) {
					if (currentService.name.equals(s.name)) {
						selfRemoval.put(currentService, s);
					}
				}
				
/*				for (Service s : outputsToRemove) {
					currentService.outputServicesList.remove(s);
				}*/
			}			
		}
		
		// remove services that output themselves in order to get rid of infinite loops
		for (Map.Entry<Service, Service> entry : selfRemoval.entrySet()) {
			entry.getKey().outputServicesList.remove(entry.getValue());
		}
	}
	



	private static List<Service> findRemovableOutputRelationships(Service currentService) {
		List<Service> removeList = new ArrayList<Service>();

		Set<String> currentServiceOutputs = new HashSet<String>();
		currentServiceOutputs.addAll(Arrays.asList(currentService.outputs));
		
		for (Service child : currentService.outputServicesList) {
			List<String> childRequiredInputs = Arrays.asList(child.inputs);
			
			List<String> parentAllOutputs = new ArrayList<String>();
			
			/******* Jacky's code of finding all outputs or something - not entirely sure how it works *********/
			for(String s: currentServiceOutputs){
				TaxonomyNode tNode = Neo4jConnection.taxonomyMap.get(s);
				parentAllOutputs.addAll(getTNodeParentsString(tNode));
			}
			
			List<String> temp = new ArrayList<String>(parentAllOutputs);			
			temp.retainAll(childRequiredInputs);
			
			String[] tempToArray = new String[temp.size()];
			for(int i = 0; i<temp.size(); i++){
				tempToArray[i] = temp.get(i);
			}
			/*************** End of Jacky's Code ****************************/

			if (!temp.containsAll(childRequiredInputs)) {
				removeList.add(child);
			}
			
		}
		
		return removeList;
	}

	private static List<Service> findRemovableInputRelationships(Service node) {

		Set<String> nodeInputs = new HashSet<String>();
		nodeInputs.addAll(Arrays.asList(node.inputs));
		
		
		Map<Service, Integer> satisfies = new HashMap<Service, Integer>();
		List<Service> removeList = new ArrayList<Service>();


		for (Service parent : node.inputServicesList) {
			/******* Jacky's method of finding all outputs or something - not entirely sure how it works *********/
			
			Set<String> parentOutputs = new HashSet<String>();
			parentOutputs.addAll(Arrays.asList(parent.outputs));
			
			List<String> parentAllOutputs = new ArrayList<String>();
			
			for(String s: parentOutputs){
				TaxonomyNode tNode = Neo4jConnection.taxonomyMap.get(s);
				parentAllOutputs.addAll(getTNodeParentsString(tNode));
			}
					
		
			List<String> temp = new ArrayList<String>(parentAllOutputs);			
			temp.retainAll(nodeInputs);
						
			String[] tempToArray = new String[temp.size()];
			for(int i = 0; i<temp.size(); i++){
				tempToArray[i] = temp.get(i);
			}			
			/*************** End of Jacky's Code ****************************/

			for (String output : tempToArray) {
				if (nodeInputs.contains(output)) { // if the output of the parent is required by the current service (node parameter)
					if (!satisfies.containsKey(parent)) { // if the parent has not been stored, add it with value of 1, else increment 1
						satisfies.put(parent, 1);
					}
					else {
						satisfies.put(parent, satisfies.get(parent) + 1);
					}
				}
			}
			
			parent.allTaxonomyOutputs = tempToArray; // set all the outputs from the taxonomy node so that we can use it in the next section
		}
			
		// check if there are parents that don't satisfy any inputs and mark them to be removed
		for (Service parent : node.inputServicesList) {
			if (!satisfies.containsKey(parent)) {
				removeList.add(parent); 
			}
		}

		// it's much easier to iterate over the sorted map and reduces problems which would arise if we were to iterate
		// over a map with unsorted values e.g. keeping nodes that satisfy less inputs than nodes that satisfy the same AND more
		Map<Service,Integer> sortedSatisfiesMap = sortMap(satisfies);
		
		// a list to keep track of which inputs have been satisfied so far
		List<String> satisfied = new ArrayList<String>();

		// iterate over the sorted satisfies map and find which parent outputs more inputs needed by the service (node parameter)
		// and keep the parents that satisfy the most
		for (Map.Entry<Service, Integer> entry : sortedSatisfiesMap.entrySet()) {
			if (entry.getKey().equals(node.name)) { // if we're iterating over the current node, continue (sometimes the same node can contain itself as an output)
				continue;
			}
			boolean shouldRemove = true;
			for (String s : entry.getKey().allTaxonomyOutputs) {
				// if there is at least one output that isn't satisfied already, mark service so that we keep it
				if (!satisfied.contains(s)) {
					satisfied.add(s); // add to list if not already in there
					shouldRemove = false;
				}
			}
			
			// if the service's outputs are already satisfied, add it to the remove list 
			if (shouldRemove) {
				removeList.add(entry.getKey());
			}
		}

		return removeList;
	}

	
	public static Set<String> getTNodeParentsString(TaxonomyNode tNode) {
		Set<String>tNodeParents = new HashSet<String>();
		for(String t: tNode.parentsString){
			TaxonomyNode taxNode = Neo4jConnection.taxonomyMap.get(t);
			if(!taxNode.isInstance){
				Set<TaxonomyNode> taxNodeInstanceChildren = taxNode.childrenInstance;
				for(TaxonomyNode childInstance: taxNodeInstanceChildren){
					tNodeParents.add(childInstance.value);
				}
			}
			else{
				tNodeParents.add(t);
			}
		}
		return tNodeParents;
	}
	
	/**
	 * This method takes in a map and sorts it in ascending values
	 * 
	 * @param satisfies
	 * @return
	 */
	private static Map<Service, Integer> sortMap(Map<Service, Integer> satisfies) {
		Set<Entry<Service, Integer>> mapEntries = satisfies.entrySet();
		List<Entry<Service,Integer>> tempList = new LinkedList<Entry<Service,Integer>>(mapEntries);
		
		Collections.sort(tempList, new Comparator<Entry<Service, Integer>>() {
            @Override
            public int compare(Entry<Service, Integer> e1, Entry<Service, Integer> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });
		
	    // use LinkedHashMap to preserve the order of insertion. 
        Map<Service,Integer> sortedMap = new LinkedHashMap<Service, Integer>();
        for(Entry<Service,Integer> entry: tempList) {
        	sortedMap.put(entry.getKey(), entry.getValue());
        }
        
        return sortedMap;
	}
	
	
	
	
	

    /**
     * This method was the old graphToTree which used the database result from Jacky's work.
     * Don't use this anymore. It's not good for GP as it creates a new database for each individual.
     * Only use this if we want to build layers from the database and convert that into a tree - probably never again.
     * @param nodeLayers
     * @param generateDatabase
     * @param newGraphDatabaseService
     * @param graphNodes
     * @param dbPath
     * @return
     */
    @Deprecated
    private static TreeNode graphToTree(Map<Integer, List<Node>> nodeLayers, GenerateDatabase generateDatabase, GraphDatabaseService newGraphDatabaseService, List<Node> graphNodes, String dbPath) {
    	Transaction transaction = newGraphDatabaseService.beginTx();
        List<TreeNode> tree = new ArrayList<TreeNode>();

    	TreeNode previous = null; // a node to store the sequence node from previous iteration
    	int loopCounter = 0;
    	int parallelCounter = 0;
    	int random = 0;

	    for (Entry<Integer, List<Node>> e : nodeLayers.entrySet()) {
	    	System.out.println(e.getKey());
	    }
     	
    	try {
	    	// iterate over the layers in reverse
	    	for (int i = nodeLayers.size()-1; i >= 0; i--) {
	    		if (nodeLayers.get(i) == null) {
	    			continue;
	    		}
	    		
	    		random++;
	    		TreeNode sequenceCurrent = new SequenceNode("Sequence", null);
	    		random++;
	    		if (previous != null) {
		    		random++;
	    			if (nodeLayers.get(i).get(0).getProperty("name").toString().equals("start")) {
	    	    		random++;
	    				TreeNode startNode = new TerminalTreeNode("start", previous);

		                Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).getProperty("inputs")));
		                Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).getProperty("outputs")));
		                startNode.setInputSet(inputSet);
		                startNode.setOutputSet(outputSet);

		                double[] qos = new double[4];
		                qos[0] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightTime").toString());
		                qos[1] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightCost").toString());
		                qos[2] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightAvailibility").toString());
		                qos[3] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightReliability").toString());

		                startNode.setQos(qos);

	    				previous.addChild(startNode);
	    				break;
	    			}
	    			else {
		    			sequenceCurrent.setParent(previous);
		    			previous.addChild(sequenceCurrent);
	    			}
	    		}
	    		
	    		 // if the number of nodes in current layer = 1
				 // create a single child with the current sequence node as its parent
	    		if (nodeLayers.get(i).size() == 1) {
	    			loopCounter++;
	    			TreeNode n = new TerminalTreeNode(nodeLayers.get(i).get(0).getProperty("name").toString(), sequenceCurrent);

	                Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).getProperty("inputs")));
	                Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) nodeLayers.get(i).get(0).getProperty("outputs")));
	                n.setInputSet(inputSet);
	                n.setOutputSet(outputSet);
	                                
	                double[] qos = new double[4];
	                qos[0] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightTime").toString());
	                qos[1] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightCost").toString());
	                qos[2] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightAvailibility").toString());
	                qos[3] = Double.parseDouble(nodeLayers.get(i).get(0).getProperty("weightReliability").toString());

	                n.setQos(qos);

	    			sequenceCurrent.addChild(n);
	    		}
	    		// else if the number of nodes in current layer > 1
				// create a parallel node with all the nodes in current layer as its children
	    		else if (nodeLayers.get(i).size() > 1) {
	    			parallelCounter++;
	    			TreeNode parallel = new ParallelNode("Parallel", sequenceCurrent);
	    			int childCounter = 0; // counter to retrieve all the children in the List<Node> from NodeLayers map

	    			for (Node ch : nodeLayers.get(i)) {
	    				TreeNode child = new TerminalTreeNode(ch.getProperty("name").toString(), parallel);

		                Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) ch.getProperty("inputs")));
		                Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) ch.getProperty("outputs")));
		                child.setInputSet(inputSet);
		                child.setOutputSet(outputSet);

		                double[] qos = new double[4];
		                qos[0] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightTime").toString());
		                qos[1] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightCost").toString());
		                qos[2] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightAvailibility").toString());
		                qos[3] = Double.parseDouble(nodeLayers.get(i).get(childCounter).getProperty("weightReliability").toString());

		                child.setQos(qos);

		                parallel.getInputSet().addAll(inputSet);
		                parallel.getOutputSet().addAll(outputSet);

	    				parallel.addChild(child);
	    				childCounter++;
	    			}
	    			sequenceCurrent.addChild(parallel);

	    		}
	    		tree.add(sequenceCurrent);
	    		previous = sequenceCurrent; // set previous to current (for next iteration)

	    	}

	       transaction.success();
	       updateTreeInputOutput(tree.get(0));
	       //printTree(tree.get(0));
	    } catch (Exception e) {
	        transaction.failure();
	        System.out.println(e);
	    } finally {
	        transaction.close();
	    }

    	if (tree.size() == 0) {
    		System.out.println(nodeLayers.size());
    		System.out.println("looop   "+loopCounter);
    		System.out.println("parallel     "+parallelCounter);
    		System.out.println("rand     "+random);

    	
    	}
    	
    	return tree.get(0);
	}


    /**
     * This method was the old buildLayers which used the database result from Jacky's work (used with the old graphToTree method)
     * 
     * Only use this if we want to build layers from the database (not for GP)
     */
    @Deprecated
	private static Map<Integer, List<Node>> buildLayers(GenerateDatabase generateDatabase2, GraphDatabaseService newGraphDatabaseService, List<Node> graphNodes) {
		Transaction transaction = newGraphDatabaseService.beginTx();

        Map<Integer, List<Node>> nodeLayers = new TreeMap<Integer, List<Node>>();

        // initialize the map and create an empty list for each potential layer
        for (int i = 0; i < graphNodes.size(); i++) {
        	nodeLayers.put(new Integer(i), new ArrayList<Node>());
        }

        try {
	        // returns an algorithm for finding all paths between two nodes
			PathFinder<Path> allPathsAlgorithm = GraphAlgoFactory.allPaths(Traversal.pathExpanderForAllTypes(Direction.OUTGOING), graphNodes.size());

			Node startNode = null;

            Iterable<Node> nodes = newGraphDatabaseService.getAllNodes();

	        for(Node node: nodes) {
	            if (node.getProperty("name").equals("start")) {
	            	startNode = node;
	            	break; // break when we find the start node
	            }
	        }

	        // for each node in the composition
	        // find all paths between start and current node and store the length of the longest path
			for (Node n : nodes) {
				int maxLength = 0;
				Iterable<Path> paths = allPathsAlgorithm.findAllPaths(startNode, n);
				for(Path p : paths) {
					if (p.length() > maxLength) {
						maxLength = p.length();
					}
				}

				nodeLayers.get(maxLength).add(n); //store the current node and it's max length
			}

			List<Integer> layersToRemove = new ArrayList<Integer>();
		
			// find and mark empty layers (if they exist)
			// 14th FEB Problem: This is now causing problems because of recursive web services
			// so we may end up with layers looking like: 0, 2, 3, 4, 5, 6, 9, 10, 11
			// so the problem is that there are missing layers, e.g. 1, 7, and 8 (in the above)
	
		    for (Entry<Integer, List<Node>> e : nodeLayers.entrySet()) {
			    	if (e.getValue().isEmpty()) {
			    		layersToRemove.add(e.getKey());
			    	}
		    }

		    // remove the empty layers
		    for (Integer layer : layersToRemove) {
		    	nodeLayers.remove(layer);
		    }
		    
		    // iterate over the nodelayers and shuffle layers down to fill up missing space - because of the problem described in the comments on lines above ("14th Feb Problem")
		    int i = 0;
		    for (Entry<Integer, List<Node>> e : nodeLayers.entrySet()) {
		    	nodeLayers.put(new Integer(i), e.getValue());
		    	
		    	i++;
		    }
			
	        transaction.success();
        } catch (Exception e) {
            transaction.failure();
        } finally {
            transaction.close();
        }

        return nodeLayers;

    }
	


/*
	*//**
	 * OLD Method
	 *
	 * Converts a graph to tree. This version has redundancy and is the common (I think) approach found in literature.
	 *
	 * @param generateDatabase
	 * @param newGraphDatabaseService
	 * @param graphNodes
	 * @param dbPath
	 *//*
	private static void graphToTreeOld(GenerateDatabase generateDatabase, GraphDatabaseService newGraphDatabaseService, List<Node> graphNodes, String dbPath) {
        //GraphDatabaseService gs = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));
        Map<Node, TreeNode> nodeToTreeNodeMap = new HashMap<Node, TreeNode>();

        List<TreeNode> toVisit = new ArrayList<TreeNode>();
        List<TreeNode> tree = new ArrayList<TreeNode>();

        List<String> visited = new ArrayList<String>();

        Transaction transaction = newGraphDatabaseService.beginTx();

        try {
            Iterable<Node> nodes = newGraphDatabaseService.getAllNodes();
            //build a map of nodes to corresponding tree nodes
            for(Node node: nodes) {
                nodeToTreeNodeMap.put(node, new TreeNode(node.getProperty("name").toString(), null));
            }

            //find the start node and add the corresponding treenode to the toVisit list
            for(Node node: nodes) {
                if (node.getProperty("name").equals("start")) {
                    toVisit.add(nodeToTreeNodeMap.get(node)); // add the start node to the toVisit list
                    break; // break when we find the start node
                }
            }

            while (!(toVisit.isEmpty())) {
                // while we still have nodes to visit
                TreeNode currentNode = toVisit.get(0);
                toVisit.remove(0);
                if (visited.contains(currentNode.getName())) {
                    continue;
                }
                //String[] outputServices = generateDatabase.getInputOutputServicesForSubGraph(currentNode, graphNodes, "outputServices", newGraphDatabaseService);
                //Optional<Node> correspondingNode = findCorrespondingNode(currentNode, nodeToTreeNodeMap);
                Node correspondingNode = findCorrespondingNode(currentNode, nodeToTreeNodeMap);

                Iterable<Relationship> relationships = correspondingNode.getRelationships(Direction.OUTGOING);

                int numOfChildren = 0;
                for (Relationship r : relationships) {
                    numOfChildren++;
                }

                if (currentNode.getName().equals("start")) { ::TODO perhaps put this outside
                    if (numOfChildren == 1) { // create a sequence node
                        currentNode.setType("Sequence");
                    }
                    else { // create a parallel node
                        currentNode.setType("Parallel");
                    }
                }

                // check if the end node is in the current node's children and remove if true
                for (Relationship r : relationships) {
                    if (r.getProperty("To").toString().equals("end")) {
                        numOfChildren--;
                        break;
                    }
                }

            	TreeNode parallel = new TreeNode("", null);

                // for each child, create a tree node and add it
                for (Relationship r : relationships) {
                    if (currentNode.getName().equals("start")) {
                        if (numOfChildren == 1) { // create a sequence node
                            TreeNode n = new TreeNode(r.getProperty("To").toString(), currentNode);
                            currentNode.addChild(new TreeNode(currentNode.getName(), currentNode));    // current becomes sequence
                            currentNode.setName("Sequence");
                            // currentNode.addChild(currentNode); maybe you don't want to add the start node to it's children
                            currentNode.addChild(n); // add the child to the current node's children
                            toVisit.add(n); // add the child to the toVisit list
                        }
                        else { // create a parallel node
                            TreeNode n = new TreeNode(r.getProperty("To").toString(), currentNode);
                            currentNode.addChild(n); // add the child to the current node's children
                            toVisit.add(n); // add the child to the toVisit list
                        }
                    }
                    else {
                        if (numOfChildren == 1) {
                        	TreeNode newNode = new TreeNode(currentNode.getName(), currentNode);
                            Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) correspondingNode.getProperty("inputs")));
                            Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) correspondingNode.getProperty("outputs")));
                            newNode.setInputSet(inputSet);
                            newNode.setOutputSet(outputSet);

                            currentNode.addChild(newNode);
                        	//currentNode.addChild(new TreeNode(currentNode.getName().toString(), currentNode));
                        	currentNode.setName("Sequence");
                        	TreeNode n2 = new TreeNode(r.getProperty("To").toString(), currentNode);
                        	currentNode.addChild(n2);
                        	toVisit.add(n2);


                        	TreeNode n = new TreeNode("Sequence", currentNode.getParent());
                        	TreeNode n2 = new TreeNode(r.getProperty("To").toString(), n);
                        	n.addChild(n2);
                        	currentNode.getParent().addChild(n);
                        	toVisit.add(n2);
                        	tree.add(n);


                        	TreeNode n = new TreeNode(r.getProperty("To").toString(), currentNode);
                            currentNode.addChild(new TreeNode(currentNode.getName(), currentNode));
                            currentNode.setName("Sequence");
                            currentNode.addChild(n);
                            toVisit.add(n);
                        }
                        else if (numOfChildren > 1) {
                            currentNode.addChild(new TreeNode(currentNode.getName(), currentNode));
                            currentNode.setName("Sequence"); // current becomes sequence with current as one of its children
                            parallel.setName("Parallel");
                            parallel.setParent(currentNode);

                            for (Relationship r2 : relationships) {
                            	TreeNode n = new TreeNode(r2.getProperty("To").toString(), parallel);
                            	parallel.addChild(n);
                            	toVisit.add(n);
                            }
                            currentNode.addChild(parallel);
                            break;
                        }
                        else { // this is a leaf node
                            TreeNode n = new TreeNode(r.getProperty("To").toString(), currentNode).getParent();
                        }
                    }
                }

                //if (correspondingNode.isPresent()) {
                    //Object obj = correspondingNode.get().getProperty("outputServices");
                    //Object obj = correspondingNode.getProperty("outputServices");

                    //String[] outputsArray = getOutputArray(obj); // retrieve current node's outputs
                    //System.out.println("CurrentNode:      "+currentNode.getName());

                    // for each child, create a tree node and add it ::TODO remove this, I have better way of finding children using
 *                    // r.getProperty()
                    for (int i = 0; i < outputsArray.length; i++) {
                        TreeNode n = new TreeNode(outputsArray[i], currentNode);
                        currentNode.addChild(n); // add the child to the current node's children
                        toVisit.add(n); // add the child to the toVisit list
                        //System.out.println("CHILD:      "+outputsArray[i]);
                    }
                //}


                Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) correspondingNode.getProperty("inputs")));
				//inputs = getNodePropertyArray(correspondingNode, "inputServices");
				System.out.println("========  "+currentNode.getName()+"    =======");
				for (String s : inputSet) {
					System.out.println("-----------------------------------------         " +s);
				}

                if (currentNode.getName().equals("start") && numOfChildren > 1) {
                	currentNode.setName("Parallel");
                }

                Set<String> inputSet = new HashSet<String>(Arrays.asList((String[]) correspondingNode.getProperty("inputs")));
                Set<String> outputSet = new HashSet<String>(Arrays.asList((String[]) correspondingNode.getProperty("outputs")));
                currentNode.setInputSet(inputSet);
                currentNode.setOutputSet(outputSet);


                tree.add(currentNode);
                if (!(parallel.getName().equals(""))) {
                    tree.add(parallel);
                }
                visited.add(currentNode.getName());
                currentNode.setVisited(true);


                //System.out.println("+++++++++++++++   "+currentNode.getProperty("outputServices").toString());
            }

        transaction.success();
        updateTreeInputOutput(tree.get(0));
        printTree(tree.get(0));
        } catch (Exception e) {
            transaction.failure();
        } finally {
            transaction.close();
        }
    }*/

	
	

}