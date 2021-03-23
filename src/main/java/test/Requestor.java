package test;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class Requestor extends FileASTRequestor  {
	HashMap<String, Record> records = new HashMap<String, Record>();
	ArrayList<String> methodsCalled = new ArrayList<String>();
	public void acceptAST(String pathFile, CompilationUnit ast) {
		System.out.println("---------------"+pathFile+"-----------------------------------------");
		Visitor visitor = new Visitor(ast, pathFile);
		ast.accept(visitor);
		for(Record record:visitor.records) {
			records.put(record.path, record);
		}
		methodsCalled.addAll(visitor.methodsCalled);
	}
}