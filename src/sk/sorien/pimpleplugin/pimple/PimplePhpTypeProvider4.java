package sk.sorien.pimpleplugin.pimple;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.NoAccessDuringPsiEvents;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import org.jetbrains.annotations.Nullable;
import sk.sorien.pimpleplugin.Configuration;
import sk.sorien.pimpleplugin.ProjectComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Konstantinos Christofilos <kostasxx@gmail.com>
 * @author Mikhail Kulikov <mmkulikov67@gmail.com>
 */
public class PimplePhpTypeProvider4 extends AbstractPimplePhpTypeProvider implements PhpTypeProvider4 {
    private static final char KEY = 'Š';

    @Override
    public char getKey() {
        return KEY;
    }

    @Override
    public @Nullable PhpType getType(PsiElement psiElement) {
        Project project = psiElement.getProject();

        if (!ProjectComponent.isEnabled(project)) {
            return null;
        }

        if (DumbService.getInstance(project).isDumb() || NoAccessDuringPsiEvents.isInsideEventProcessing()) {
            return null;
        }

        String signature = getTypeForArrayAccess(psiElement);
        if (signature == null) {
            signature = getTypeForParameterOfAnonymousFunction(psiElement);
            if (signature == null) {
                return null;
            }
        }

        return new PhpType().add("#" + this.getKey() + signature);
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int depth, Project project) {
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Signature signature = new Signature(s);

        // try to resolve service type
        if (ProjectComponent.isEnabled(project) && signature.hasParameter()) {
            ArrayList<String> parameters = new ArrayList<String>();
            if (Utils.findPimpleContainer(phpIndex, s, parameters)) {
                return phpIndex.getClassesByFQN(getClassNameFromParameters(phpIndex, project, parameters));
            }
        }

        // if it's not a service try to get original type
        Collection<? extends PhpNamedElement> collection = phpIndex.getBySignature(signature.base, set, ++depth);
        if (collection.isEmpty()) {
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