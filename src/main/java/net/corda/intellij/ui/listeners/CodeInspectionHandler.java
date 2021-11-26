package net.corda.intellij.ui.listeners;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import net.corda.intellij.inspection.CordaCodeInspection;


public class CodeInspectionHandler implements BaseHandler {

    @Override
    public void perform(Project project) {
        final InspectionManagerEx managerEx = (InspectionManagerEx) InspectionManager.getInstance(project);
        final InspectionProfile currentProfile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        final InspectionToolWrapper<?, ?> toolWrapper = currentProfile.getInspectionTool(
            CordaCodeInspection.INSPECTION_SHORT_NAME, project);
        DumbService.getInstance(project).smartInvokeLater(() -> RunInspectionIntention.rerunInspection(toolWrapper, managerEx, new AnalysisScope(project), null));
    }
}
