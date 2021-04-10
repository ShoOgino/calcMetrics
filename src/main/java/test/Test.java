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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
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
	private static String   pathProject           = "C:\\Users\\ShoOgino\\data\\1_task\\20200421_094917\\projects\\MLTool\\datasets\\egit";
	private static String[] intervalCommitMethod = {"e47f0c1c1390a956f5f8b19e62bb933c614492fc", "1241472396d11fe0e7b31c6faf82d04d39f965a6"};

	private static String pathRepositoryMethod = pathProject+"\\repositoryMethod";
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
			setEmptyRecords(pathRepositoryMethod, intervalCommitMethod[1]);
		    //calcCodeMetrics(pathRepositoryFile, intervalCommitFile[1], "sourcefile");
		    calcCodeMetricsTest(pathRepositoryMethod, intervalCommitMethod, "method");
		    calcProcessMetrics(intervalCommitMethod, "method");
    	    saveRecords();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void calcCodeMetricsTest(String pathRepositoryFile, String[] intervalCommit, String guranurality) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
		checkoutRepository(pathRepositoryMethod, intervalCommit[1]);
		for(String pathModule: ProgressBar.wrap(recordsAll.keySet(), "calcCodeMetrics")){
			Record record = recordsAll.get(pathModule);
			Module module = modulesAll.get(pathModule);
			//record.fanIN        = calcFanIN(module, intervalCommit);
			record.fanOut       = calcFanOut(module, intervalCommit);
			record.parameters   = calcParameters(module, intervalCommit);
			record.localVar     = calcLocalVar(module, intervalCommit);
			record.commentRatio = calcCommentRatio(module, intervalCommit);
			record.countPath    = calcCountPath(module, intervalCommit);
			record.complexity   = calcComplexity(module, intervalCommit);
			record.execStmt     = calcExecStmt(module, intervalCommit);
			record.maxNesting   = calcMaxNesting(module, intervalCommit);
		}
	}

	private static int calcMaxNesting(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorMaxNesting visitorMaxNesting = new VisitorMaxNesting();
		unit.accept(visitorMaxNesting);
		return visitorMaxNesting.maxNesting;
	}

	private static int calcExecStmt(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorExecStmt visitorExecStmt = new VisitorExecStmt();
		unit.accept(visitorExecStmt);
		return visitorExecStmt.execStmt;
	}

	private static int calcComplexity(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorComplexity visitorComplexity = new VisitorComplexity();
		unit.accept(visitorComplexity);
		return visitorComplexity.complexity;
	}

	private static long calcCountPath(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorCountPath visitorCountPath = new VisitorCountPath();
		unit.accept(visitorCountPath);
		long countPath=1;
		for(int branch: visitorCountPath.branches) {
			countPath*=branch;
		}
		return countPath;
	}

	private static double calcCommentRatio(Module module, String[] intervalCommit) {		String regex  = "\n|\r\n";
		String sourceMethod = readFile(pathRepositoryMethod+"/"+module.path);
		String[] linesMethod = sourceMethod.split(regex, 0);

		int countLineCode=0;
		int countLineComment=0;
		boolean inComment=false;
		for(String line:linesMethod) {
		    countLineCode++;
		    if(line.matches(".*\\*/\\S+")) {
    			inComment=false;
		    }else if(line.matches(".*\\*/\\s*")) {
    			inComment=false;
		    	countLineComment++;
		    }else if(inComment) {
    		    countLineComment++;
		    }else if(line.matches("\\S+/\\*.*")){
		    	inComment=true;
		    }else if(line.matches("\\s*/\\*.*")){
		    	countLineComment++;
		    	inComment=true;
		    }else if(line.matches("\\S+//.*")) {
			}else if(line.matches("\\s*//.*")) {
				countLineComment++;
			}
		}
	   	return (float) countLineComment/ (float)countLineCode;
	}

	private static int calcLocalVar(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorLocalVar visitorLocalVar = new VisitorLocalVar();
		unit.accept(visitorLocalVar);
		return visitorLocalVar.NOVariables;
	}

	private static CompilationUnit getCompilationUnit(Module module) {
		String sourceMethod = readFile(pathRepositoryMethod+"/"+module.path);
		String sourceClass = "public class Dummy{"+sourceMethod+"}";
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(sourceClass.toCharArray());
		CompilationUnit unit =(CompilationUnit) parser.createAST(new NullProgressMonitor());
		return unit;
	}

	private static int calcParameters(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorMethodDeclaration visitorMethodDeclaration = new VisitorMethodDeclaration();
		unit.accept(visitorMethodDeclaration);
		return visitorMethodDeclaration.parameters;
	}

	private static int calcFanOut(Module module, String[] intervalCommit) {
		CompilationUnit unit =getCompilationUnit(module);
		VisitorFanout visitor = new VisitorFanout();
		unit.accept(visitor);
		return visitor.fanout;
	}

	private static List<String> findFiles(String dirRoot, String ext, String extIgnore) {
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
		return pathsFile;
	}

	private static List<String> findFiles(String[] dirsRoot, String ext, String extIgnore) {
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
		return pathsFile;
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
		git.reset().setMode(ResetCommand.ResetType.HARD).call();
		git.clean().call();
        git.checkout().setName(idCommit).call();
	}

	private static void setEmptyRecords(String pathRepository, String idCommit) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
		checkoutRepository(pathRepository, idCommit);
		List<String> pathSources = findFiles(pathRepositoryMethod, ".mjava", "test");
		for(String pathSource: ProgressBar.wrap(pathSources, "setEmptyRecords")) {
			String prefix = pathRepositoryMethod+"\\";
		    int index = pathSource.indexOf(prefix);
		    String pathModule = pathSource.substring(index+prefix.length()).replace("\\", "/");
		    Record record = new Record();
		    record.path=pathModule;
			recordsAll.put(pathModule, record);
		}
	}

	//コミット、モジュール、バグについてまとめたファイルをロードする。
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
		//module
		try {
			String strModule=readFile(pathSourceFiles);
			ObjectMapper mapper = new ObjectMapper();
			modulesAll = mapper.readValue(strModule, new TypeReference<HashMap<String, Module>>() {});
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

	//算出されたレコードをファイルに保存する。
    private static void saveRecords() {
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


	//全ファイルについて、プロセスメトリクスを算出する。
	private static void calcProcessMetrics(String[] intervalCommit, String granurarity) {
		for(String pathModule: ProgressBar.wrap(recordsAll.keySet(), "calcProcessMetrics")){
			Record record = recordsAll.get(pathModule);
			Module module = modulesAll.get(pathModule);
			record.moduleHistories= calcModuleHistories(module, intervalCommit);
			record.authors       = calcDevTotal(module, intervalCommit);
			record.stmtAdded      = calcStmtAdded(module, intervalCommit);
			record.maxStmtAdded   = calcMaxStmtAdded(module, intervalCommit);
			record.avgStmtAdded   = calcAvgStmtAdded(module, intervalCommit);
			record.stmtDeleted    = calcStmtDeleted(module, intervalCommit);
			record.maxStmtDeleted = calcMaxStmtDeleted(module, intervalCommit);
			record.avgStmtDeleted = calcAvgStmtDeleted(module, intervalCommit);
			record.churn          = calcChurn(module, intervalCommit);
			record.maxChurn       = calcMaxChurn(module, intervalCommit);
			record.avgChurn       = calcAvgChurn(module, intervalCommit);
			record.decl           = calcDecl(module, intervalCommit);
			record.cond           = calcCond(module, intervalCommit);
			record.elseAdded      = calcElseAdded(module, intervalCommit);
			record.elseDeleted    = calcElseDeleted(module, intervalCommit);
			record.isBuggy        = calcIsBuggy(module, intervalCommit);
		}
	}

	private static int calcCond(Module module, String[] intervalCommit) {
		int cond=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
		    	if(change.getChangeType()==ChangeType.CONDITION_EXPRESSION_CHANGE)cond++;
			}
		}
		return cond;
	}

	private static int calcDecl(Module module, String[] intervalCommit) {
		int decl=0;
		List<ChangeType> ctdecl= Arrays.asList(
				ChangeType.METHOD_RENAMING,
				ChangeType.PARAMETER_DELETE,
				ChangeType.PARAMETER_INSERT,
				ChangeType.PARAMETER_ORDERING_CHANGE,
				ChangeType.PARAMETER_RENAMING,
				ChangeType.PARAMETER_TYPE_CHANGE,
				ChangeType.RETURN_TYPE_INSERT,
				ChangeType.RETURN_TYPE_DELETE,
				ChangeType.RETURN_TYPE_CHANGE,
				ChangeType.PARAMETER_TYPE_CHANGE
				);
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
		    	if(ctdecl.contains(change.getChangeType()))decl++;
			}
		}
		return decl;
	}

	private static double calcAvgChurn(Module module, String[] intervalCommit) {
		int avgChurn=calcChurn(module, intervalCommit)/calcModuleHistories(module, intervalCommit);
		return avgChurn;
	}

	private static int calcMaxChurn(Module module, String[] intervalCommit) {
		int maxChurn=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			int churnTemp=0;
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)churnTemp++;
		    	else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)churnTemp--;
			}
			if(maxChurn<churnTemp)maxChurn=churnTemp;
		}
		return maxChurn;
	}

	private static int calcChurn(Module module, String[] intervalCommit) {
		int churn=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)churn++;
		    	else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)churn--;
			}
		}
		return churn;
	}

	private static double calcAvgStmtDeleted(Module module, String[] intervalCommit) {
		int avgStmtDeleted=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_DELETE)avgStmtDeleted++;
			}
		}
		int moduleHistories=calcModuleHistories(module, intervalCommit);
		return avgStmtDeleted/(double)moduleHistories;
	}

	private static int calcMaxStmtDeleted(Module module, String[] intervalCommit) {
		int maxStmtDeleted=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			int stmtDeletedOnCommit=0;
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeletedOnCommit++;
			}
			if(maxStmtDeleted<stmtDeletedOnCommit) {
				maxStmtDeleted=stmtDeletedOnCommit;
			}
		}
		return maxStmtDeleted;
	}

	private static int calcStmtDeleted(Module module, String[] intervalCommit) {
		int stmtDeleted=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeleted++;
			}
		}
		return stmtDeleted;
	}

	private static double calcAvgStmtAdded(Module module, String[] intervalCommit) {
		int avgStmtAdded=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)avgStmtAdded++;
			}
		}
		int moduleHistories=calcModuleHistories(module, intervalCommit);
		return avgStmtAdded/(double)moduleHistories;
	}

	private static int calcMaxStmtAdded(Module module, String[] intervalCommit) {
		int maxStmtAdded=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			int stmtAddedTemp=0;
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAddedTemp++;
			}
			if(maxStmtAdded<stmtAddedTemp) {
				maxStmtAdded=stmtAddedTemp;
			}
		}
		return maxStmtAdded;
	}

	private static List<SourceCodeChange> calcChanges(CommitOnModule commitOnModule){
		String idCommit = commitOnModule.id;
		String pathNew  = commitOnModule.pathNew;
		String pathOld  = commitOnModule.pathOld;
		Modification modification = commitsAll.get(idCommit).modifications.stream().filter(item->item.pathNew.equals(pathNew)&item.pathOld.contentEquals(pathOld)).findFirst().get();
		String sourcePrev =  null;
		String sourceCurrent =null;
		String strPre;
		String strPost;
		if(modification.sourceOld==null) {
	        String tmp=modification.sourceNew;
	        Pattern patternPre = Pattern.compile("[\\s\\S.]*?(?=\\{)");
	        Matcher matcherPre = patternPre.matcher(tmp);
	        if(matcherPre.find()) {
			    strPre=matcherPre.group();
	        }else {
	        	strPre="";
	        }
		    Pattern patternPost = Pattern.compile("(?<=\\{)[\\s\\S.]*");
		    Matcher matcherPost = patternPost.matcher(tmp);
		    if(matcherPost.find()) {
		    	strPost=matcherPost.group();
		    }else {
		    	strPost="}";
		    }
			sourcePrev= "public class Test{"+strPre+
					"{"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
			    	"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"}"+
					"}";
			sourceCurrent ="public class Test{"+strPre+
					"{"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
			        "token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					"token();\n"+
					strPost+
					"}";
		}else {
			sourcePrev= "public class Test{"+modification.sourceOld+"}";
			sourceCurrent ="public class Test{"+modification.sourceNew+"}";
		}

		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		try {
		    distiller.extractClassifiedSourceCodeChanges(sourcePrev, sourceCurrent);
		} catch(Exception e) {
		    System.err.println("Warning: error while change distilling. " + e.getMessage());
		}
		return distiller.getSourceCodeChanges();
	}

	private static int calcStmtAdded(Module module, String[] intervalCommit) {
		int stmtAdded=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
		    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAdded++;
			}
		}
		return stmtAdded;
	}

	private static int calcElseDeleted(Module module, String[] intervalCommit) {
		int elseDeleted=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
		    	if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))elseDeleted++;
			}
		}
		return elseDeleted;
	}

	private static int calcElseAdded(Module module, String[] intervalCommit) {
		int elseAdded=0;
		List<CommitOnModule> commitOnModules = calcCommitOnModulesInInterval(module, intervalCommit);
		for(int i=0;i<commitOnModules.size();i++) {
			CommitOnModule commitOnModule = commitOnModules.get(i);
			List<SourceCodeChange> changes = calcChanges(commitOnModule);
			for(SourceCodeChange change : changes) {
			    EntityType et = change.getChangedEntity().getType();
			    if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT") )elseAdded++;
			}
		}
		return elseAdded;
	}

	private static int calcDevTotal(Module module, String[] intervalCommit) {
		Set<String> setAuthors = new HashSet<String>();
		List<Commit> commits =calcCommitsInInterval(module, intervalCommit);
		commits.stream().forEach(item->setAuthors.add(item.author));
		int authors=setAuthors.size();
		return authors;
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
			if(dateBegin<=commit.date & commit.date<=dateEnd) {
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
			if(dateBegin<=commit.date & commit.date<=dateEnd) {
				commitOnModules.add(module.idCommit2CommitOnModule.get(idCommit));
			}
		}

		return commitOnModules;
	}

	private static int calcIsBuggy(Module module, String[] intervalCommit) {
		HashMap<String, String[]> fix2induces = bugs.get(module.path);
		if(fix2induces==null)return 0;
		for(String idCommitFix: fix2induces.keySet()) {
			for(String idCommitInduce: fix2induces.get(idCommitFix)) {
				Commit commitFix = commitsAll.get(idCommitFix);
				Commit commitInduce = commitsAll.get(idCommitInduce);
				Commit commitTimePoint = commitsAll.get(intervalCommit[1]);
				if(commitInduce.date<commitTimePoint.date & commitTimePoint.date<commitFix.date)return 1;
			}
		}
		return 0;
	}
}