import data.*;

public class Main {
	private static String   pathProject   = "C:/Users/ShoOgino/data/1_task/20200421_094917/projects/MLTool/datasets/egit";
	private static String[] commitEdges = {"e47f0c1c1390a956f5f8b19e62bb933c614492fc", "1241472396d11fe0e7b31c6faf82d04d39f965a6"};

	private static String pathRepository = pathProject+"/repository";
	private static String pathDataset = pathProject+"/datasets/"+ commitEdges[0].substring(0,8)+"_"+ commitEdges[1].substring(0,8)+".csv";
	private static String pathModules = pathProject+"/modules.json";
	private static String pathCommits = pathProject+"/commits";
	private static String pathBugs = pathProject+"/bugs.json";

	private static Commits commitsAll = new Commits();
	private static ModulesAll modulesAll = new ModulesAll();
	private static Bugs bugsAll = new Bugs();

	public static void main(String[] args){
		try {
			commitsAll.loadCommits(pathCommits);
			modulesAll.loadModules(pathModules);
			bugsAll.loadBugs(pathBugs);
			ModulesTarget modulesTarget = new ModulesTarget();
			modulesTarget.identifyTargetModules(pathRepository, commitEdges);
		    modulesTarget.calcCodeMetrics(modulesAll, pathRepository, commitEdges);
		    modulesTarget.calcProcessMetrics(modulesAll, commitsAll, bugsAll, commitEdges);
    	    modulesTarget.saveMetricsAsRecords(pathDataset);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}