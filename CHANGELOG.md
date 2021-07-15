# Change Log

All notable changes to this project will be documented in this file.
This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Fixed
- Show all licenses if more than one classifier specified ([#52](https://github.com/pilosus/pip-license-checker/issues/52))
- Support Artistic license ([#45](https://github.com/pilosus/pip-license-checker/issues/45))
- Support Zope Public License ([#46](https://github.com/pilosus/pip-license-checker/issues/46))
- Add GPL linking exception to permissive licenses ([#50](https://github.com/pilosus/pip-license-checker/issues/50))

## [0.15.0] - 2021-07-14
### Fixed
- Support legacy default values for missing license field in package metadata ([#47](https://github.com/pilosus/pip-license-checker/issues/47))
- Use most detailed license trove classifier ([#44](https://github.com/pilosus/pip-license-checker/issues/44))
- Skip unspecific license classifiers ([#43](https://github.com/pilosus/pip-license-checker/issues/43))
- Improve BSD-family licenses matching

## [0.14.0] - 2021-07-10
### Fixed
- Used multi-stage Dockerfile to reduce docker image size from 390Mb to ~230Mb

## [0.13.3] - 2021-06-29
### Fixed
- Separate GHA workflows for master push and pull requests

## [0.13.2] - 2021-06-29
### Fixed
- Do not use PGP when pushing artifacts to Clojars in GHA

## [0.13.1] - 2021-06-29
### Added
- Clojars fully qualified group name added

## [0.13.0] - 2021-04-30
### Added
- HTTP requests concurrency added


## [0.12.0] - 2021-03-14
### Added
- CLI option `-f` or `--fail LICENSE_TYPE` to return non-zero exit code (1) if specified license type is found. More that one option `-f` may be specified.


## [0.11.0] - 2021-03-13
### Added
- CLI option `-t` or `--[no-]with-totals` to print totals for license types
- CLI option `-o` or `--[no-]totals-only` to print only totals for license types, skipping table of requirements
- CLI option `-d` or `--[no-]table-headers` to print table headers

## [0.10.0] - 2021-01-23
### Added
- [Spec](https://clojure.org/guides/spec) and [Test.check](https://clojure.github.io/test.check/index.html) used for generative testing and functions instrumenting.
### Fixed
- Fixed bug with version resolution for specifier with upper-case characters (thanks specs!)

## [0.9.0] - 2021-01-03
### Added
- ``--pre`` option to include pre-release and development versions in version resolution
### Changed
- By default exclude pre-release and development versions from version resolution. Use such versions if and only if explicitly asked with ``--pre`` option, or no other versions available to satisfy specifiers

## [0.8.0] - 2021-01-03
### Fixed
- Fix version resolution for exclusive order comparison, i.e. ``>`` and ``<``

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

[Unreleased]: https://github.com/pilosus/pip-license-checker/compare/0.15.0...HEAD
[0.15.0]: https://github.com/pilosus/pip-license-checker/compare/0.14.0...0.15.0
[0.14.0]: https://github.com/pilosus/pip-license-checker/compare/0.13.3...0.14.0
[0.13.3]: https://github.com/pilosus/pip-license-checker/compare/0.13.2...0.13.3
[0.13.2]: https://github.com/pilosus/pip-license-checker/compare/0.13.1...0.13.2
[0.13.1]: https://github.com/pilosus/pip-license-checker/compare/0.13.0...0.13.1
[0.13.0]: https://github.com/pilosus/pip-license-checker/compare/0.12.0...0.13.0
[0.12.0]: https://github.com/pilosus/pip-license-checker/compare/0.11.0...0.12.0
[0.11.0]: https://github.com/pilosus/pip-license-checker/compare/0.10.0...0.11.0
[0.10.0]: https://github.com/pilosus/pip-license-checker/compare/0.9.0...0.10.0
[0.9.0]: https://github.com/pilosus/pip-license-checker/compare/0.8.0...0.9.0
[0.8.0]: https://github.com/pilosus/pip-license-checker/compare/0.7.0...0.8.0
[0.7.0]: https://github.com/pilosus/pip-license-checker/compare/0.6.1...0.7.0
[0.6.1]: https://github.com/pilosus/pip-license-checker/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/pilosus/pip-license-checker/compare/0.5.0...0.6.0
[0.5.0]: https://github.com/pilosus/pip-license-checker/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/pilosus/pip-license-checker/compare/0.3.0...0.4.0
[0.3.0]: https://github.com/pilosus/pip-license-checker/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/pilosus/pip-license-checker/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/pilosus/pip-license-checker/compare/0.1.0...0.1.1
