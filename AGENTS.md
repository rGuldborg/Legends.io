# Repository Guidelines

## Project Structure & Module Organization
This Maven-based JavaFX app keeps executable code in `src/main/java`, grouped by responsibility: `controller` wires UI logic, `model` will host data abstractions, `view` encapsulates UI helpers, and `util` holds shared services such as `ThemeManager`. UI assets live in `src/main/resources/org/example/**` where `fxml/` stores layouts, `css/` stores skins, and `images/` carries bundled art. Keep new resources inside the matching `org/example` namespace so `FXMLLoader` and `ThemeManager` can resolve them on the classpath. Tests belong in `src/test/java`, mirroring the package tree.

## Build, Test, and Development Commands
- `mvn clean install` – compile against Java 21, run unit tests, and stage artifacts under `target/`.
- `mvn javafx:run` – launch the desktop client using `org.example.Main` and the configured JavaFX runtime.
- `mvn -DskipTests package` – produce a build quickly when UI-only changes make tests irrelevant.
Use `mvn --quiet` during CI to reduce log noise, and delete `target/` before measuring bundle sizes.

## Coding Style & Naming Conventions
Adhere to standard Java 21 style: 4-space indentation, braces on the same line, and descriptive class names. Place controllers under `org.example.controller` and suffix them with `Controller` to align with `fx:controller` attributes (`GameController`, `MainController`). FXML files follow the `*-view.fxml` pattern; keep CSS themes as `*.css` inside `org/example/css` to stay compatible with `ThemeManager.applyTheme`. Favor enums or records for new model types and keep logging statements concise (`System.out.println` currently traces lifecycle checkpoints).

## Testing Guidelines
Even though no tests exist yet, wire new ones under `src/test/java/org/example/...` and name them `*Test` to leverage Maven Surefire defaults. Use JUnit Jupiter (`@Test`, `@BeforeEach`) and mock JavaFX nodes via TestFX or headless doubles where possible. Run `mvn test` before every push; target at least basic coverage for controllers that manipulate state (e.g., verifying placeholder images and list population in `GameController`). Document any UI flows that still require manual verification in the PR description.

## Commit & Pull Request Guidelines
Recent history mixes semantic releases (`1.0.0`) with descriptive subjects (`Refine header layout alignment`). Follow that pattern: a concise, imperative subject ≤72 chars and optional release tag commits when bumping versions. Each PR should include: goal summary, linked issue or ticket, testing notes (`mvn test`, manual steps), and screenshots/gifs for UI-affecting work. Keep diffs focused; push schema or asset updates separately when possible, and mention resource additions (new FXML/CSS) explicitly so reviewers inspect them closely.
