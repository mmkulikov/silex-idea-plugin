package sk.sorien.silexplugin.pimple;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import org.jetbrains.annotations.Nullable;
import sk.sorien.silexplugin.SilexProjectComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Stanislav Turza
 */
public class PimplePhpTypeProvider implements PhpTypeProvider2 {

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

    private Signature getChildElementSignature(PsiElement element) {

        element = PsiTreeUtil.getChildOfAnyType(element, Variable.class, FieldReference.class, ArrayAccessExpression.class);
        if (element == null) {
            return null;
        }

        Signature signature = new Signature();

        if (element instanceof PhpReference) {
            signature.set(((PhpReference) element).getSignature());
        }

        if (element instanceof ArrayAccessExpression) {
            signature.set(getTypeForArrayAccess(element));
        }

        if (signature.getClassSignature().isEmpty() || signature.getClassSignature().equals(Utils.ARRAY_SIGNATURE)) {
            return null;
        }

        return signature;
    }

    private String getStringOrSignature(PsiElement element) {

        if (element instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) element).getContents();
        }
        else if (element instanceof PhpReference) {
            return ((PhpReference) element).getSignature();
        }

        return null;
    }

    private String getTypeForArrayAccess(PsiElement e) {

        ArrayAccessExpression arrayAccessExpression;
        Boolean internalResolve = false;

        if (e instanceof ArrayAccessExpression) {
            arrayAccessExpression = (ArrayAccessExpression)e;
        }
        else if (e instanceof NewExpression)
        {
            ClassReference[] classReferences = PsiTreeUtil.getChildrenOfType(e, ClassReference.class);
            if (classReferences == null || classReferences.length != 1) {
                return null;
            }

            ArrayAccessExpression[] arrayAccessExpressions = PsiTreeUtil.getChildrenOfType(classReferences[0], ArrayAccessExpression.class);
            if (arrayAccessExpressions == null || arrayAccessExpressions.length != 1) {
                return null;
            }

            arrayAccessExpression = arrayAccessExpressions[0];
            internalResolve = true;
        }
        else return null;

        Signature signature = getChildElementSignature(arrayAccessExpression);
        if (signature == null) {
            return null;
        }

        ArrayIndex arrayIndex = arrayAccessExpression.getIndex();
        if (arrayIndex == null) {
            return null;
        }

        String serviceName = getStringOrSignature(arrayIndex.getValue());
        if (serviceName == null) {
            return null;
        }

        return signature.toString() + '[' + (internalResolve ? "@" : "") + serviceName + ']';
    }

    private String getTypeForParameterOfAnonymousFunction(PsiElement e) {

        if (!(e instanceof com.jetbrains.php.lang.psi.elements.Parameter)) {
            return null;
        }

        PsiElement element = e.getParent();
        if (!(element instanceof ParameterList)) {
            return  null;
        }

        PsiElement[] anonymousFunctionParams = ((ParameterList) element).getParameters();

        if (anonymousFunctionParams.length == 0) {
            return null;
        }

        element = element.getParent();
        if (!(element instanceof Function)) {
            return null;
        }

        PsiElement closure = element.getParent();
        if (!(closure instanceof PhpExpression)) {
            return null;
        }

        String serviceName = null;
        Signature signature = new Signature();

        element = closure.getParent();

        if (element instanceof ParameterList) {

            PsiElement[] methodParams = ((ParameterList) element).getParameters();
            if (methodParams.length == 0) {
                return null;
            }

            element = element.getParent();
            if (!(element instanceof MethodReference)) {
                return null;
            }

            String methodName = ((MethodReference) element).getName();
            if (methodName == null) {
                return null;
            }

            if ((methodName.equals("factory") || methodName.equals("share")) && methodParams.length == 1 &&
                    Utils.isParameter(closure, methodParams, 0) && Utils.isParameter(e, anonymousFunctionParams, 0)) {
                serviceName = null;

            } else if (methodName.equals("extend") && Utils.isParameter(closure, methodParams, 1)) {

                if (Utils.isParameter(e, anonymousFunctionParams, 0)) {

                    serviceName = getStringOrSignature(methodParams[0]);
                    if (serviceName == null) {
                        return null;
                    }
                }

            } else return null;

            signature = getChildElementSignature(element);
            if (signature == null) {
                return null;
            }

        } else if (element instanceof AssignmentExpression) {

            // $app[''] = function ($c)

            element = PsiTreeUtil.getChildOfType(element, ArrayAccessExpression.class);
            if (element == null) {
                return null;
            }

            element = PsiTreeUtil.getChildOfType(element, Variable.class);
            if (element == null) {
                return null;
            }

            signature.set(((Variable)element).getSignature());

        } else return null;

        return signature.toString() + ( serviceName == null ? "" : '[' + serviceName + ']');
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Signature signature = new Signature(expression);

        // try to resolve service type
        if(SilexProjectComponent.isEnabled(project)) {

            if (signature.hasParameters() && Utils.isPimpleContainerClass(phpIndex, signature.getClassSignature())) {
                return phpIndex.getClassesByFQN(getClassNameFromParameters(phpIndex, project, signature.getParameters()));
            }
        }

        // if try to find internal type from array
        Collection<? extends PhpNamedElement> collection = phpIndex.getBySignature(signature.getClassSignature(), null, 0);
        if (collection.size() == 0) {
            return Collections.emptySet();
        }

        // internal type seems to be array som find proper value type
        if (signature.hasParameters()) {
            PhpNamedElement element = collection.iterator().next();

            for (String type : element.getType().getTypes()) {
                if (type.endsWith("[]")) {
                    Collection<? extends PhpNamedElement> result = phpIndex.getClassesByFQN(type.substring(0, type.length() - 2));
                    if (result.size() != 0) {
                        return result;
                    }
                }
            }
        }

        return collection;
    }

    private String getClassNameFromParameters(PhpIndex phpIndex, Project project, List<String> parameters) {

        Container container = ContainerResolver.get(project);

        for (int i = 0; i < parameters.size() - 1; i++) {
            container = container.getContainers().get(Utils.resolveParameter(phpIndex, parameters.get(i)));
            if (container == null)
                return null;
        }

        String parameter = Utils.resolveParameter(phpIndex, parameters.get(parameters.size() - 1));

        if (parameter.startsWith("@")) {

            Parameter param = container.getParameters().get(parameter.substring(1));
            return param != null ? param.getValue() : null;
        }

        Service service = container.getServices().get(parameter);
        if (service != null) {
            return service.getClassName();
        }

        if (container.getContainers().containsKey(parameter)) {
            return "\\Pimple";
        }

        return null;
    }
}

