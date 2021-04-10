package test;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class requestorTest extends FileASTRequestor {
	public void acceptAST(String pathFile, CompilationUnit ast) {
		System.out.println("---------------"+pathFile+"-----------------------------------------");
		VisitorFanout visitor = new VisitorFanout();
		ast.accept(visitor);
	}
}
