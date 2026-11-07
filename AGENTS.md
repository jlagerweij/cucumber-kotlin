# Agent instructions

## Commits and pull requests

- Do not add `Co-Authored-By` trailers to commit messages.
- Do not add AI attribution (such as "Generated with ...") to commit messages or pull request descriptions.
- Every pull request must change the plugin version in `gradle.properties` (`version=YYYY.N.P`), in a separate
  commit named `Bump version to <version>`. Increase the patch number `P` (e.g. `2026.3.1` → `2026.3.2`), unless the
  pull request adds compatibility with a new IntelliJ IDEA version: then use that IDE version with patch `0`
  (e.g. `2026.4.0`). Skip the bump only if the version already changed on the branch compared to `main`.
