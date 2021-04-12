package main.java;

import data.*;
import data.Bugs;
import data.Commits;
import data.ModulesAll;
import data.ModulesTarget;
import main.java.misc.ArgBean;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Main {
	public static void main(String[] args){
		ArgBean bean = new ArgBean();
		CmdLineParser parser = new CmdLineParser(bean);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("usage:");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		final String pathProject = bean.pathProject;
		final String[] commitEdgesMethod = bean.commitEdgesMethod;
		final String[] commitEdgesFile = bean.commitEdgesFile;

		String pathRepositoryMethod = pathProject+"/repositoryMethod";
		String pathRepositoryFile = pathProject+"/repositoryFile";
		String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+".csv";
		String pathModules = pathProject+"/modules.json";
		String pathCommits = pathProject+"/commits";
		String pathBugs = pathProject+"/bugs.json";

		Commits commitsAll = new Commits();
		ModulesAll modulesAll = new ModulesAll();
		Bugs bugsAll = new Bugs();

		try {
			commitsAll.loadCommits(pathCommits);
			modulesAll.loadModules(pathModules);
			bugsAll.loadBugs(pathBugs);
			ModulesTarget modulesTarget = new ModulesTarget();
			modulesTarget.identifyTargetModules(modulesAll, pathRepositoryMethod, commitEdgesMethod);
			modulesTarget.calcCodeMetrics(pathRepositoryFile, commitEdgesFile, pathRepositoryMethod, commitEdgesMethod);
			modulesTarget.calcProcessMetrics(commitsAll, bugsAll, commitEdgesMethod);
    	    modulesTarget.saveMetricsAsRecords(pathDataset);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}