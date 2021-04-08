package test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import me.tongfei.progressbar.ProgressBar;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;

public class Test {
	private static String   pathProject           = "C:\\Users\\login\\data\\1_task\\20200421_094917\\projects\\MLTool\\datasets\\egit\\raw";
	private static String   pathRepositoryFile = pathProject+"\\repositoryFile";
	private static String   pathRepositoryMethod = pathProject+"\\repositoryMethod";
	private static String[] pathDirsLibrary = {pathRepositoryFile};
	private static String[] intervalCommitMethod = {"e47f0c1c1390a956f5f8b19e62bb933c614492fc", "f020b5381b96f3a2dde3d748cd43283d0968acf5"};
	private static String[] intervalCommitFile   = {"dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b","0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7"};

	private static String pathDataset = pathProject+"/datasets/"+intervalCommitMethod[0].substring(0,8)+"_"+intervalCommitMethod[1].substring(0,8)+".csv";
	private static String pathSourceFiles = pathProject+"/sourceFiles.json";
	private static String pathCommits = pathProject+"/commits";
	private static String pathBugs = pathProject+"/bugs.json";

	private static HashMap<String, Record> recordsAll = new HashMap<String, Record>();
	private static HashMap<String, Commit> commitsAll = new HashMap<>();
	private static HashMap<String, Module> modulesAll = new HashMap<>();
	private static HashMap<String, HashMap<String,String[]>> bugs = new HashMap<String, HashMap<String,String[]>>();

	public static void main(String[] args){
		try {
			loadFiles();
			setEmptyRecords();
		    calcCodeMetrics(pathRepositoryFile, intervalCommitFile[1], "sourcefile");
		    calcProcessMetrics(intervalCommitMethod, "method");
    	    saveDataset();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static String[] findFiles(String dirRoot, String ext, String extIgnore) {
		List<String> pathsFile = new ArrayList<String>();
		try {
			pathsFile.addAll(
					Files.walk(Paths.get(dirRoot))
					.map(Path::toString)
					.filter(p -> p.endsWith(ext))
					.filter(p -> !p.contains(extIgnore))
					.collect(Collectors.toList())
					);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pathsFile.toArray(new String[pathsFile.size()]);
	}

	private static String[] findFiles(String[] dirsRoot, String ext, String extIgnore) {
		List<String> pathsFile = new ArrayList<String>();
		try {
			for(String dirRoot: dirsRoot) {
				pathsFile.addAll(
					Files.walk(Paths.get(dirRoot))
					.map(Path::toString)
					.filter(p -> p.endsWith(ext))
					.filter(p -> !p.contains(extIgnore))
					.collect(Collectors.toList())
					);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pathsFile.toArray(new String[pathsFile.size()]);
	}

	public static String readFile(final String path){
		String value=null;
	    try {
	    	value = Files.lines(Paths.get(path), Charset.forName("UTF-8")).collect(Collectors.joining(System.getProperty("line.separator")));
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			return value;
		}
	}

	private static void checkoutRepository(String pathRepository, String idCommit) throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		//repositoryについて、そのコミットidへcheckout。
        Git git = Git.open(new File(pathRepository));
        git.checkout().setName(idCommit).call();
	}

	private static void setEmptyRecords() {
		String[] pathSources = findFiles(pathRepositoryMethod, ".mjava", "test");
		for(String pathSource: pathSources) {
			recordsAll.put(pathSource, new Record());
		}
	}

	private static void loadFiles() {
		//commits
		File fileCommits = new File(pathCommits);
		List<File> filesCommit = Arrays.asList(fileCommits.listFiles());
		for(File file: ProgressBar.wrap(filesCommit, "loadFiles")) {
			String path =file.toString();
			try {
				String strCommit=readFile(path);
				ObjectMapper mapper = new ObjectMapper();
				Commit commit = mapper.readValue(strCommit, new TypeReference<Commit>() {});
				commitsAll.put(commit.id,commit);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//sourceFiles
		try {
			String strSourceFile=readFile(pathSourceFiles);
			ObjectMapper mapper = new ObjectMapper();
			modulesAll = mapper.readValue(strSourceFile, new TypeReference<HashMap<String, Module>>() {});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//bugs
		try {
			String strBugs=readFile(pathBugs);
			ObjectMapper mapper = new ObjectMapper();
			bugs = mapper.readValue(strBugs, new TypeReference<HashMap<String, HashMap<String, String[]>>>() {});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private static void saveDataset() {
		try {
			FileOutputStream fos= new FileOutputStream(pathDataset);
			OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			BufferedWriter writer = new BufferedWriter(osw);
			CsvConfiguration config = new CsvConfiguration();
			config.setFieldDelimiter(',');
			config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
			File csv = new File(pathDataset);
		    Serializer serializer = CsvIOFactory.createFactory(config, Record.class).createSerializer();

			serializer.open(writer);
			for(String key: recordsAll.keySet()) {
				Record record=recordsAll.get(key);
				serializer.write(record);
			}
			serializer.close(true);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	private static void calcCodeMetrics(String pathRepository, String idCommit, String granurarity) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
		checkoutRepository(pathRepository, idCommit);
		final String[] sourcePathDirs = {};
		final String[] libraries      = findFiles(pathDirsLibrary, ".jar", "");
		final String[] sources        = findFiles(pathRepositoryFile, ".java", "test");

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final Map<String,String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		parser.setEnvironment(libraries, sourcePathDirs, null, true);

		String[] keys = new String[] {""};
		Requestor requestor = new Requestor();
		parser.createASTs(sources, null, keys, requestor, new NullProgressMonitor());
		recordsAll=requestor.records;
		for(String idMethodCalled: requestor.methodsCalled) {
			for(String pathMethod: recordsAll.keySet()) {
				String id0 =recordsAll.get(pathMethod).id;
				String id1 = idMethodCalled;
				if(id0.equals(id1)) {
					recordsAll.get(pathMethod).fanIN++;
				}
			}
		}
	}

	//あるファイルについて、一通りのプロセスメトリクスを取得する。
	private static void calcProcessMetrics(String[] intervalCommit, String granurarity) {
		for(String pathModule: recordsAll.keySet()) {
			Module moduleTarget = modulesAll.get(pathModule);

			Record record = recordsAll.get(pathModule);
			record.moduleHistories= calcModuleHistories(moduleTarget, intervalCommit);
			record.devTotal       = calcDevTotal(moduleTarget, intervalCommit);
			record.devMajor       = calcDevMajor(moduleTarget, intervalCommit);
			record.devMinor       = calcDevMinor(moduleTarget, intervalCommit);
			record.ownership      = calcOwnership(moduleTarget, intervalCommit);
			record.elseAdded      = calcElseAdded(moduleTarget, intervalCommit);
			record.elseDeleted    = calcElseDeleted(moduleTarget, intervalCommit);
			//record.fixChgNum      = calcFixChgNum(moduleTarget, intervalCommit);
			//record.pastBugNum     = calcPastBugNum(moduleTarget, intervalCommit);
			//record.bugIntroNum    = calcBugIntroNum(moduleTarget, intervalCommit);
			//record.logCoupNum     = calcLogCoupNum(moduleTarget, intervalCommit);
			//record.period         = calcPeriod(moduleTarget, intervalCommit);
			//record.avgInterval    = calcAvgInterval(moduleTarget, intervalCommit);
			//record.maxInterval    = calcMaxInterval(moduleTarget, intervalCommit);
			//record.minInterval    = calcMinInterval(moduleTarget, intervalCommit);
			//record.stmtAdded      = calcStmtAdded(moduleTarget, intervalCommit);
			//record.maxStmtAdded   = calcMaxStmtAdded(moduleTarget, intervalCommit);
			//record.avgStmtAdded   = calcAvgStmtAdded(moduleTarget, intervalCommit);
			//record.stmtDeleted    = calcStmtDeleted(moduleTarget, intervalCommit);
			//record.maxStmtDeleted = calcMaxStmtDeleted(moduleTarget, intervalCommit);
			//record.avgStmtDeleted = calcAvgStmtDeleted(moduleTarget, intervalCommit);
			//record.churn          = calcChurn(moduleTarget, intervalCommit);
			//record.maxChurn       = calcMaxChurn(moduleTarget, intervalCommit);
			//record.avgChurn       = calcAvgChurn(moduleTarget, intervalCommit);
			//record.decl           = calcDecl(moduleTarget, intervalCommit);
			//record.cond           = calcCond(moduleTarget, intervalCommit);
		}
	}

	private static int calcPeriod(Module module, String[] intervalCommit) {
		return 0;
	}

	private static int calcElseDeleted(Module module, String[] intervalCommit) {
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		int elseDeleted=0;
		for(int i=0;i<commitOnModules.size();i++) {
			String idCommit = commitOnModules.get(i).id;
			String pathNew = commitOnModules.get(i).pathNew;
			String pathOld = commitOnModules.get(i).pathOld;
			Modification modification = commitsAll.get(idCommit).modifications.stream().filter(item->item.pathNew.equals(pathNew)&item.pathOld.contentEquals(pathOld)).findFirst().get();
			String sourceOld= "public class Test{"+modification.sourceOld+"}";
			String sourceNew ="public class Test{"+modification.sourceNew+"}";

			FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
			try {
			    distiller.extractClassifiedSourceCodeChanges(sourceOld, sourceNew);
			} catch(Exception e) {
			    System.err.println("Warning: error while change distilling. " + e.getMessage());
			}

			List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
		    	if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))elseDeleted++;
			}
		}
		return elseDeleted;
	}

	private static int calcElseAdded(Module module, String[] intervalCommit) {
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		int elseAdded=0;
		for(int i=0;i<commitOnModules.size();i++) {
			String idCommit = commitOnModules.get(i).id;
			String pathNew = commitOnModules.get(i).pathNew;
			String pathOld = commitOnModules.get(i).pathOld;
			Modification modification = commitsAll.get(idCommit).modifications.stream().filter(item->item.pathNew.equals(pathNew)&item.pathOld.contentEquals(pathOld)).findFirst().get();
			String sourceOld= "public class Test{"+modification.sourceOld+"}";
			String sourceNew ="public class Test{"+modification.sourceNew+"}";

			FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
			try {
			    distiller.extractClassifiedSourceCodeChanges(sourceOld, sourceNew);
			} catch(Exception e) {
			    System.err.println("Warning: error while change distilling. " + e.getMessage());
			}

			List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
			    if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT") )elseAdded++;
			}
		}
		return elseAdded;
	}

	private static double calcOwnership(Module module, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<String>();
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		List<String> authors = commits.stream().map(commit->commit.author).collect(Collectors.toList());
		float ownership=0;
		for(String author: setAuthors) {
			int count = (int)authors.stream().filter(item->item.equals(author)).count();
			if(ownership<count/(float)setAuthors.size()) {
				ownership=count/(float)setAuthors.size();
			}
		}
		return ownership;
	}

	private static int calcDevMinor(Module module, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<String>();
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		List<String> authors = commits.stream().map(commit->commit.author).collect(Collectors.toList());
		int devMinor=0;
		for(String author: setAuthors) {
			int count = (int)authors.stream().filter(item->item.equals(author)).count();
			if(count/(float)setAuthors.size()<0.20) {
				devMinor++;
			}
		}
		return devMinor;
	}

	private static int calcDevMajor(Module module, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<String>();
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		List<String> authors = commits.stream().map(commit->commit.author).collect(Collectors.toList());
		int devMajor=0;
		for(String author: setAuthors) {
			int count = (int)authors.stream().filter(item->item.equals(author)).count();
			if(0.20<count/(float)setAuthors.size()) {
				devMajor++;
			}
		}
		return devMajor;
	}

	private static int calcDevTotal(Module module, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<String>();
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		int devTotal=setAuthors.size();
		return devTotal;
	}

	private static int calcModuleHistories(Module module, String[] intervalCommit) {
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		int moduleHistories=commits.size();
		return moduleHistories;
	}

	private static List<Commit> calcCommitsInInterval(Module module, String[] intervalCommit){
		List<Commit> commits = new ArrayList<Commit>();

		int dateBegin = commitsAll.get(intervalCommit[0]).date;
		int dateEnd   = commitsAll.get(intervalCommit[1]).date;
		for(String idCommit:module.idCommit2CommitOnModule.keySet()) {
			Commit commit = commitsAll.get(idCommit);
			if(dateBegin<commit.date & commit.date<dateEnd) {
				commits.add(commit);
			}
		}

		return commits;
	}

	private static List<CommitOnModule> calcCommitOnModulesInInterval(Module module, String[] intervalCommit){
		List<CommitOnModule> commitOnModules = new ArrayList<>();

		int dateBegin = commitsAll.get(intervalCommit[0]).date;
		int dateEnd   = commitsAll.get(intervalCommit[1]).date;
		for(String idCommit:module.idCommit2CommitOnModule.keySet()) {
			Commit commit = commitsAll.get(idCommit);
			if(dateBegin<commit.date & commit.date<dateEnd) {
				commitOnModules.add(module.idCommit2CommitOnModule.get(idCommit));
			}
		}

		return commitOnModules;
	}

	private static void calcHasBeenIntroduced() {
	}

	private static void calcHasBeenFixed() {
	}

	private static void calcIsBuggy(String path, String idCommit) {
	}
}