package test;

import java.util.ArrayList;
import java.util.HashMap;

public class History {
	public String path;
	public ArrayList<Commit> commits;
	public HashMap<String, ArrayList<String>> commit2Parents;
	public HashMap<String, ArrayList<String>> commit2Childs;
	public ArrayList<String> heads;
	public ArrayList<String> roots;
	public ArrayList<String> pathCommit;

	public History() {
		this.path=new String();
		this.commits=new ArrayList<Commit>();
		this.commit2Parents=new HashMap<String, ArrayList<String>>();
		this.commit2Childs=new HashMap<String, ArrayList<String>>();
		this.heads = new ArrayList<String>();
		this.roots = new ArrayList<String>();
	}
}
