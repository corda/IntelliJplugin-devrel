package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.corda.intellij.ui.listeners.BaseHandler;

@AllArgsConstructor
@Getter
public class Command {
    CommandType type;
    BaseHandler handler;
}
