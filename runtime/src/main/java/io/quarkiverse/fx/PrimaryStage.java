package io.quarkiverse.fx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import javafx.stage.Stage;

/**
 * This is used as a qualifier as for receiving the notification main <code>start</code> entry point for all
 * JavaFX applications has been called.
 * It is also used to qualify the CountDownLatch type used by the {@linkplain FxApplication} uses
 * as a startup notification barrier.
 *
 * @see javafx.application.Application#start(Stage)
 * @see FxApplication#start(Stage)
 */
@Qualifier
@Target({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PrimaryStage {
}
