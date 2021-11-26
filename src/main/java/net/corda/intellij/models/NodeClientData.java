package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NodeClientData {
    private final String nodeDefaults;
    private final String activeNodeConfigs;
    private final boolean isNodesRunning;
    private final boolean isValid;
}
