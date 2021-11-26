package net.corda.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.utils.Constants;

@State(name = "CordaSettings", storages = @Storage("Corda.xml"))
public class CordaSettings implements PersistentStateComponent<CordaSettings.State> {

    @NotNull
    public static CordaSettings getInstance(@NotNull Project project) {
        return project.getService(CordaSettings.class);
    }

    private State myState = new State();

    public State getState() {
        return myState;
    }

    public void loadState(@NotNull State state) {
        myState = state;
    }

    public void setCordaProject(String b) {
        myState.setCordaProject(b);
    }

    public boolean firstOpen() {
        return myState.getCordaProject() == null;
    }

    public boolean isCordaProject() {
        return Constants.NAME.equals(myState.getCordaProject());
    }

    @Data
    public static class State {
        private String cordaProject;
    }
}
