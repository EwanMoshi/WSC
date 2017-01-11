package Main;

import generateDatabase.GenerateDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modellingServices.LoadFiles;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;

import task.ReduceGraphDb;
import task.RunTask;
import component.ServiceNode;
import component.TaxonomyNode;

public class Neo4jConnection {

	public static String serviceFileName = null;
	public static String taxonomyFileName = null;
	public static String taskFileName = null;
	
	public final static String Neo4j_testServicesDBPath = "database/test_services";
	public final static String Neo4j_ServicesDBPath = "database/";
	public final static String newResultDBPath = "database/result/";
	
	public static Map<String, Node> neo4jServNodes = new HashMap<String, Node>();
	public static Map<String, Node> subGraphNodesMap = new HashMap<String, Node>();;
	public static Map<String, ServiceNode> serviceMap = new HashMap<String, ServiceNode>();
	public static Map<String, TaxonomyNode> taxonomyMap = new HashMap<String, TaxonomyNode>();
	
	public static IndexManager index = null;
	public static Index<Node> services;
	public static Index<Node> tempServices;

	public static LoadFiles loadFiles = null;
	
	public static GraphDatabaseService graphDatabaseService = null;
	public static GraphDatabaseService subGraphDatabaseService = null;
	public static GraphDatabaseService graphDatabaseServiceTemp = null;

	public static Neo4jConnection dbConnection;
	
	public final static String year = "2008";
	public final static String dataSet = "01";
	public static String databaseName = "";

	public static Node endNode = null;
	public static Node startNode = null;
	
	public static RunTask runtask = null;
	public static ReduceGraphDb reduceGraphDb = null;
	
	public static Set<ServiceNode> serviceNodes = new HashSet<ServiceNode>();

	
	private Neo4jConnection() {}
	
	public static Neo4jConnection getInstance() {
		if (dbConnection == null) {
			synchronized (Neo4jConnection.class) {
				if (dbConnection == null) {
					dbConnection = new Neo4jConnection();
				}
			}
		}
		return dbConnection;
	}
	
	public void loadExistingDB() {
		databaseName = "wsc"+year+"dataset"+dataSet;
		//graphDatabaseService.shutdown();
		if (graphDatabaseService == null) {
			graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(Neo4j_ServicesDBPath+""+databaseName));
			registerShutdownHook(graphDatabaseService, "exist original");
			Transaction transaction = graphDatabaseService.beginTx();
			index = graphDatabaseService.index();
			services = index.forNodes( "identifiers" );
			transaction.success();
			transaction.close();
			signNodesToField(neo4jServNodes, graphDatabaseService);
		}
	}
	
	public void generateDB(List<Node> nodes, String dbpath, String string, String databaseName) {

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
	
	public void reduceDB() {
		if (reduceGraphDb == null) {
			reduceGraphDb = new ReduceGraphDb(graphDatabaseService);

			reduceGraphDb.setStartNode(startNode);
			reduceGraphDb.setEndNode(endNode);
			reduceGraphDb.setNeo4jServNodes(neo4jServNodes);
			reduceGraphDb.setTaxonomyMap(taxonomyMap);
			reduceGraphDb.setServiceMap(serviceMap);

			Set<Node> relatedNodes = new HashSet<Node>();;
			reduceGraphDb.findAllReleatedNodes(relatedNodes, false);
			System.out.println(relatedNodes.size());

			
			reduceGraphDb.createNodes(relatedNodes);
			reduceGraphDb.createRel();
			relatedNodes = reduceGraphDb.getRelatedNodes();
			startNode = reduceGraphDb.getStartNode();
			endNode = reduceGraphDb.getEndNode();
			subGraphDatabaseService = reduceGraphDb.getSubGraphDatabaseService();
			subGraphNodesMap = reduceGraphDb.getSubGraphNodesMap();
		}
	}
	
	public void runTask(String path) {
		if (runtask == null) {
			runtask = new RunTask(path);
			runtask.setServiceNodes(serviceNodes);
			runtask.setTaxonomyMap(taxonomyMap);
			runtask.setServiceNodes(serviceNodes);
			runtask.setTaskInputs(loadFiles.getTaskInputs());
			runtask.setTaskOutputs(loadFiles.getTaskOutputs());
			runtask.copyDb();
			runtask.createTempDb();
			graphDatabaseServiceTemp = runtask.getTempGraphDatabaseService();
			registerShutdownHook(graphDatabaseServiceTemp, "Temp");
			neo4jServNodes.clear();
			neo4jServNodes = runtask.getNeo4jServNodes();
			tempServices = runtask.getTempServices();
			runtask.addStartEndNodes();
			startNode = runtask.getStartNode();
			endNode = runtask.getEndNode();
			runtask.createRel(startNode);
			runtask.createRel(endNode);
		}
	}
	
	public static void loadFiles() {
		loadFiles = new LoadFiles(serviceFileName,taxonomyFileName, taskFileName);
		loadFiles.runLoadFiles();
		taxonomyMap = loadFiles.getTaxonomyMap();
		serviceMap = loadFiles.getServiceMap();
		//		neo4jwsc.taskInputs = loadFiles.getTaskInputs();
		loadFiles.getTaskOutputs();
		serviceNodes = loadFiles.getServiceNodes();
	}
	
	private static void registerShutdownHook(GraphDatabaseService graphDatabaseService, String database ) {
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
	
	
}
