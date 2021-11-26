package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class CheckResult {
    boolean javaCheck;
    boolean gitCheck;
    boolean gradleCheck;
    long memory;
}
