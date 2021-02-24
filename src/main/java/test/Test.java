package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

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
import net.sf.jsefa.Deserializer;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;

public class Test {
	private static int release;
	private static String pathProject;
	private static String pathPlugins;


	private static String pathRepositoryFile;
	private static String pathDataset;
	private static String pathRevision2Date;
	private static String pathRelease2Date;
	private static String pathBugsAllfile;
	private static String pathCommitIntro2Path;
	private static String pathCommit2PathHasBeenBuggy;
	private static String pathCommit2IsFix;


	private static HashMap<String, History> historiesAllfile=new HashMap<String, History>();
	private static HashMap<String, Method> dataset = new HashMap<String, Method>();
	private static HashMap<String, HashMap<String,String[]>> bugAllfiles = new HashMap<String, HashMap<String,String[]>>();
	private static HashMap<String, String[]> commitIntro2Path=null;
	private static HashMap<String, String[]> commit2PathHasBeenBuggy=null;
	private static HashMap<String, Integer> commit2IsFix=null;


	public static void main(String[] args) {
		//release = Integer.parseInt(args[0]);
		//pathProject= args[1];
		release = 1;
		pathProject="C:\\Users\\login\\data\\1_task\\20200421_094917\\projects\\inferBugs\\datasets\\egit\\raw";
        pathPlugins = "C:\\Users\\login\\work\\pleiades\\eclipse\\plugins";

		pathRepositoryFile = pathProject+"/file"+release;
		pathDataset = pathProject+"/"+release+".csv";
		pathRevision2Date = pathProject + "/revision2Date.json";
		pathRelease2Date  = pathProject + "/release2Date.json";
		pathBugsAllfile  = pathProject + "/bugintros.json";
		pathCommitIntro2Path = pathProject + "/commitIntro2path.json";
		pathCommit2PathHasBeenBuggy = pathProject + "/commit2PathHasBeenBuggy.json";
		pathCommit2IsFix = pathProject + "/commitsFix.json";

		String strRelease2Date=readAll(pathRelease2Date);
		String strCommitIntro2Path=readAll(pathCommitIntro2Path);
		String strCommit2PathAlreadyBuggy=readAll(pathCommit2PathHasBeenBuggy);
		String strCommit2IsFix=readAll(pathCommit2IsFix);
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Integer> release2Date=null;
		/*
		try {
			loadFiles();
			loadBug();
			release2Date=mapper.readValue(strRelease2Date, new TypeReference<HashMap<String, Integer>>() {});
			commitIntro2Path=mapper.readValue(strCommitIntro2Path, new TypeReference<HashMap<String, String[]>>() {});
			commit2PathHasBeenBuggy=mapper.readValue(strCommit2PathAlreadyBuggy, new TypeReference<HashMap<String, String[]>>() {});
			commit2IsFix =mapper.readValue(strCommit2IsFix, new TypeReference<HashMap<String, Integer>>() {});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
*/
		dataset = new HashMap<String, Method>();
		loadDataset();
		getCodeMetrics(dataset);
		getProcessMetrics(dataset);
    	storeDataset();
	}

	private static void loadFiles() {
		File dir = new File(pathProject+"/testBugfixes");
		File[] list = dir.listFiles();
		int count=0;
		int NOFiles=list.length;
		for(File file: list) {
			System.out.println(count + "/" + NOFiles);
			String path =file.toString();
			System.out.println(path);
			try {
				String strFile=readAll(path);
				ObjectMapper mapper = new ObjectMapper();
				History history = mapper.readValue(strFile, new TypeReference<History>() {});
				historiesAllfile.put( history.path, history );
				count++;
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void loadBug() {
		try {
			String strInfoBug = readAll(pathBugsAllfile);
			ObjectMapper mapper = new ObjectMapper();
			bugAllfiles = mapper.readValue(strInfoBug, new TypeReference<HashMap<String, HashMap<String,String[]>>>() {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private static void loadDataset() {
		File file=new File(pathDataset);
		if (file.exists()) {
			try (FileInputStream fis = new FileInputStream(pathDataset);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
					BufferedReader reader = new BufferedReader(isr)){
				CsvConfiguration config = new CsvConfiguration();
				config.setFieldDelimiter(',');
				config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
				Deserializer deserializer = CsvIOFactory.createFactory(config, Method.class).createDeserializer();
				deserializer.open(reader);
				while (deserializer.hasNext()) {
					Method m = deserializer.next();
				    dataset.put(m.path, m);
				}
				deserializer.close(true);
			} catch (JsonParseException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }

    private static void storeDataset() {
		try {
			FileOutputStream fos= new FileOutputStream(pathDataset);
			OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			BufferedWriter writer = new BufferedWriter(osw);
			CsvConfiguration config = new CsvConfiguration();
			config.setFieldDelimiter(',');
			config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
			File csv = new File(pathDataset);
		    Serializer serializer = CsvIOFactory.createFactory(config, Method.class).createSerializer();

			serializer.open(writer);
			for(String key: dataset.keySet()) {
				Method method=dataset.get(key);
				serializer.write(method);
			}
			serializer.close(true);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter datetimeformatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		String datetimeformated = datetimeformatter.format(now);

		try {
			FileOutputStream fos= new FileOutputStream(pathDataset+datetimeformated);
			OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
			BufferedWriter writer = new BufferedWriter(osw);
			CsvConfiguration config = new CsvConfiguration();
			config.setFieldDelimiter(',');
			config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
			File csv = new File(pathDataset);
		    Serializer serializer = CsvIOFactory.createFactory(config, Method.class).createSerializer();

			serializer.open(writer);
			for(String key: dataset.keySet()) {
				Method method=dataset.get(key);
				serializer.write(method);
			}
			serializer.close(true);
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		*/
    }


	private static void getCodeMetrics(HashMap<String, Method> dataset) {
		final String[] sourcePathDirs = {};
		final String[] libs = getLibraries(pathRepositoryFile);
		final String[] sources = getSources(pathRepositoryFile);
		//for(int i=0;i<sources.length;i++)
		//System.out.println(sources[i]);

		ArrayList<Method> methods;

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
		methods=requestor.methods;


		for(String id: requestor.methodsCalled) {
			for(Method test: methods) {
				if(test.id.equals(id)) {
					test.fanIN++;
				}
			}
		}
		for(Method method: methods) {
			dataset.put(method.path, method);
		}
	}

	private static void getProcessMetrics(HashMap<String, Method> dataset) {
		//String strHistory=readAll(pathHistoriesAllfile);
		String strRelease2Date=readAll(pathRelease2Date);
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Integer> release2Date=null;
		try {
			//historiesAllfile = mapper.readValue(strHistory, new TypeReference<HashMap<String, History>>() {});
			release2Date=mapper.readValue(strRelease2Date, new TypeReference<HashMap<String, Integer>>() {});
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		int count=0;
		int allFiles=dataset.size();
		int dateFrom=release2Date.get(Integer.toString(release-1));
		int dateUntil=release2Date.get(Integer.toString(release));


		History tmp=null;
		List<Commit> commits=null;
		for(String path: dataset.keySet()) {
			Method method = dataset.get(path);
			History history=historiesAllfile.get(path);
			if(history==null)continue;


			count++;
			System.out.println(count+ "/" + dataset.keySet().size());
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
			//コミットパスを考慮
			/*
			for(Commit commit: nodes) {
				if(history.pathCommit.contains(commit.id)
						& dateFrom<commit.date
						& commit.date<dateUntil
						) {
					commits.add(commit);
				}
			}
			*/

			int periodFrom = Integer.MAX_VALUE;
			for(Commit commit: nodes) {
				if(commit.date<periodFrom) {
					periodFrom=commit.date;
				}
			}
			int periodTo = dateUntil;
			method.period = (periodTo - periodFrom)/(60*60*24);

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

			getA(method, commits);
		}
	}


	private static void getA(Method method, List<Commit> commits){
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
					if(commit2IsFix.get(commits.get(i).id)==1)fixChgNum++;
					if(commitIntro2Path.get(commits.get(i).id)==null) {
					}else if(2<=commitIntro2Path.get(commits.get(i).id).length) {
						bugIntroNum++;
					}else if(1==commitIntro2Path.get(commits.get(i).id).length && commitIntro2Path.get(commits.get(i).id)[0].equals(method.path)) {
						bugIntroNum++;
					}
					if(commit2PathHasBeenBuggy.get(commits.get(i).id)==null) {
					}else if(2<=commit2PathHasBeenBuggy.get(commits.get(i).id).length) {
	    		        logCoupNum++;
					}else if(1==commit2PathHasBeenBuggy.get(commits.get(i).id).length && commit2PathHasBeenBuggy.get(commits.get(i).id)[0].equals(method.path)) {
	    				logCoupNum++;
					}

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
			method.methodHistories=methodHistories;
			method.devTotal=authorsSet.size();
			method.stmtAdded=stmtAddeds.stream().mapToInt(e->e).sum();
			method.maxStmtAdded=stmtAddeds.stream().mapToInt(e->e).max().getAsInt();
			method.avgStmtAdded=method.stmtAdded/(float)methodHistories;
			method.stmtDeleted=stmtDeleteds.stream().mapToInt(e->e).sum();
			method.maxStmtDeleted=stmtDeleteds.stream().mapToInt(e->e).max().getAsInt();
			method.avgStmtDeleted=method.stmtDeleted/(float)methodHistories;
			method.churn=churns.stream().mapToInt(e->e).sum();
			method.maxChurn=churns.stream().mapToInt(e->e).max().getAsInt();
			method.avgChurn=method.churn/(float)methodHistories;
			method.decl=decls.stream().mapToInt(e->e).sum();
			method.cond=conds.stream().mapToInt(e->e).sum();
			method.elseAdded=elseAddeds.stream().mapToInt(e->e).sum();
			method.elseDeleted=elseDeleteds.stream().mapToInt(e->e).sum();
			method.pastBugNum=pastBugNum;
			method.fixChgNum=fixChgNum;
			method.bugIntroNum=bugIntroNum;
			method.logCoupNum=logCoupNum;
			method.avgInterval = method.period / (float)method.methodHistories;
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
    			method.minInterval=minInterval/(60*60*24);
			    method.maxInterval=maxInterval/(60*60*24);
			}
			method.devTotal=authorsSet.size();
			for(Iterator<String> iterator = authorsSet.iterator(); iterator.hasNext(); ) {
				String author=iterator.next();
				float count=0;
				for(int i=0;i<authors.size();i++) {
					if(author.equals(authors.get(i))) {
						count++;
					}
				}
				float ratio=count/authors.size();
				if(method.ownership<ratio) {
					method.ownership=ratio;
				}
				if(0.20<ratio) {
					method.devMajor++;
				}else {
					method.devMinor++;
				}
			}
			method.isBuggy = 0;//commits.get(commits.size()-1).isBuggy? 1:0;
			for(Commit commit: commits) {
				/*
				if(commit.bugFix!=null) {
					method.isBuggy = 1;
				}
				*/

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
					int allFiles=dataset.size();
					int dateRelease=release2Date.get(Integer.toString(release));


					for(String id: commit.bugIntro) {
					    if(commit.date <release2Date.get(Integer.toString(release))
							    & release2Date.get(Integer.toString(release))< revision2Date.get(id)) {
				            method.isBuggy = 1;
					    }
					}
				}


			}
		    //System.out.println("tset");
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
	}


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


	private static String[] getSources(String pathFile) {
		Path rootDir = Paths.get(pathFile);
		String[] sources = null;
		try {
			List<String> test =Files.walk(rootDir)
					.map(Path::toString)
					.filter(p -> p.endsWith(".java"))
					.filter(p -> !p.contains("test"))
					.collect(Collectors.toList());
			sources=test.toArray(new String[test.size()]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sources;
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