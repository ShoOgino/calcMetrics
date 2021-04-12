package data;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestModule {
    private static String   pathProject   = "C:/Users/ShoOgino/data/1_task/20200421_094917/projects/MLTool/datasets/egit";
    private static String[] commitEdgesMethod = {"2c1b0f4ad24fb082e5eb355e912519c21a5e3f41", "1241472396d11fe0e7b31c6faf82d04d39f965a6"};
    private static String[] commitEdgesFile =  {"dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b","0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7"};

    private static String pathRepositoryMethod = pathProject+"/repositoryMethod";
    private static String pathRepositoryFile = pathProject+"/repositoryFile";
    private static String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+".csv";
    private static String pathModules = pathProject+"/modules.json";
    private static String pathCommits = pathProject+"/commits";
    private static String pathBugs = pathProject+"/bugs.json";

    private static Commits commitsAll = new Commits();
    private static ModulesAll modulesAll = new ModulesAll();
    private static Bugs bugsAll = new Bugs();

    @BeforeAll
    static public void setUp() {
        modulesAll.loadModules(pathModules);
        commitsAll.loadCommits(pathCommits);
    }

    @Test
    public void testCalcFanOut1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/command/FetchConfiguredRemoteCommand#execute(ExecutionEvent).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(10, module.fanOut);
    }

    @Test
    public void testCalcFanOut2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitMessageComponentStateManager#persistState(Repository,CommitMessageComponentState).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(12, module.fanOut);
    }

    @Test
    public void testCalcFanOut3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/SynchronizeWithActionHandler#execute(ExecutionEvent).mjava");
        module.calcFanOut(pathRepositoryMethod);
        assertEquals(15, module.fanOut);
    }

    @Test
    public void testCalcLocalVar1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/push/SimpleConfigurePushDialog#createDialogArea(Composite).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(35, module.localVar);
    }
    @Test
    public void testCalcLocalVar2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(12, module.localVar);
    }
    @Test
    public void testCalcLocalVar3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/GitHistoryPage#buildFilterPaths(IResource[],File[],Repository).mjava");
        module.calcLocalVar(pathRepositoryMethod);
        assertEquals(10, module.localVar);
    }
    @Test
    public void testCalcCommentRatio1(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/ContainerTreeIterator#ContainerTreeIterator(Repository,IWorkspaceRoot).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f", 0.684210538864135), String.format("%.5f", module.commentRatio));
    }
    @Test
    public void testCalcCommentRatio2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/decorators/DecoratableResourceHelper#createThreeWayTreeWalk(RepositoryMapping,ArrayList[String]).mjava");
        module.calcCommentRatio(pathRepositoryMethod);
        assertEquals(String.format("%.5f",0.161290317773818), String.format("%.5f",module.commentRatio));
    }

    @Test
    public void testCalcCountPath1(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/ContainerTreeIterator#isEntryIgnoredByTeamProvider(IResource).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(4, module.countPath);
    }

    @Test
    public void testCalcCountPath2(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/synchronize/GitSyncInfo#calculateKindImpl(Repository,TreeWalk,int,int).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(16, module.countPath);
    }

    @Test
    public void testCalcCountPath3(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/synchronize/dto/GitSynchronizeData#updateRevs().mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(8, module.countPath);
    }
    @Test
    public void testCalcCountPath4(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcCountPath(pathRepositoryMethod);
        assertEquals(109, module.countPath);
    }

    @Test
    public void testCalcComplexity1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#getFileStatus(String,IndexDiff).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(14, module.complexity);
    }
    @Test
    public void testCalcComplexity2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/history/FileDiff#compute(TreeWalk,RevCommit).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(13, module.complexity);
    }
    @Test
    public void testCalcComplexity3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/tree/command/RepositoriesViewCommandHandler#enableWorkingDirCommand(Object).mjava");
        module.calcComplexity(pathRepositoryMethod);
        assertEquals(13, module.complexity);
    }

    @Test
    public void testCalcExecStmt1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/commit/CommitUI#getSelectedFiles().mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(10, module.execStmt);
    }
    @Test
    public void testCalcExecStmt2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/CompareUtils#getAdapter(Object,Class,boolean).mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(11, module.execStmt);
    }
    @Test
    public void testCalcExecStmt3(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/CompareWithIndexActionHandler#execute(ExecutionEvent).mjava");
        module.calcExecStmt(pathRepositoryMethod);
        assertEquals(13, module.execStmt);
    }

    @Test
    public void testCalcMaxNesting1(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/repository/RepositoriesViewLabelProvider#decorateImage(Image,Object).mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(8, module.maxNesting);
    }
    @Test
    public void testCalcMaxNesting2(){
        Module module = new Module("org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/clone/GitSelectWizardPage#createControl(Composite).mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(7, module.maxNesting);
    }
    @Test
    public void testCalcMaxNesting3(){
        Module module = new Module("org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava");
        module.calcMaxNesting(pathRepositoryMethod);
        assertEquals(1, module.maxNesting);
    }

    @Test
    public void testCalcModuleHistories1(){
        String pathModule="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#okPressed().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(10, module.moduleHistories);
    }
    @Test
    public void testCalcModuleHistories2(){
        String pathModule ="org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(3, module.moduleHistories);
    }
    @Test
    public void testCalcModuleHistories3(){
        String pathModule = "org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/MergeActionHandler#execute(ExecutionEvent).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcModuleHistories(commitsAll, commitEdgesMethod);
        assertEquals(8, module.moduleHistories);
    }

    @Test
    public void testCalcAuthors1(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/dialogs/CommitDialog#okPressed().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcDevTotal(commitsAll, commitEdgesMethod);
        assertEquals(6, module.authors);
    }
    @Test
    public void testCalcAuthors2(){
        String pathModule ="org.eclipse.egit.core/src/org/eclipse/egit/core/project/RepositoryMapping#getGitDirAbsolutePath().mjava";
        Module module = modulesAll.get(pathModule);
        module.calcDevTotal(commitsAll, commitEdgesMethod);
        assertEquals(3, module.authors);
    }
    @Test
    public void testCalcAuthors3(){
        String pathModule ="org.eclipse.egit.ui/src/org/eclipse/egit/ui/internal/actions/MergeActionHandler#execute(ExecutionEvent).mjava";
        Module module = modulesAll.get(pathModule);
        module.calcDevTotal(commitsAll, commitEdgesMethod);
        assertEquals(4, module.authors);
    }
}