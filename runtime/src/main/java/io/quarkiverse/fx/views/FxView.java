package io.quarkiverse.fx.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE })
public @interface FxView {

    /**
     * Defines the base name for .fxml, .css and .properties resources.
     * If omitted, name will use convention (controller's name without the "Controller" suffix)
     */
    String value() default "";

    /**
     * Defines location for fx resource (.fxml)
     * If omitted, "${quarkus.fx.fxml-root}/${baseName}.fxml" will be used.
     */
    String fxmlLocation() default "";

    /**
     * Root location for style resources (.css)
     * If omitted, "${quarkus.fx.style-root}/${baseName}.css" will be used.
     */
    String styleLocation() default "";

    /**
     * Root location for bundle resources (.properties)
     * If omitted, "${quarkus.fx.bundle-root}/${baseName}.properties" will be used.
     */
    String bundleLocation() default "";
}
