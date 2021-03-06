package sk.sorien.pimpleplugin.pimple;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import sk.sorien.pimpleplugin.ProjectComponent;

/**
 * @author Stanislav Turza
 */
public class PimpleReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
                PlatformPatterns.psiElement(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new ServiceReferenceProvider()
        );
    }

    private static class ServiceReferenceProvider extends PsiReferenceProvider {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

            String serviceName = Utils.normalizedString((StringLiteralExpression) psiElement);

            if(!ProjectComponent.isEnabled(psiElement.getProject())) {
                return new PsiReference[0];
            }

            Container container = Utils.findContainerForPimpleArrayAccessLiteral((StringLiteralExpression) psiElement);
            if (container == null) {

                container = Utils.findContainerForFirstParameterOfPimpleMethod((StringLiteralExpression) psiElement);
                if (container == null) {
                    return new PsiReference[0];
                }

                // we cant detect if we are triggering CTRL+Click from some SubContainer or not so fallback to top most.
                container = ContainerResolver.get(psiElement.getProject());
            }

            Service service = container.getServices().get(serviceName);
            if (service == null) {
                return new PsiReference[0];
            }

            ServiceReference psiReference = new ServiceReference(service.getClassName(), (StringLiteralExpression) psiElement);
            return new PsiReference[]{psiReference};
        }
    }
}
