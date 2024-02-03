# quarkus-fx
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
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

For a sample app / usage, check :
https://github.com/CodeSimcoe/quarkus-using-fx-extension

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/CodeSimcoe"><img src="https://avatars.githubusercontent.com/u/110094118?v=4?s=100" width="100px;" alt="Clément"/><br /><sub><b>Clément</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-fx/commits?author=CodeSimcoe" title="Code">💻</a> <a href="#maintenance-CodeSimcoe" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ghazyami"><img src="https://avatars.githubusercontent.com/u/7247810?v=4?s=100" width="100px;" alt="Ghazy Abdallah"/><br /><sub><b>Ghazy Abdallah</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-fx/commits?author=ghazyami" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!