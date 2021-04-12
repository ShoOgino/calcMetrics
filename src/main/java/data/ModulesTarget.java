package data;

import ast.RequestorFanIn;
import me.tongfei.progressbar.ProgressBar;
import misc.DoubleConverter;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jgit.api.errors.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.FileUtil.findFiles;
import static util.RepositoryUtil.checkoutRepository;

public class ModulesTarget {
    HashMap<String, Module> modules = new HashMap<>();

    public void identifyTargetModules(ModulesAll modulesAll, String pathRepository, String[] commitEdges) throws IOException, GitAPIException {
        checkoutRepository(pathRepository, commitEdges[1]);
        List<String> pathSources = findFiles(pathRepository, ".mjava", "test");
        for(String pathSource: ProgressBar.wrap(pathSources, "identifyTargetModules")) {
            pathSource=pathSource.replace("\\", "/");
            String prefix = pathRepository+"/";
            int index = pathSource.indexOf(prefix);
            String pathModule = pathSource.substring(index+prefix.length());
            Module module = new Module();
            module.path=pathModule;
            modules.put(pathModule, modulesAll.get(pathModule));
        }
    }

    //対象モジュール全部について、コードメトリクスを算出する。
    public void calcCodeMetrics(String pathRepositoryFile, String[] commitEdgesFile, String pathRepositoryMethod, String[] commitEdgesMethod) throws IOException, GitAPIException {
        checkoutRepository(pathRepositoryMethod, commitEdgesMethod[1]);
        System.out.println("calculating FanIn...");
        calcFanIn(pathRepositoryFile, commitEdgesFile);
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcCodeMetrics")){
            Module module = modules.get(pathModule);
            module.calcFanOut(pathRepositoryMethod);
            module.calcParameters(pathRepositoryMethod);
            module.calcLocalVar(pathRepositoryMethod);
            module.calcCommentRatio(pathRepositoryMethod);
            module.calcCountPath(pathRepositoryMethod);
            module.calcComplexity(pathRepositoryMethod);
            module.calcExecStmt(pathRepositoryMethod);
            module.calcMaxNesting(pathRepositoryMethod);
        }
    }

    //FanInは個々のモジュールで独立に計算できない。仕方なく別口で計算する。
    private void calcFanIn(String pathRepositoryFile, String[] commitEdgesFile) throws GitAPIException, IOException {
        checkoutRepository(pathRepositoryFile, commitEdgesFile[1]);
        final String[] sourcePathDirs = {};
        final String[] libraries      = findFiles(pathRepositoryFile, ".jar", "test").toArray(new String[0]);
        final String[] sources        = findFiles(pathRepositoryFile, ".java", "test").toArray(new String[0]);

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
        RequestorFanIn requestorFanIn = new RequestorFanIn(modules);
        parser.createASTs(sources, null, keys, requestorFanIn, new NullProgressMonitor());
        for(String idMethodCalled: requestorFanIn.methodsCalled) {
            for(String pathMethod: modules.keySet()) {
                String idMethod = modules.get(pathMethod).id;
                if(idMethod.equals(idMethodCalled)) {
                    modules.get(pathMethod).fanIn++;
                }
            }
        }
    }

    //対象モジュール全部について、プロセスメトリクスを算出する。
    public void calcProcessMetrics(Commits commitsAll, Bugs bugsAll, String[] commitEdges) {
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcProcessMetrics")){
            Module module = modules.get(pathModule);
            module.calcModuleHistories(commitsAll, commitEdges);
            module.calcDevTotal(commitsAll, commitEdges);
            module.calcStmtAdded(commitsAll, commitEdges);
            module.calcMaxStmtAdded(commitsAll, commitEdges);
            module.calcAvgStmtAdded(commitsAll, commitEdges);
            module.calcStmtDeleted(commitsAll, commitEdges);
            module.calcMaxStmtDeleted(commitsAll, commitEdges);
            module.calcAvgStmtDeleted(commitsAll, commitEdges);
            module.calcChurn(commitsAll, commitEdges);
            module.calcMaxChurn(commitsAll, commitEdges);
            module.calcAvgChurn(commitsAll, commitEdges);
            module.calcDecl(commitsAll, commitEdges);
            module.calcCond(commitsAll, commitEdges);
            module.calcElseAdded(commitsAll, commitEdges);
            module.calcElseDeleted(commitsAll, commitEdges);
            module.calcIsBuggy(commitsAll, bugsAll, commitEdges);
        }
    }
    //算出されたレコードをファイルに保存する。
    public void saveMetricsAsRecords(String pathDataset) {
        try {
            FileOutputStream fos= new FileOutputStream(pathDataset);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            BufferedWriter writer = new BufferedWriter(osw);
            CsvConfiguration config = new CsvConfiguration();
            config.setFieldDelimiter(',');
            config.getSimpleTypeConverterProvider().registerConverterType(double.class, DoubleConverter.class);
            Serializer serializer = CsvIOFactory.createFactory(config, Module.class).createSerializer();

            serializer.open(writer);
            for(String key: modules.keySet()) {
                Module module=modules.get(key);
                serializer.write(module);
            }
            serializer.close(true);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
