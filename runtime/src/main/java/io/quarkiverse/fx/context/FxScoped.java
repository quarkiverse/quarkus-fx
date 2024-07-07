package io.quarkiverse.fx.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Scope;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Scope
//@NormalScope
public @interface FxScoped {
}
