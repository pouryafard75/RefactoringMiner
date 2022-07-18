

package jdt;

import gen.SyntaxException;
import gen.TreeGenerator;
import tree.TreeContext;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public abstract class AbstractJdtTreeGenerator extends TreeGenerator {
    private static char[] readerToCharArray(Reader r) throws IOException {
        StringBuilder fileData = new StringBuilder();
        try (BufferedReader br = new BufferedReader(r)) {
            char[] buf = new char[10];
            int numRead = 0;
            while ((numRead = br.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        }
        return  fileData.toString().toCharArray();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TreeContext generate(Reader r) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map pOptions = JavaCore.getOptions();
        pOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_16);
        pOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_16);
        pOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_16);
        pOptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setCompilerOptions(pOptions);
        char[] source = readerToCharArray(r);
        parser.setSource(source);
        IScanner scanner = ToolFactory.createScanner(false, false, false, false);
        scanner.setSource(source);
        AbstractJdtVisitor v = createVisitor(scanner);
        ASTNode node = parser.createAST(null);
        if ((node.getFlags() & ASTNode.MALFORMED) != 0) // bitwise flag to check if the node has a syntax error
            throw new SyntaxException(this, r, null);
        node.accept(v);
        return v.getTreeContext();
    }

    public TreeContext generate(Reader r, ASTNode node) throws IOException {
        char[] source = readerToCharArray(r);
        IScanner scanner = ToolFactory.createScanner(false, false, false, false);
        scanner.setSource(source);
        AbstractJdtVisitor v = createVisitor(scanner);
        if ((node.getFlags() & ASTNode.MALFORMED) != 0) // bitwise flag to check if the node has a syntax error
            throw new SyntaxException(this, r, null);
        node.accept(v);
        return v.getTreeContext();
    }


    protected abstract AbstractJdtVisitor createVisitor(IScanner scanner);
}
