package sk.sorien.pimpleplugin.pimple;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;

import java.util.List;

/**
 * @author Konstantinos Christofilos <kostasxx@gmail.com>
 */
public class AbstractPimplePhpTypeProvider {

    protected String getStringOrSignature(PsiElement element) {

        if (element instanceof StringLiteralExpression) {
            return Utils.normalizedString((StringLiteralExpression) element);
        }
        else if (element instanceof PhpReference) {
            return ((PhpReference) element).getSignature();
        }

        return null;
    }

    protected String getTypeForParameterOfAnonymousFunction(PsiElement e) {

        if (!(e instanceof com.jetbrains.php.lang.psi.elements.Parameter)) {
            return null;
        }

        if (PsiTreeUtil.getChildOfType(e, com.jetbrains.php.lang.psi.elements.ClassReference.class) != null) {
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

    protected String getClassNameFromParameters(PhpIndex phpIndex, Project project, List<String> parameters) {

        Container container = ContainerResolver.get(project);

        for (int i = 0; i < parameters.size() - 1; i++) {
            container = container.getContainers().get(Utils.getResolvedParameter(phpIndex, parameters.get(i)));
            if (container == null)
                return null;
        }

        String parameter = Utils.getResolvedParameter(phpIndex, parameters.get(parameters.size() - 1));

        if (parameter.startsWith("@")) {

            Parameter param = container.getParameters().get(parameter.substring(1));
            return param != null ? param.getValue() : null;
        }

        Service service = container.getServices().get(parameter);
        if (service != null) {
            return service.getClassName();
        }

        if (container.getContainers().containsKey(parameter)) {
            return "\\Pimple\\Container";
        }

        return null;
    }

    protected String getTypeForArrayAccess(PsiElement e) {

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

    protected Signature getChildElementSignature(PsiElement element) {

        element = PsiTreeUtil.getChildOfAnyType(element, Variable.class, FieldReference.class, ArrayAccessExpression.class);
        if (element == null) {
            return null;
        }

        Signature signature = new Signature();

        if (element instanceof PhpReference) {
            signature.set(((PhpReference) element).getSignature());
        }
        else if (element instanceof ArrayAccessExpression) {
            signature.set(getTypeForArrayAccess(element));
        }

        return signature.hasValidClassSignature() ? signature : null;
    }

}
