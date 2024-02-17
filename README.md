# quarkus-fx

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.fx/quarkus-fx-parent?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.fx/quarkus-fx)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-3-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->
This Quarkus extension allows you to use JavaFX in your Quarkus application. \
It will allow component injection in FX Controllers and will allow you to use CDI events to register on primary stage creation.

Live reload is still problematic and will be studied in the future.

You will be able to register on primary stage creation event via such code example.
```java
public class QuarkusFxApp {

  @Inject
  FXMLLoader fxmlLoader;

  public void start(@Observes final FxStartupEvent event) {
    try {
      InputStream fxml = this.getClass().getResourceAsStream("/app.fxml");
      Parent fxmlParent = this.fxmlLoader.load(fxml);

      Stage stage = event.primaryStage();
      
      Scene scene = new Scene(fxmlParent);
      stage.setScene(scene);
      stage.show();

    } catch (IOException e) {
      // Handle error
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

For some sample apps and usage, check the `samples/` directory.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/CodeSimcoe"><img src="https://avatars.githubusercontent.com/u/110094118?v=4?s=100" width="100px;" alt="Clément de Tastes"/><br /><sub><b>Clément de Tastes</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-fx/commits?author=CodeSimcoe" title="Code">💻</a> <a href="#maintenance-CodeSimcoe" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ghazyami"><img src="https://avatars.githubusercontent.com/u/7247810?v=4?s=100" width="100px;" alt="Ghazy Abdallah"/><br /><sub><b>Ghazy Abdallah</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-fx/commits?author=ghazyami" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://www.jboss.org"><img src="https://avatars.githubusercontent.com/u/332210?v=4?s=100" width="100px;" alt="Scott M Stark"/><br /><sub><b>Scott M Stark</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-fx/commits?author=starksm64" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
