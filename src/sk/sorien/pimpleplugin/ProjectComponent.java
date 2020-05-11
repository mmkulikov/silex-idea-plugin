package sk.sorien.pimpleplugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.sorien.pimpleplugin.pimple.*;
import sk.sorien.pimpleplugin.ui.ContainerStatusBarWidget;

/**
 * @author Stanislav Turza
 */
public class ProjectComponent implements com.intellij.openapi.components.ProjectComponent {
    private final Project project;
    private StatusBar statusBar;
    private ContainerStatusBarWidget containerStatusBarWidget;

    public ProjectComponent(Project project) {
        this.project = project;
    }

    public void projectOpened() {
        statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            containerStatusBarWidget = new ContainerStatusBarWidget(project);
            statusBar.addWidget(containerStatusBarWidget);

            setStatusBarText("Loading...");
            //containerStatusBarWidget.setText("Loading...");
        }

        Configuration conf = Configuration.getInstance(project);
        Container jsonContainer = new JsonFileContainer(project, conf.containerDefinitionFileName);
        ContainerResolver.put(project, jsonContainer);
    }

    public void projectClosed() {
        if (statusBar != null && containerStatusBarWidget != null) {
            statusBar.removeWidget(ContainerStatusBarWidget.WIDGET_ID);
        }

        ContainerResolver.remove(project);
    }

    public void setStatusBarText(String text) {
        if (containerStatusBarWidget != null) {
            containerStatusBarWidget.setText(text);
        }
    }

    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Configuration.getInstance(project).pluginEnabled;
    }

    public static void error(String text, Project project) {
        Notifications.Bus.notify(new Notification("Silex Plugin", "Silex Plugin", text, NotificationType.ERROR), project);
    }

    public static void warning(String text, Project project) {
        Notifications.Bus.notify(new Notification("Silex Plugin", "Silex Plugin", text, NotificationType.WARNING), project);
    }

    public static void configChanged(Project project) {
        ContainerResolver.remove(project);
        ContainerResolver.put(project, new JsonFileContainer(project, Configuration.getInstance(project).containerDefinitionFileName));
    }

//    public void initComponent() {
//
//    }
//
//    public void disposeComponent() {
//
//    }

    @NotNull
    public String getComponentName() {
        return "silexPlugin";
    }
}
