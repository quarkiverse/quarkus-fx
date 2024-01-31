package io.quarkiverse.fx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Mark annotated target to be executed on JavaFx UI Thread (JavaFX Application Thread),
 * passing the target as a runnable to {@link javafx.application.Platform#runLater(Runnable)}
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface RunOnFxThread {
}
