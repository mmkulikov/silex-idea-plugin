package sk.sorien.pimpleplugin.pimple;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Konstantinos Christofilos <kostasxx@gmail.com>
 */
public class PimplePhpTypeProvider3 extends AbstractPimplePhpTypeProvider implements PhpTypeProvider3 {
    static PhpTypeProvider2 provider2 = new PimplePhpTypeProvider2();

    @Override
    public char getKey() {
        return provider2.getKey();
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement psiElement) {

        String elemType = provider2.getType(psiElement);
        if (elemType != null) {
            Project project = psiElement.getProject();
            Signature sig = new Signature();
            sig.set(elemType);
            Collection<? extends PhpNamedElement> col = provider2.getBySignature(elemType, project);

            // Return first element
            for (PhpNamedElement elem: col) {
                return elem.getType();
            }
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project) {

        return provider2.getBySignature(s, project);
    }
}
