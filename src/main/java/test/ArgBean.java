package test;
import org.kohsuke.args4j.Option;

public class ArgBean {
    @Option(name = "-f")
    public Boolean flag;
    @Option(name = "--release", metaVar = "release", required = true, usage = "release")
    public int release;
    @Option(name = "--pathProject", metaVar = "pathProject", required = true, usage = "pathProject")
    public String pathProject;
    @Option(name = "--pathPlugins", metaVar = "pathPlugin", required = true, usage = "pathPlugin")
    public String pathPlugins;
}
