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
import java.util.List;
import java.util.Map;
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

import me.tongfei.progressbar.ProgressBar;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;

public class Test {
	private static String pathPlugins  = "C:/Users/login/work/pleiades/eclipse/plugins";
	private static String pathProject  = "C:/Users/login/data/1_task/20200421_094917/projects/inferBugs/datasets/egit/raw";
	private static String idCommitMethodFrom = "e47f0c1c1390a956f5f8b19e62bb933c614492fc";
	private static String idCommitMethodTo   = "f020b5381b96f3a2dde3d748cd43283d0968acf5";
	private static String idCommitFileFrom   = "dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b";
	private static String idCommitFileTo     = "0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7";

	private static String pathRepositoryFile = pathProject+"/repositoryFile";
	private static String pathRepositoryMethod = pathProject+"/repositoryMethod";
	private static String pathDataset = pathProject+"/datasets/"+idCommitMethodFrom.substring(0,8)+"_"+idCommitMethodTo.substring(0,8)+".csv";
	private static String pathSourceFiles = pathProject+"/sourceFiles.json";
	private static String pathCommits = pathProject+"/commits";
	private static String pathBugs = pathProject+"/bugs.json";

	private static HashMap<String, Record> records = new HashMap<String, Record>();
	private static HashMap<String, Commit> commits = new HashMap<>();
	private static HashMap<String, SourceFile> sourceFiles = new HashMap<>();
	private static HashMap<String, HashMap<String,String[]>> bugs = new HashMap<String, HashMap<String,String[]>>();

	public static void main(String[] args) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
		loadFiles();
		setRepository();
		setEmptyRecords();
		getCodeMetrics();
		//getProcessMetrics();
    	saveDataset();
	}

	private static void getHasBeenIntroduced() {
	}

	private static void getHasBeenFixed() {
	}

	private static void getIsBuggy(String path, String idCommit) {	}

	private static void setRepository() throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		//repositoryFileについて、そのidCommitへcheckout。
        Git git = Git.open(new File(pathRepositoryFile));
        git.checkout().setName(idCommitFileTo).call();
	}

	private static void setEmptyRecords() {
		Path rootDir = Paths.get(pathRepositoryFile);
		try {
			List<String> paths =Files.walk(rootDir)
					.map(Path::toString)
					.filter(p -> p.endsWith(".java"))
					.filter(p -> !p.contains("test"))
					.collect(Collectors.toList());
			for(String path: paths) {
				records.put(path, new Record());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void loadFiles() {
		//commits
		File fileCommits = new File(pathCommits);
		List<File> filesCommit = Arrays.asList(fileCommits.listFiles());
		for(File file: ProgressBar.wrap(filesCommit, "loadFiles")) {
			String path =file.toString();
			try {
				String strCommit=readAll(path);
				ObjectMapper mapper = new ObjectMapper();
				Commit commit = mapper.readValue(strCommit, new TypeReference<Commit>() {});
				commits.put(commit.id,commit);
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
			String strSourceFile=readAll(pathSourceFiles);
			ObjectMapper mapper = new ObjectMapper();
			sourceFiles = mapper.readValue(strSourceFile, new TypeReference<HashMap<String, SourceFile>>() {});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//bugs
		try {
			String strBugs=readAll(pathBugs);
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
			for(String key: records.keySet()) {
				Record record=records.get(key);
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

	private static void getCodeMetrics() {
		final String[] sourcePathDirs = {};
		final String[] libs = getLibraries(pathRepositoryFile);
		String[] sources = new String[records.size()];
		records.keySet().toArray(sources);

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final Map<String,String> options = JavaCore.getOptions();
		//caution. to calculateIDMethod http://www.nextdesign.co.jp/tips/tips_eclipse_jdt.html
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		parser.setEnvironment(libs, sourcePathDirs, null, true);

		String[] keys = new String[] {""};
		Requestor requestor = new Requestor();
		parser.createASTs(sources, null, keys, requestor, new NullProgressMonitor());
		records=requestor.records;

		for(String pathCalled: requestor.methodsCalled) {
			for(String path: records.keySet()) {
				if(path.equals(pathCalled)) {
					records.get(path).fanIN++;
				}
			}
		}
	}
/*
	private static void getProcessMetrics() {
		getIsBuggy();
		getHasBeenFixed();
		getHasBeenIntroduced();

		History tmp=null;
		List<Commit> commits=null;
		for(String path: records.keySet()) {
			Record record = records.get(path);
			System.out.println(count+ "/" + records.keySet().size());
			if(history==null)continue;
			HashSet<Commit> nodes=new HashSet<Commit>();
			HashMap<String, ArrayList<String>> edges= new HashMap<String, ArrayList<String>>();
			HashMap<String, ArrayList<String>> edgesReverse= new HashMap<String, ArrayList<String>>();
			ArrayList<String[]> toBeSearched = new ArrayList<String[]>();
			String[] tmp0=new String[2];
			tmp0[0]=path;
			tmp0[1]=history.commits.get(history.commits.size()-1).id;
			toBeSearched.add(tmp0);
			while(0<toBeSearched.size()) {
				History historyTmp=historiesAllfile.get(toBeSearched.get(0)[0]);
				for(int i=historyTmp.commits.size()-1; 0<=i; i--) {
					if(historyTmp.commits.get(i).id.equals(toBeSearched.get(0)[1])) {
						for(int j=i;0<=j;j--) {
							Commit commit=historyTmp.commits.get(j);
							if(commit.type==1 | commit.type==2) {
								String[] tmp1=new String[2];
								tmp1[0]=commit.pathOld;
								tmp1[1]=commit.id;
								toBeSearched.add(tmp1);
							    edgesReverse.put(commit.id, historyTmp.commit2Childs.get(commit.id));
							}else {
							    nodes.add(commit);
							    edges.put(commit.id, historyTmp.commit2Parents.get(commit.id));
							    if(!edgesReverse.keySet().contains(commit.id))edgesReverse.put(commit.id, historyTmp.commit2Childs.get(commit.id));
							}
						}
						break;
					}
				}
				toBeSearched.remove(0);
			}

			commits=new ArrayList<Commit>();


			int periodFrom = Integer.MAX_VALUE;
			for(Commit commit: nodes) {
				if(commit.date<periodFrom) {
					periodFrom=commit.date;
				}
			}
			int periodTo = dateUntil;
			record.period = (periodTo - periodFrom)/(60*60*24);

			//コミットパスを考慮しない
			for(Commit commit: nodes) {
				if(dateFrom<commit.date
						& commit.date<dateUntil
						& !commit.isMerge
						) {
					commits.add(commit);
				}
			}


			commits.sort((a,b)->a.date-b.date);

			getA();
		}
	}


	private static void getA(){
		Record record = new Record();
		ArrayList<String> authors = new ArrayList<String>();

		//try {
			int methodHistories=0;
			int pastBugNum=0;
			int fixChgNum=0;
			int bugIntroNum=0;
			int logCoupNum=0;
			ArrayList<Integer> stmtAddeds = new ArrayList<Integer>();
			ArrayList<Integer> stmtDeleteds = new ArrayList<Integer>();
			ArrayList<Integer> churns = new ArrayList<Integer>();
			ArrayList<Integer> decls = new ArrayList<Integer>();
			ArrayList<Integer> conds = new ArrayList<Integer>();
			ArrayList<Integer> elseAddeds = new ArrayList<Integer>();
			ArrayList<Integer> elseDeleteds = new ArrayList<Integer>();

			List<String> statements = Arrays.asList("AssertStatement","BreakStatement","ConstructorInvocation","ContinueStatement","DoStatement","EnhancedForStatement", "ExpressionStatement","ForStatement","IfStatement","ReturnStatement","SuperConstructorInvocation","SwitchStatement","ThrowStatement","TryStatement","WhileStatement");
			List<String> operatorsCondition = Arrays.asList("<", ">", "<=", ">=", "==", "!=", "^", "&", "|", "&&", "||");

			String sourcePrev =  null;
			String sourceCurrent =null;


			for(int i=0;i<commits.size();i++) {
				if(0<i && StringUtils.equals(commits.get(i-1).sourceNew, commits.get(i).sourceNew))continue;
				String strPre;
				String strPost;
				if(commits.get(i).sourceOld==null) {
			        String tmp=commits.get(i).sourceNew;
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
					sourcePrev= "public class Test{"+commits.get(i).sourceOld+"}";
					sourceCurrent ="public class Test{"+commits.get(i).sourceNew+"}";
				}

				FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
				try {
				    distiller.extractClassifiedSourceCodeChanges(sourcePrev, sourceCurrent);
				} catch(Exception e) {
				    System.err.println("Warning: error while change distilling. " + e.getMessage());
				}

				List<SourceCodeChange> changes = distiller.getSourceCodeChanges();
				if(0 < changes.size()) {
					int stmtAdded=0;
					int stmtDeleted=0;
					int churn=0;
					int decl=0;
					int cond=0;
					int elseAdded=0;
					int elseDeleted=0;
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
				    for(SourceCodeChange change : changes) {
				    	EntityType et = change.getChangedEntity().getType();
				    	if(change.getChangeType()==ChangeType.STATEMENT_INSERT)stmtAdded++;
				    	else if(change.getChangeType()==ChangeType.STATEMENT_DELETE)stmtDeleted++;
				    	else if(ctdecl.contains(change.getChangeType()))decl++;
				    	else if(change.getChangeType()==ChangeType.CONDITION_EXPRESSION_CHANGE)cond++;
				    	else if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_INSERT & et.toString().equals("ELSE_STATEMENT") )elseAdded++;
				    	else if(change.getChangeType()==ChangeType.ALTERNATIVE_PART_DELETE & et.toString().equals("ELSE_STATEMENT"))elseDeleted++;
				    }
					churn=stmtAdded-stmtDeleted;
					methodHistories++;
					if(commits.get(i).bugFix!=null)pastBugNum++;

					authors.add(commits.get(i).author);
					stmtAddeds.add(stmtAdded);
					stmtDeleteds.add(stmtDeleted);
					churns.add(churn);
					decls.add(decl);
					conds.add(cond);
					elseAddeds.add(elseAdded);
					elseDeleteds.add(elseDeleted);
					//if(commits.get(i).bugFix!=null)method.isBuggy=1;
				}else {
					continue;
				}
			}
			if(methodHistories==0)return;
			Set<String> authorsSet = new HashSet<String>(authors);
			record.methodHistories=methodHistories;
			record.devTotal=authorsSet.size();
			record.stmtAdded=stmtAddeds.stream().mapToInt(e->e).sum();
			record.maxStmtAdded=stmtAddeds.stream().mapToInt(e->e).max().getAsInt();
			record.avgStmtAdded=record.stmtAdded/(float)methodHistories;
			record.stmtDeleted=stmtDeleteds.stream().mapToInt(e->e).sum();
			record.maxStmtDeleted=stmtDeleteds.stream().mapToInt(e->e).max().getAsInt();
			record.avgStmtDeleted=record.stmtDeleted/(float)methodHistories;
			record.churn=churns.stream().mapToInt(e->e).sum();
			record.maxChurn=churns.stream().mapToInt(e->e).max().getAsInt();
			record.avgChurn=record.churn/(float)methodHistories;
			record.decl=decls.stream().mapToInt(e->e).sum();
			record.cond=conds.stream().mapToInt(e->e).sum();
			record.elseAdded=elseAddeds.stream().mapToInt(e->e).sum();
			record.elseDeleted=elseDeleteds.stream().mapToInt(e->e).sum();
			record.pastBugNum=pastBugNum;
			record.fixChgNum=fixChgNum;
			record.bugIntroNum=bugIntroNum;
			record.logCoupNum=logCoupNum;
			record.avgInterval = record.period / (float)record.methodHistories;
			if(2<=commits.size()) {
			    int minInterval=Integer.MAX_VALUE;
			    int maxInterval=Integer.MIN_VALUE;
			        for(int i=0;i<commits.size()-1;i++) {
				        int interval=commits.get(i+1).date-commits.get(i).date;
				        if(maxInterval < interval) {
					        maxInterval = interval;
				        }
				        if(interval < minInterval) {
    				    	minInterval = interval;
				        }
			        }
    			record.minInterval=minInterval/(60*60*24);
			    record.maxInterval=maxInterval/(60*60*24);
			}
			record.devTotal=authorsSet.size();
			for(Iterator<String> iterator = authorsSet.iterator(); iterator.hasNext(); ) {
				String author=iterator.next();
				float count=0;
				for(int i=0;i<authors.size();i++) {
					if(author.equals(authors.get(i))) {
						count++;
					}
				}
				float ratio=count/authors.size();
				if(record.ownership<ratio) {
					record.ownership=ratio;
				}
				if(0.20<ratio) {
					record.devMajor++;
				}else {
					record.devMinor++;
				}
			}
			record.isBuggy = 0;//commits.get(commits.size()-1).isBuggy? 1:0;
			for(Commit commit: commits) {
				/*
				if(commit.bugFix!=null) {
					method.isBuggy = 1;
				}
				*/
/*
				if(0 < commit.bugIntro.size()) {
					String strRelease2Date=readAll(pathRelease2Date);
					String strRevision2Date=readAll(pathRevision2Date);
					ObjectMapper mapper = new ObjectMapper();
					HashMap<String, Integer> release2Date=null;
					HashMap<String, Integer> revision2Date=null;
					try {
						release2Date=mapper.readValue(strRelease2Date, new TypeReference<HashMap<String, Integer>>() {});
						revision2Date=mapper.readValue(strRevision2Date, new TypeReference<HashMap<String, Integer>>() {});
					} catch (JsonParseException e) {
						e.printStackTrace();
					} catch (JsonMappingException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					int count=0;
					int allFiles=records.size();
					int dateRelease=release2Date.get(Integer.toString(release));


					for(String id: commit.bugIntro) {
					    if(commit.date <release2Date.get(Integer.toString(release))
							    & release2Date.get(Integer.toString(release))< revision2Date.get(id)) {
				            record.isBuggy = 1;
					    }
					}
				}


			}
	}
*/

	private static String[] getLibraries(String pathFile) {
		Path[] dirs = new Path[]{
				Paths.get(pathPlugins),
				Paths.get(pathFile)
		};
		List<String> classes=new ArrayList<String>();
		try {
			for(int i=0;i<dirs.length;i++) {
				classes.addAll(Files.walk(dirs[i])
						.map(Path::toString)
						.filter(p -> p.endsWith(".jar"))
						.collect(Collectors.toList()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes.toArray(new String[classes.size()]);
	}


	public static String readAll(final String path){
		String value=null;
	    try {
	    	value = Files.lines(Paths.get(path), Charset.forName("UTF-8")).collect(Collectors.joining(System.getProperty("line.separator")));
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			return value;
		}
	}
}