# Contributing to Mute Point

## Getting Started

1. Fork the repository
2. Clone your fork
3. Create a feature branch: `git checkout -b feature/my-feature`
4. Set up `local.properties` (see README.md)
5. Make your changes
6. Run tests: `./gradlew testDebugUnitTest`
7. Run lint: `./gradlew lintDebug`
8. Run detekt: `./gradlew detekt`
9. Commit with a clear message
10. Open a pull request

## Code Style

- Follow Kotlin official code style (`kotlin.code.style=official`)
- Use 4-space indentation
- Max line length: 120 characters
- No wildcard imports
- No `!!` operator (use safe calls or explicit null checks)
- Add KDoc to all public functions and classes

## Security

- **Never commit secrets** — use `local.properties` or environment variables
- Run `pre-commit install` to enable gitleaks scanning
- If you find a security issue, report it privately (see SECURITY.md)

## Testing

- Add unit tests for new domain logic
- Add instrumented tests for new UI screens
- Ensure all tests pass before submitting a PR

## Pull Request Process

1. Ensure CI passes (build, test, lint, detekt, secret scan)
2. Request review from a maintainer
3. Address review feedback
4. Squash commits if requested
