package test;

import java.util.ArrayList;
import java.util.HashMap;

public class SourceFile {
	public String path;
	public HashMap<String, CommitOnModule> idCommit2CommitOnModule;
	public ArrayList<String> commitsHead;
	public ArrayList<String> commitsRoot;

	public SourceFile() {
		this.path=new String();
		this.idCommit2CommitOnModule = new HashMap<>();
		this.commitsHead = new ArrayList<String>();
		this.commitsRoot = new ArrayList<String>();
	}
}