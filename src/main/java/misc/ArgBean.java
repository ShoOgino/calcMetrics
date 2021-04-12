package main.java.misc;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

public class ArgBean {
    @Option(name = "--pathProject", metaVar = "pathProject", required = true)
    public  String pathProject = new String() ;
    @Option(name = "--commitEdgesMethod", handler = StringArrayOptionHandler.class, metaVar = "commitEdgesMethod", required = true)
    public  String[] commitEdgesMethod = new String[2];
    @Option(name = "--commitEdgesFile", handler = StringArrayOptionHandler.class, metaVar = "commitEdgesFile", required = true)
    public  String[] commitEdgesFile = new String[2] ;
}
