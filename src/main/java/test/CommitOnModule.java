package test;

import java.util.HashSet;
import java.util.Set;

public class CommitOnModule {
    public String id;
    public String pathOld;
    public String pathNew;
    public Set<String> parents;
    public Set<String> childs;

	public CommitOnModule() {
		this.id=new String();
		this.pathOld=new String();
		this.pathNew=new String();
		this.parents = new HashSet<String>();
		this.childs  = new HashSet<String>();
	}
}