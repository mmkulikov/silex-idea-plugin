package sk.sorien.pimpleplugin.pimple;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import org.jetbrains.annotations.Nullable;
import sk.sorien.pimpleplugin.ProjectComponent;

import java.util.*;

/**
 * @author Konstantinos Christofilos <kostasxx@gmail.com>
 */
public class PimplePhpTypeProvider3 extends AbstractPimplePhpTypeProvider implements PhpTypeProvider3 {

    @Override
    public char getKey() {
        return 'Š';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement psiElement) {

        String signature = getTypeForArrayAccess(psiElement);
        if (signature == null) {
            signature = getTypeForParameterOfAnonymousFunction(psiElement);
        }
        if (signature == null) {
            return null;
        }
        Project project = psiElement.getProject();
        Signature sig = new Signature();
        sig.set(signature);
        Collection<? extends PhpNamedElement> col = getBySignature(signature, null, 0, project);

        // Return first element
        for (PhpNamedElement elem: col) {
            return elem.getType();
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Signature signature = new Signature(s);

        // try to resolve service type
        if(ProjectComponent.isEnabled(project) && signature.hasParameter()) {
            ArrayList<String> parameters = new ArrayList<String>();
            if (Utils.findPimpleContainer(phpIndex, s, parameters)) {
                return phpIndex.getClassesByFQN(getClassNameFromParameters(phpIndex, project, parameters));
            }
        }

        // if it's not a service try to get original type
        Collection<? extends PhpNamedElement> collection = phpIndex.getBySignature(signature.base, set, i);
        if (collection.size() == 0) {
            return Collections.emptySet();
        }

        // original type can be array (#C\ClassType[]) resolve to proper value type
        PhpNamedElement element = collection.iterator().next();

        for (String type : element.getType().getTypes()) {
            if (type.endsWith("[]")) {
                Collection<? extends PhpNamedElement> result = phpIndex.getClassesByFQN(type.substring(0, type.length() - 2));
                if (result.size() != 0) {
                    return result;
                }
            }
        }

        return collection;
    }
}