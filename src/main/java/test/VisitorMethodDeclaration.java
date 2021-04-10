package test;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class VisitorMethodDeclaration extends ASTVisitor{
	int parameters=0;
	public boolean visit(MethodDeclaration node) {
		parameters=node.parameters().size();
		return super.visit(node);
	}

}
