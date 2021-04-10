package data;

import me.tongfei.progressbar.ProgressBar;
import misc.DoubleConverter;
import net.sf.jsefa.Serializer;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;
import org.eclipse.jgit.api.errors.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import static util.FileUtil.findFiles;
import static util.RepositoryUtil.checkoutRepository;

public class ModulesTarget {
    HashMap<String, Module> modules = new HashMap<String, Module>();

    public void identifyTargetModules(String pathRepository, String[] commitEdges) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
        checkoutRepository(pathRepository, commitEdges[1]);
        List<String> pathSources = findFiles(pathRepository, ".mjava", "test");
        for(String pathSource: ProgressBar.wrap(pathSources, "setEmptyRecords")) {
            pathSource=pathSource.replace("\\", "/");
            String prefix = pathRepository+"/";
            int index = pathSource.indexOf(prefix);
            String pathModule = pathSource.substring(index+prefix.length());
            Module module = new Module();
            module.path=pathModule;
            modules.put(pathModule, module);
        }
    }
    //対象モジュール全部について、コードメトリクスを算出する。
    public void calcCodeMetrics(ModulesAll modulesAll, String pathRepository, String[] commitEdges) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, IOException, GitAPIException {
        checkoutRepository(pathRepository, commitEdges[1]);
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcCodeMetrics")){
            Module module = modulesAll.get(pathModule);
            module.calcFanOut(pathRepository);
            module.calcParameters(pathRepository);
            module.calcLocalVar(pathRepository);
            module.calcCommentRatio(pathRepository);
            module.calcCountPath(pathRepository);
            module.calcComplexity(pathRepository);
            module.calcExecStmt(pathRepository);
            module.calcMaxNesting(pathRepository);
        }
    }

    //対象モジュール全部について、プロセスメトリクスを算出する。
    public void calcProcessMetrics(ModulesAll modulesAll, Commits commitsAll, Bugs bugsAll, String[] commitEdges) {
        for(String pathModule: ProgressBar.wrap(modules.keySet(), "calcProcessMetrics")){
            Module module = modulesAll.get(pathModule);
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
            File csv = new File(pathDataset);
            Serializer serializer = CsvIOFactory.createFactory(config, Module.class).createSerializer();

            serializer.open(writer);
            for(String key: modules.keySet()) {
                Module module=modules.get(key);
                serializer.write(module);
            }
            serializer.close(true);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
