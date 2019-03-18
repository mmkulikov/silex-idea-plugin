package sk.sorien.pimpleplugin.pimple;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import org.jetbrains.annotations.Nullable;
import sk.sorien.pimpleplugin.ProjectComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Stanislav Turza
 */
public class PimplePhpTypeProvider2 extends AbstractPimplePhpTypeProvider implements PhpTypeProvider2 {
    @Override
    public char getKey() {
        return 'Š';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        String signature = getTypeForArrayAccess(e);
        if (signature != null) {
            return signature;
        }

        signature = getTypeForParameterOfAnonymousFunction(e);
        if (signature != null) {
            return signature;
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Signature signature = new Signature(expression);

        // try to resolve service type
        if(ProjectComponent.isEnabled(project) && signature.hasParameter()) {
            ArrayList<String> parameters = new ArrayList<String>();
            if (Utils.findPimpleContainer(phpIndex, expression, parameters)) {
                return phpIndex.getClassesByFQN(getClassNameFromParameters(phpIndex, project, parameters));
            }
        }

        // if it's not a service try to get original type
        Collection<? extends PhpNamedElement> collection = phpIndex.getBySignature(signature.base, null, 0);
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

