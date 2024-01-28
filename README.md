# quarkus-fx
This Quarkus extension allows you to use JavaFX in your Quarkus application. \
It will allow component injection in FX Controllers and will allow you to use CDI events to register on primary stage creation.

Live reload is still problematic and will be studied in the future.

You will be able to register on primary stage creation event via such code example.
```java
import java.io.InputStream;

public class QuarkusFxApp {

  @Inject
  FXMLLoader fxmlLoader;

  public void start(@Observes @PrimaryStage final Stage stage) {
    try {
      URL fxml = this.getClass().getResource("/app.fxml");
      Parent fxmlParent = this.fxmlLoader.load(fxml.openStream());

      Scene scene = new Scene(fxmlParent);
      stage.setScene(scene);
      stage.show();

    } catch (IOException e) {
      // TODO
    }
  }
}
```
To load multiple FXML files, you can use :
```java
@Inject
Instance<FXMLLoader> fxmlLoader;
```

Also, setting the location is required by some use cases (use of relative paths in FXML)
```java
FXMLLoader loader = this.fxmlLoader.get();
// Set location for relative path resolution
loader.setLocation(xxx);
```

By default, the application is automatically launched (call to javafx.application.Application.launch). \
If you wish to have manual launch, you can update the build property :
```properties
quarkus.fx.auto-launch = false
```

And use such code to start the app
You'll have to use a custom Main in your application
```java
package io.quarkiverse.fxapp;

import io.quarkiverse.fx.FxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javafx.application.Application;

@QuarkusMain
public class CDIApplication implements QuarkusApplication {

  public static void main(final String[] args) {
    Quarkus.run(CDIApplication.class);
  }

  @Override
  public int run(final String... args) {
    Application.launch(FxApplication.class, args);
    return 0;
  }
}
```

For a sample app / usage, check :
https://github.com/CodeSimcoe/quarkus-using-fx-extension
