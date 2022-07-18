

package jdt;

import org.eclipse.jdt.core.compiler.IScanner;


public class JdtTreeGenerator extends AbstractJdtTreeGenerator {
    @Override
    protected AbstractJdtVisitor createVisitor(IScanner scanner) {
        return new JdtVisitor(scanner);
    }
}
