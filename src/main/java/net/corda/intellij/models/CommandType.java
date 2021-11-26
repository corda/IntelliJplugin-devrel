package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.corda.intellij.utils.MessageBundle;

@AllArgsConstructor @Getter
public enum CommandType {
    CLEAN("clean", MessageBundle.message("command.clean")),
    BUILD("build", MessageBundle.message("command.build")),
    DEPLOY("deploy", MessageBundle.message("command.deploy")),
    RUN("run", MessageBundle.message("command.run")),
    SHOW("show", MessageBundle.message("command.show")),
    STOP("stop", MessageBundle.message("command.stop")),
    TEST("test", MessageBundle.message("command.test")),
    INSPECT("inspect", MessageBundle.message("command.inspect")),
    ;
    private final String code;
    private final String label;
}
