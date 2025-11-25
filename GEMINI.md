# Project: mejais

## Project Overview

This project is a JavaFX-based desktop application called "mejais". It appears to be a tool for the game League of Legends, likely providing stats, recommendations, or other in-game information. It uses Maven for dependency management and includes libraries for interacting with the Riot Games API (`orianna`), handling JSON (`jackson-databind`), and logging (`slf4j`).

The application is structured with a clear separation of concerns, with packages for controllers, models, services, and utilities. The UI is defined in FXML files and styled with CSS.

## Building and Running

### Prerequisites

*   Java 21
*   Maven

### Commands

*   **Build the project:**
    ```bash
    mvn clean install
    ```
*   **Run the application:**
    ```bash
    mvn javafx:run
    ```
*   **Run tests:**
    ```bash
    mvn test
    ```
*   **Package the application (skip tests):**
    ```bash
    mvn -DskipTests package
    ```

## Development Conventions

*   **Code Style:** Adhere to standard Java 21 style with 4-space indentation.
*   **Naming Conventions:**
    *   Controllers are placed in `org.example.controller` and suffixed with `Controller` (e.g., `GameController`).
    *   FXML files follow the `*-view.fxml` pattern.
    *   CSS themes are located in `src/main/resources/org/example/css`.
*   **Testing:**
    *   Tests are located in `src/test/java/org/example`.
    *   Test classes should be named with a `*Test` suffix.
    *   The project uses JUnit Jupiter for testing.
*   **Commits and Pull Requests:**
    *   Commit messages should have a concise, imperative subject line.
    .
