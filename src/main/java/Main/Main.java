package Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
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
public class Main implements Runnable{
	private static String serviceFileName = null;
	private static String taxonomyFileName = null;
	private static String taskFileName = null;

	private final static String Neo4j_testServicesDBPath = "database/test_services";
	private final static String Neo4j_ServicesDBPath = "database/";
	private final static String newResultDBPath = "database/result/";

	private boolean running = true;
	private Map<String,Long>records = new HashMap<String,Long>();
	private Map<Integer, Map<String,String>> bestResults = new HashMap<Integer, Map<String, String>>();
	private Map<Integer, Double> bestResultsTimes = new HashMap<Integer,Double>();
	private static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	private static Map<String, Node> subGraphNodesMap = new HashMap<String, Node>();;
	private static GraphDatabaseService graphDatabaseService = null;
	private static GraphDatabaseService subGraphDatabaseService = null;
	private static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	private static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	private static IndexManager index = null;
	@SuppressWarnings("unused")
	private static Index<Node> services;
	@SuppressWarnings("unused")
	private static Index<Node> tempServices;
	private static Node endNode = null;
	private static Node startNode = null;
	private static Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();
	private static String databaseName = "";
	public static LoadFiles loadFiles = null;
	//For setup == file location, composition size, and run test file or not
	//******************************************************//
	private final boolean runTestFiles = false;
	private final static String year = "2008";
	private final static String dataSet = "01";
	private final static int individuleNodeSize = 12;
	public static int candidateSize = 50;
	private final boolean runQosDataset = true;
	private final boolean runMultipileTime = false;
	private final int timesToRun = 30;

	private final static double m_a = 0.15;
	private final static double m_r = 0.05;
	private final static double m_c = 0.05;
	private final static double m_t = 0.75;

	//******************************************************//

	public static TreeNode rootNode; // TODO:: static not good here because it means root is always the same??
	private static int dbCounter = 0;
	public static boolean shouldParseFiles = true;
	
	public static void main( String[] args ) throws IOException, OuchException {
		setupDatabase();
	}


    private static TreeNode setupDatabase() throws IOException, OuchException {
		Main neo4jwsc = new Main();
		dbCounter++;


		//		Thread t = new Thread(neo4jwsc,"Neo4jThread");
		//		t.start();
		databaseName = "wsc"+year+"dataset"+dataSet;
		String path;
		if(!neo4jwsc.runTestFiles){
			if(!neo4jwsc.runQosDataset){
				serviceFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/services.xml";
				taxonomyFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/wsc"+year+"/Set"+dataSet+"MetaData/problem.xml";
			}else{
				serviceFileName = "dataset/Dataset/Set"+dataSet+"MetaData/services-output.xml";
				taxonomyFileName = "dataset/Dataset/Set"+dataSet+"MetaData/taxonomy.xml";
				taskFileName = "dataset/Dataset/Set"+dataSet+"MetaData/problem.xml";
			}
		}else{
			serviceFileName = "dataset/test/test_serv.xml";
			taxonomyFileName = "dataset/test/test_taxonomy.xml";
			taskFileName = "dataset/test/test_problem.xml";
		}
		//load files
		long startTime = System.currentTimeMillis();
		loadFiles();
		long endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Load files", endTime - startTime);
		System.out.println("Load files Total execution time: " + (endTime - startTime) );


		//populateTaxonomytree
		startTime = System.currentTimeMillis();
		populateTaxonomytree(); // Algorithm 1
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("Populate Taxonomy Tree", endTime - startTime);
		System.out.println("Populate Taxonomy Tree total execution time: " + (endTime - startTime) );


		//Identify run test file or not
		//if database exist read database
		//else if database not exist create database
		if(!neo4jwsc.runTestFiles){
			boolean dbExist;
			File f = new File(Neo4j_ServicesDBPath+""+databaseName +"/index");
			if (f.exists() && f.isDirectory()) {
				dbExist = true;
			}else{
				dbExist = false;
			}
			if(dbExist){
				startTime = System.currentTimeMillis();
				loadExistingDB();
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Load existing db", endTime - startTime);
				System.out.println("Load existing db Total execution time: " + (endTime - startTime) );

			}else{

				startTime = System.currentTimeMillis();
				neo4jwsc.generateDB(null,Neo4j_ServicesDBPath,"original",databaseName+dbCounter);
				endTime = System.currentTimeMillis();
				neo4jwsc.records.put("Create new db", endTime - startTime);
				System.out.println("Create new db Total execution time: " + (endTime - startTime) );

			}
			path = Neo4j_ServicesDBPath+""+databaseName+dbCounter;
		}
		else{
			startTime = System.currentTimeMillis();
			try {
				FileUtils.deleteRecursively(new File(Neo4j_testServicesDBPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			neo4jwsc.generateDB(null,Neo4j_testServicesDBPath,"original test", null);
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("Create new test db", endTime - startTime);
			System.out.println("Create new test db Total execution time: " + (endTime - startTime) );

			path = Neo4j_testServicesDBPath;
		}



		//run task
		//1: copy database and call->tempServiceDatabase
		//2: connect to tempServiceDatabase
		//3: use task inputs outputs create start and end nodes and link to tempservicedatabase
		startTime = System.currentTimeMillis();
		runTask(path); 
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("run task: copied db, create temp db, add start and end nodes", endTime - startTime);
		System.out.println("run task: copied db, create temp db, add start and end nodes Total execution time: " + (endTime - startTime) );



		//reduce database use copied database
		startTime = System.currentTimeMillis();
		reduceDB(); // Algorithm 4: Reduce Graph Database
		endTime = System.currentTimeMillis();
		neo4jwsc.records.put("reduce graph db ", endTime - startTime);
		System.out.println("reduce graph db Total execution time: " + (endTime - startTime) );


		// FIND COMPOSITIONS - Algorithm 5?
		if(!neo4jwsc.runMultipileTime){
			//find compositions
			startTime = System.currentTimeMillis();
			Map<List<Node>, Map<String,Map<String, Double>>> resultWithQos =findCompositions();
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("generate candidates", endTime - startTime);
			System.out.println();
			System.out.println("generate candidates Total execution time: " + (endTime - startTime) );
			System.out.println();
			System.out.println();
			System.out.println();

			startTime = System.currentTimeMillis();
			printResult(resultWithQos);
			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("generate best result", endTime - startTime);
			System.out.println();
			System.out.println("generate best result Total execution time: " + (endTime - startTime) );
			System.out.println();
			System.out.println();
			System.out.println();

			// resultWithQos.size() is 50 becuase candidateSize is 50 (maybe this is initial population size?)
			// do I begin performing GP from this point once I have my candidates?

			// create a new DB containing the resulting composition
			//::TODO I probably don't want to find the best result - rather just keep population
			// the findCompositions() method on line 193 automatically returns the best (QoS) composition
			startTime = System.currentTimeMillis();
			int newDBCounter = 0; //::TODO remoev this later and from line 228, it was just to create a lot of small databases with different compositions

			// helper method to see how the result is computed ::TODO remove
/*			try {
				printCandidates(resultWithQos);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}*/

			for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : resultWithQos.entrySet()) {
				newDBCounter++;
				try { //::TODO maybe enable this later? NOTE: i've enabled it for now - used to be disabled
					FileUtils.deleteRecursively(new File(newResultDBPath+dbCounter));
				} catch (IOException e) {
					e.printStackTrace();
				}
				//				generateDB(entry.getKey(),newResultDBPath,"result db", null);
				GenerateDatabase generateDatabase2 = new GenerateDatabase(entry.getKey(), subGraphDatabaseService, newResultDBPath+dbCounter);
				generateDatabase2.createDbService();
				GraphDatabaseService newGraphDatabaseService = generateDatabase2.getGraphDatabaseService();
				registerShutdownHook(graphDatabaseService,"original test");
				generateDatabase2.setServiceMap(serviceMap);
				generateDatabase2.setTaxonomyMap(taxonomyMap);
				generateDatabase2.createServicesDatabase();
				//				System.out.println("findCompositions.getBestRels()"+bestRels);
				//				generateDatabase2.set(bestRels);
				generateDatabase2.addServiceNodeRelationShip();
				removeRedundantRel(newGraphDatabaseService, generateDatabase2);


				// find out how to access this new non-redundant database
				Map<Integer, List<Node>> nodeLayers = buildLayers(generateDatabase2, newGraphDatabaseService, entry.getKey());
				//graphToTreeOld(generateDatabase2, newGraphDatabaseService, entry.getKey(), newResultDBPath+newDBCounter);
				rootNode = graphToTree(nodeLayers, generateDatabase2, newGraphDatabaseService, entry.getKey(), newResultDBPath+newDBCounter);
				registerShutdownHook(subGraphDatabaseService,"Reduced");
				registerShutdownHook(newGraphDatabaseService, "Result");
			}

			endTime = System.currentTimeMillis();
			neo4jwsc.records.put("create new result graph db ", endTime - startTime);
			System.out.println("create new result graph db Total execution time: " + (endTime - startTime) );


		}
		// run 30 times and find best composition from each iteration
		// I probably want to set runMultipileTime to false so that the if statement above is executed.
		// This way the "initial population" is generated and from there I can begin using GP to evolve
		// the solutions
		else {
			int count  = 0;
			while(count <neo4jwsc.timesToRun) {
				//find compositions
				startTime = System.currentTimeMillis();
				FindCompositions findCompositions = new FindCompositions(candidateSize, individuleNodeSize, subGraphDatabaseService);
				findCompositions.setStartNode(startNode);
				findCompositions.setEndNode(endNode);
				findCompositions.setNeo4jServNodes(neo4jServNodes);
				findCompositions.setTaxonomyMap(taxonomyMap);
				findCompositions.setSubGraphNodesMap(subGraphNodesMap);
				findCompositions.setM_a(m_a);
				findCompositions.setM_r(m_r);
				findCompositions.setM_c(m_c);
				findCompositions.setM_t(m_t);
				Map<List<Node>, Map<String,Map<String, Double>>> candidates = findCompositions.run();
				
				// pick out a random candidate
				Random random = new Random();
				List<List<Node>> keys = new ArrayList<List<Node>> (candidates.keySet());
				List<Node> randomkey = keys.get(random.nextInt(keys.size()));
				Map<String,Map<String, Double>> value = candidates.get(randomkey);		
				Map<List<Node>, Map<String,Map<String, Double>>> randomResultWithQos = new HashMap<List<Node>, Map<String,Map<String, Double>>>();
				randomResultWithQos.put(randomkey, value);
				
				//24 JAN Update: getResult() seems like it slows down execution. Rather than finding best (which takes time), we just choose a random one (above). 
				//Map<List<Node>, Map<String,Map<String, Double>>> resultWithQos = findCompositions.getResult(candidates);
				//		bestRels = findCompositions.getBestRels();

				System.out.println("Best result"+ count+": ");
				Transaction tx = subGraphDatabaseService.beginTx();


				try{
/*					System.out.println("====================================      "+resultWithQos.size());
					for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry2 : resultWithQos.entrySet()){*/
					System.out.println("====================================      "+randomResultWithQos.size());
					for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry2 : randomResultWithQos.entrySet()){
						String services = "";
						for(Node n: entry2.getKey()){
							System.out.print(n.getProperty("name")+"--"+n.getId()+"   ");
							services += n.getProperty("name")+"  ";
						}
						String qos = "";
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
								qos = entry3.getValue().get("A")+" "+entry3.getValue().get("R")+" "+entry3.getValue().get("T")+" "+entry3.getValue().get("C");
							}


						}
						Map<String, String> result = new HashMap<String,String>();
						result.put(qos, services);
						neo4jwsc.bestResults.put(count, result);
					}
					System.out.println();
				} catch (Exception e) {
					System.out.println(e);
					System.out.println("print populations error..");
				} finally {
					tx.close();
				}
				endTime = System.currentTimeMillis();
				neo4jwsc.bestResultsTimes.put(count, (double) (endTime - startTime));
				neo4jwsc.records.put("generate best result", endTime - startTime);
				System.out.println();
				System.out.println("generate best result Total execution time: " + (endTime - startTime) );
				System.out.println();
				System.out.println();
				System.out.println();
				count++;
			}

			if(neo4jwsc.runTestFiles){
				FileWriter fw = new FileWriter("test-dataset-bestResults.stat");
				for(Entry<Integer,Map<String, String>> entry : neo4jwsc.bestResults.entrySet()){
					for(Entry<String,String> entry2: entry.getValue().entrySet()){
						fw.write(neo4jwsc.bestResultsTimes.get(entry.getKey())+"\n");
						fw.write(entry2.getKey()+ "\n");
						fw.write(entry2.getValue()+ "\n");
					}

				}
				fw.close();
			}
			else{
				FileWriter fw = new FileWriter("evaluationNeo4jResults/"+year+"-dataset"+dataSet+".stat");
				for(Entry<Integer,Map<String, String>> entry : neo4jwsc.bestResults.entrySet()){
					for(Entry<String,String> entry2: entry.getValue().entrySet()){
						fw.write(neo4jwsc.bestResultsTimes.get(entry.getKey())+"\n");
						fw.write(entry2.getKey()+ "\n");
						fw.write(entry2.getValue()+ "\n");
					}

				}
				fw.close();
			}
		}
		TaxonomyNode tNode = taxonomyMap.get("inst1716616603").parentNode;
		tNode.getParent();
		System.out.println("inst1716616603 parents: ");

		for(String s: tNode.childrenString){
//			System.out.println(s);
//			TaxonomyNode ttNode = taxonomyMap.get(s);
//			for(String c: ttNode.childrenString){
				if(s.equals("inst927259823"))
					System.out.print("inst927259823");
				if(s.equals("inst608977925"))
					System.out.print("inst608977925");
				if(s.equals("inst885068313"))
					System.out.print("inst885068313");
				if(s.equals("inst1420249694"))
					System.out.print("inst1420249694");
				if(s.equals("inst1488043421"))
					System.out.print("inst1488043421");
//			}
			//  inst927259823,inst608977925 inst885068313,inst1420249694,inst1488043421
		}
		FileWriter fw = new FileWriter("timeRecord.txt");
		for(Entry<String, Long> entry : neo4jwsc.records.entrySet()){
			fw.write(entry.getKey()+"    " +entry.getValue()+ "\n");
		}
		fw.close();
		neo4jwsc.setRunning(false);
		return rootNode;
	}


    private static TreeNode graphToTree(Map<Integer, List<Node>> nodeLayers, GenerateDatabase generateDatabase, GraphDatabaseService newGraphDatabaseService, List<Node> graphNodes, String dbPath) {
    	Transaction transaction = newGraphDatabaseService.beginTx();
        List<TreeNode> tree = new ArrayList<TreeNode>();

    	TreeNode previous = null; // a node to store the sequence node from previous iteration

    	try {
	    	// iterate over the layers in reverse
	    	for (int i = nodeLayers.size()-1; i >= 0; i--) {
	    		TreeNode sequenceCurrent = new SequenceNode("Sequence", null);

	    		if (previous != null) {
	    			if (nodeLayers.get(i).get(0).getProperty("name").toString().equals("start")) {
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
	    } finally {
	        transaction.close();
	    }

    	return tree.get(0);
	}


	private static Map<Integer, List<Node>> buildLayers(GenerateDatabase generateDatabase2, GraphDatabaseService newGraphDatabaseService, List<Node> graphNodes) {
		Transaction transaction = newGraphDatabaseService.beginTx();

        Map<Integer, List<Node>> nodeLayers = new HashMap<Integer, List<Node>>();

        // initialize the map and create an empty list for each potential layer
        for (int i = 0; i < graphNodes.size(); i++) {
        	nodeLayers.put(i, new ArrayList<Node>());
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
		    for (Entry<Integer, List<Node>> e : nodeLayers.entrySet()) {
			    	if (e.getValue().isEmpty()) {
			    		layersToRemove.add(e.getKey());
			    	}
		    }

		    // remove the empty layers
		    for (Integer layer : layersToRemove) {
		    	nodeLayers.remove(layer);
		    }

/*		    System.out.println("--------------------DEBUGGING-------------------");
		    for (Map.Entry<Integer, List<Node>> e : nodeLayers.entrySet()) {
			    System.out.println("LAYER   "+e.getKey());
			    for (Node n : e.getValue()) {
			    	System.out.println(n.getProperty("name").toString());
			    }
		    }*/

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
		Transaction tx = subGraphDatabaseService.beginTx();


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
		FindCompositions findCompositions = new FindCompositions(candidateSize, individuleNodeSize, subGraphDatabaseService);
		findCompositions.setStartNode(startNode);
		findCompositions.setEndNode(endNode);
		findCompositions.setNeo4jServNodes(neo4jServNodes);
		findCompositions.setTaxonomyMap(taxonomyMap);
		findCompositions.setSubGraphNodesMap(subGraphNodesMap);
		findCompositions.setM_a(m_a);
		findCompositions.setM_r(m_r);
		findCompositions.setM_c(m_c);
		findCompositions.setM_t(m_t);
		Map<List<Node>, Map<String, Map<String, Double>>> candidates = null;
		try {
			candidates = findCompositions.run();
		} catch (OuchException e1) {
			e1.printStackTrace();
		}

		Transaction transaction = subGraphDatabaseService.beginTx();
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
			transaction.close();
		}
		 return findCompositions.getResult(candidates); //::TODO this returns the best candidate but maybe I just want all the candidates to perform GP on
		// TODO: maybe I should only return 1 (i.e. the above)? but then is that a good thing? maybe takes more time to find best, maybe I just want to return a random one?
		//return candidates;
	}


	private static void reduceDB() {
		ReduceGraphDb reduceGraphDb = new ReduceGraphDb(graphDatabaseService);
		reduceGraphDb.setStartNode(startNode);
		reduceGraphDb.setEndNode(endNode);
		reduceGraphDb.setNeo4jServNodes(neo4jServNodes);
		reduceGraphDb.setTaxonomyMap(taxonomyMap);
		reduceGraphDb.setServiceMap(serviceMap);

		Set<Node> relatedNodes = new HashSet<Node>();;
		reduceGraphDb.findAllReleatedNodes(relatedNodes, false);
		System.out.println(relatedNodes.size());
//		Set<Node>toRemove = new HashSet<Node>();
//		for(Node node: relatedNodes){
//			Transaction transaction = graphDatabaseService.beginTx();
//			if(!node.getProperty("name").equals("start")){
//				if(!isNodeFulfilled(node, relatedNodes, graphDatabaseService)){
//					toRemove.add(node);
//				}
//			}
//			transaction.close();
//		}
//		System.out.println(	"tomove: "+			toRemove.size());

		reduceGraphDb.createNodes(relatedNodes, dbCounter);
		reduceGraphDb.createRel();
		relatedNodes = reduceGraphDb.getRelatedNodes();
		startNode = reduceGraphDb.getStartNode();
		endNode = reduceGraphDb.getEndNode();
		subGraphDatabaseService = reduceGraphDb.getSubGraphDatabaseService();
		subGraphNodesMap = reduceGraphDb.getSubGraphNodesMap();
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
	private static void runTask(String path) {

		RunTask runtask = new RunTask(path);
		runtask.setServiceNodes(serviceNodes);
		runtask.setTaxonomyMap(taxonomyMap);
		runtask.setServiceNodes(serviceNodes);
		runtask.setTaskInputs(loadFiles.getTaskInputs());
		runtask.setTaskOutputs(loadFiles.getTaskOutputs());
		runtask.copyDb(dbCounter);
		runtask.createTempDb(dbCounter);
		graphDatabaseService = runtask.getTempGraphDatabaseService();
		registerShutdownHook(graphDatabaseService, "Temp");
		neo4jServNodes.clear();
		neo4jServNodes = runtask.getNeo4jServNodes();
		tempServices = runtask.getTempServices();
		runtask.addStartEndNodes();
		startNode = runtask.getStartNode();
		endNode = runtask.getEndNode();
		runtask.createRel(startNode);
		runtask.createRel(endNode);
	}


	private void generateDB(List<Node> nodes, String dbpath, String string, String databaseName) {

		String path = "";
		if(databaseName == null){
			path = dbpath;
		}else{
			path = dbpath+""+databaseName;
		}
		GenerateDatabase generateDatabase = new GenerateDatabase(null, null,path);
		generateDatabase.createDbService();
		graphDatabaseService = generateDatabase.getGraphDatabaseService();
		registerShutdownHook(graphDatabaseService, string);
		generateDatabase.setServiceMap(serviceMap);
		generateDatabase.setTaxonomyMap(taxonomyMap);
		generateDatabase.createServicesDatabase();
		generateDatabase.addServiceNodeRelationShip();
		neo4jServNodes.clear();
		neo4jServNodes = generateDatabase.getNeo4jServNodes();
		Transaction transaction = graphDatabaseService.beginTx();
		try{
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );
			transaction.success();
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Main set index and services error..");
		} finally {
			transaction.close();
		}
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

	@SuppressWarnings("deprecation")
	private static void loadExistingDB() {
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(Neo4j_ServicesDBPath+""+databaseName+dbCounter));
		
		registerShutdownHook(graphDatabaseService, "exist original");
		Transaction transaction = graphDatabaseService.beginTx();
		index = graphDatabaseService.index();
		services = index.forNodes( "identifiers" );
		transaction.success();
		transaction.close();
		signNodesToField(neo4jServNodes, graphDatabaseService);
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


	private static void loadFiles() {
		if (shouldParseFiles) {
			loadFiles = new LoadFiles(serviceFileName,taxonomyFileName, taskFileName);
			loadFiles.runLoadFiles();
			taxonomyMap = loadFiles.getTaxonomyMap();
			serviceMap = loadFiles.getServiceMap();
			//		neo4jwsc.taskInputs = loadFiles.getTaskInputs();
			loadFiles.getTaskOutputs();
			serviceNodes = loadFiles.getServiceNodes();
		}
		else {
			loadFiles.getTaskOutputs();
		}
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

	public void run() {
		while (running) {
			System.out.println(new Date() + " ### Neo4jService working.....！");
			try {
				Thread.sleep(20000);
			} catch (InterruptedException e) {
				System.out.println(e);
			}
		}

	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	private static void registerShutdownHook(GraphDatabaseService graphDatabaseService,String database ) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running example before it's completed)
		Runtime.getRuntime()
		.addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				System.out.println("neo4j graph database shutdown hook ("+database+")... ");
				graphDatabaseService.shutdown();
			}
		} );
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

	// list of nodes in comp -> (normalize/non-normal -> (A/R/C/T -> Value))
	private static void printCandidates(Map<List<Node>, Map<String, Map<String, Double>>> resultWithQos) throws InterruptedException {
		int count = 0;
		for (Map.Entry<List<Node>, Map<String,Map<String, Double>>> entry : resultWithQos.entrySet()) {
			//System.out.println(entry.getKey());
			for (Map.Entry<String, Map<String, Double>> entry2 : entry.getValue().entrySet()) {
				//System.out.println(entry2.getKey());
				for (Map.Entry<String, Double> entry3 : entry2.getValue().entrySet()) {
					System.out.println(entry3.getKey()+"  >>>>>>>>>>>>>>>>>>>   "+ entry3.getValue());

				}


			}



			count++;
		}

		System.out.println(count);

		TimeUnit.MINUTES.sleep(50);

	}


}