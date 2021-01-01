# Change Log

All notable changes to this project will be documented in this file.
This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Fixed
- Nothing here yet

## [0.7.0] - 2021-01-01
### Fixed
- When resolving version exclude prereleases (i.e. dev, rc, alpha, beta versions)
- NullPointerException for invalid versions; skip those

## [0.6.1] - 2021-01-01
### Added
- Docker image to run app inside container

## [0.6.0] - 2020-12-26
### Fixed
- Version resolution for all specifiers

## [0.5.0] - 2020-12-20
### Added
- Check license name using GitHub API as a fallback when no license found on the PyPI

### Fixed
- Code formatting (after introduction of [clj-kondo](https://github.com/borkdude/clj-kondo) and [cljfmt](https://github.com/weavejester/cljfmt))

## [0.4.0] - 2020-12-16
### Added
- Multiple -r options support
- Exit status codes
- Usage examples in docs

### Fixed
- Massive code refactoring
- CLI args parsing rebuilt

## [0.3.0] - 2020-12-06
### Added
- Permissive license detection

## [0.2.0] - 2020-12-05
### Added
- Scan requirements files with option -r/--requirement

## [0.1.1] - 2020-11-22
### Changed
- Exception handling
- Copyleft licenses recognition
- License set to MIT

### Removed
- Leiningen's documentation template

### Fixed
- Checks for packages with no version specified

## 0.1.0 - SNAPSHOT - 2020-11-21
### Added
- Structure for Leiningen app project

[Unreleased]: https://github.com/pilosus/pip-license-checker/compare/0.7.0...HEAD
[0.7.0]: https://github.com/pilosus/pip-license-checker/compare/0.6.1...0.7.0
[0.6.1]: https://github.com/pilosus/pip-license-checker/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/pilosus/pip-license-checker/compare/0.5.0...0.6.0
[0.5.0]: https://github.com/pilosus/pip-license-checker/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/pilosus/pip-license-checker/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/pilosus/pip-license-checker/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/pilosus/pip-license-checker/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/pilosus/pip-license-checker/compare/0.1.0...0.1.1
