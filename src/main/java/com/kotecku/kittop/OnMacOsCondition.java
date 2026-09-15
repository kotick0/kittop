package com.kotecku.kittop;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnMacOsCondition implements Condition {

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        return System.getProperty("os.arch").contains("aarch64") && System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
