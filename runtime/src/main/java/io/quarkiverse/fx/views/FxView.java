package io.quarkiverse.fx.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE })
public @interface FxView {

    /**
     * Defines the base name for .fxml and .properties resources.
     * If omitted, name will use convention (controller's name without the "Controller" suffix)
     */
    String value() default "";
}
