# Change Log

All notable changes to this project will be documented in this file.
This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.50.0] - 2025-07-24

### Added

- `PSF-2.0` license support ([#143](https://github.com/pilosus/pip-license-checker/issues/143))

## [0.49.0] - 2025-07-22

### Changed

- PyPI JSON API's `license_expr` is used as a primary source for the
  license name (SPDX identifier). If absent, fall back to the
  `license` field
  ([#152](https://github.com/pilosus/pip-license-checker/issues/152)).

- Bump Docker base images to `eclipse-temurin:21`
- Bump Clojure dependencies


## [0.48.0] - 2023-04-28

### Changed
- Code refactoing to remove `defrecord` in favour of
  [clojure.spec.alpha](https://clojure.org/guides/spec)

## [0.47.0] - 2023-04-14

### Changed
- Docker base image's pinned sha256 digest removed for quicker and
  easier security updates

### Fixed
- Clojure package `org.pilosus/pip-license-checker` to be updated
  since the release `0.45.0`

## [0.46.1] - 2023-03-20

### Fixed
- Address the bug with exception throwing for leading zeros in integer
  parts of the Python-native package versions
  ([#138](https://github.com/pilosus/pip-license-checker/issues/138))

## [0.46.0] - 2023-03-15

### Fixed
- Address the bug with supporting `BigInteger` in epoch, major, minor,
  and patch parts of the Python-native package versions
  ([#136](https://github.com/pilosus/pip-license-checker/issues/136))

## [0.45.0] - 2023-03-11

Release **breaks backward compatibility** by adding mandatory `Misc`
column to the reports. See `Changed` sections for more details.

### Changed
- `Misc` column no longer depends on the verbosity level and is always
  shown. For `stdout` reports (default format) visibility of the
  column can be suppresed via custom `formatter` (e.g. `%s %s %s` to
  show only first three columns)
- Default `--formatter` option spans 4 columns (`Dependency`, `License
  name`, `License type`, `Misc`) and equals to `%-35s %-55s %-20s
  %-40s`.
- `--totals` formatting assumes that the first two columns delimited
  with the same separator; the first separator is used (by default a
  single space)

### Added
- Report output format option `--report-format` to support `stdout`
  (default tabular report printed to the standard output), `json`,
  `json-pretty` and `csv` formats
  ([#90](https://github.com/pilosus/pip-license-checker/issues/90))

## [0.44.0] - 2023-02-25

### Fixed
- Allow pre-release versions for Python native packages in case of
  exact equal (`==`) or arbitrary string equal (`===`) specifiers
  ([#132](https://github.com/pilosus/pip-license-checker/issues/132))

## [0.43.0] - 2023-02-10

### Fixed
- Resolve versions for yanked Python packages for
  [exact version matching](https://peps.python.org/pep-0440/#version-matching) and
  [arbitrary equality](https://peps.python.org/pep-0440/#arbitrary-equality)
  [#125](https://github.com/pilosus/pip-license-checker/issues/125)
- Resolved pre-release versions only when `--pre` option is specified
  [#126](https://github.com/pilosus/pip-license-checker/issues/126)

### Changed
- Verbosity level is defined by number of `-v` (or `--verbose`)
  options: errors only `-v`; info and errors `-vv`; debug, info and
  errors `-vvv`
- Fallback to GitHub API for license detection is visible for info
  verbosity level
  ([#89](https://github.com/pilosus/pip-license-checker/issues/89))

## [0.42.1] - 2023-01-15

### Fixed
- Addressed a bug in Python package version parsing for
  PEP517-non-compliant package filenames
  [#123](https://github.com/pilosus/pip-license-checker/issues/123)

## [0.42.0] - 2023-01-10

### Changed
- Code formatting

## [0.42.0-SNAPSHOT] - 2022-12-30

### Changed
- Python packages version resolution migrated to PyPI Simple API for
  available versions as PyPI JSON API had deprecated releases
  information
  ([#108](https://github.com/pilosus/pip-license-checker/issues/108)).

## [0.41.1] - 2022-12-26

### Changed
- Clojars project moved to
  [org.pilosus](https://clojars.org/org.pilosus/pip-license-checker/)
  group.

## [0.41.0] - 2022-12-26

Release **breaks backward compatibility** if you use the tool as a
library in your Clojure code (see `Changed` section).

Release **deprecates** some command-line options in favour of the new
ones. Deprecated options **will be removed in future releases**. Until
removed, backward compatibility is supported for the deprecated
options when used explicitly (see `Deprecated` section).

### Added
- Option `--[no-]parallel` (default true) to toggle parallel requests to
  third-party APIs.
- Option `--[no-]exit` (default true) to toggle explicit `System/exit` call
  upon program completion. Used to avoid exit when calling `main` in
  your Clojure code as opposed to CLI invocation.

### Changed
- Massive refactoring to change function names and signatures, add new
  functions, remove unused to better separate report generation and
  representation (see [PR 120](https://github.com/pilosus/pip-license-checker/pull/120)).
- Dependencies `org.clojars.vrs/cocoapods-acknowledgements-licenses`
  and `org.clojars.vrs/gradle-licenses` moved in to the projects code
  base. See changes in namespaces `pip-license-checker.file` and
  `pip-license-checker.external`.
- Report headers renamed
- Clojure version upgraded from `1.10.1` to `1.11.1`.

### Deprecated
- Option `--[no-]with-totals` deprecated in favour of `--[no-]totals`
- Option `--[no-]table-headers` deprecated in favour of `--[no-]headers`

## [0.40.0] - 2022-12-04

### Added
- GitHub API versioning header added to requests to prevent using
  backward-incompatible API versions
  ([#118](https://github.com/pilosus/pip-license-checker/issues/118))

## [0.39.0] - 2022-12-02

### Changed
- Verbose output imrpoved for Java-native exceptions with no
  `clj-http` specific meta
  ([#116](https://github.com/pilosus/pip-license-checker/issues/116))
- Prefix format used in errors output changed from
  `[System] status-code message` to `System::subsystem error message`

## [0.38.0] - 2022-11-27

Release **breaks backward compatibility** if you use the tool as a
library in your Clojure code (see `Changed` section).

### Added
- CLI option `--verbose` (or `-v` for short) to make output
  verbose. Options adds `Misc` column for error messages to the
  report.
  ([#105](https://github.com/pilosus/pip-license-checker/issues/105))

### Changed
- Massive code refactoring: multiple functions renamed, a few removed,
  some function arguments order changed
  (see [PR 115](https://github.com/pilosus/pip-license-checker/pull/115/files)).

## [0.37.0] - 2022-11-23
### Changed
- Docker base image switched over vendor-agnostic Eclipse Temurin JRE 17.

## [0.36.0] - 2022-11-22
### Added
- Environment variable `GITHUB_TOKEN` is used as a default value for
  `--github-token` option. Use as a safer alternative in CI/CD
  pipelines so that OAuth token is not exposed in the logs as a plain text.

## [0.35.0] - 2022-11-18
### Added
- Rate limiting for GitHub public API requests. Option `--rate-limits
   requests/milliseconds` is used for all external API requests (PyPI, GitHub)
- Option `--github-token` for OAuth tokens to increase GitHub API requests rate-limits
  ([#109](https://github.com/pilosus/pip-license-checker/issues/109))

## [0.34.0] - 2022-07-17
### Added
-  Rate limiting for PyPI's public API requests with `--rate-limits
   requests/milliseconds` CLI option. **NB!** Default rate limits are
   set to 120 requests per minute. This release **may slow down** your
   Python dependencies check significantly in case large number of
   packages
   ([#101](https://github.com/pilosus/pip-license-checker/issues/101))

## [0.33.0] - 2022-06-25
### Fixed
- Parsing long numbers in Python versions ([#99](https://github.com/pilosus/pip-license-checker/issues/99))

## [0.32.0] - 2022-06-23
### Added
- `lein ancient` to check outdated deps

### Changed
- Use `clojure:openjdk-11-lein-slim-buster` Docker base image for a build step
- Bump Clojure version and project's deps

## [0.31.0] - 2021-11-19
### Added
- Support both `zlib` and `zlib/libpng` (a.k.a. zlib with Acknowledgement) licenses

## [0.30.0] - 2021-09-04
### Added
- `--formatter` option to specify a
  [printf](https://en.wikipedia.org/wiki/Printf_format_string)-style
  formatter string for report formatting. The formatter is expected to
  cover 3 columns of string data, e.g. `--formatter '%-35s %-55s %-30s'`.
- `edn` format for `--external-format` option to parse
  [EDN](http://edn-format.org/) generated by the
  [lein-licenses](https://github.com/technomancy/lein-licenses) plugin.

## [0.29.0] - 2021-08-30
### Added
- GitHub action to check `pip-license-checker` deps licenses

## [0.28.1] - 2021-08-29
### Fixed
- Project's license metadata fixed

## [0.28.0] - 2021-08-29

Release **breaks backward compatibility** by changing from permissive
MIT license to the weak copyleft license of EPL-2.0 or GNU GPLv2 or any
later with the GNU Classpath Exception.

### Changed
- MIT license changed to Eclipse Public License 2.0 or GNU General
  Public License as published by the Free Software Foundation, either
  version 2 of the License, or (at your option) any later version, with
  the GNU Classpath Exception

## [0.27.0] - 2021-08-29
### Added
- Options for `csv` external file format to specify a package name
  column index and a license name column index respectively:
  `:package-column-index [integer]`, `license-column-index
  [integer]`. To pass options in use:
  ```
  --external-options '{:package-column-index 0 :license-column-index 1}'
  ```

## [0.26.0] - 2021-08-28
### Fixed
- NullPointerException fixed for `--exclude` option regex matching for packages with `nil` name.
- NullPointerException fixed for `--exclude-license` option regex matching for packages with `nil` license name.

## [0.25.0] - 2021-08-27
### Added
- `gradle` format for `--external-format` option to parse JSON generated by
  [gradle-license-plugin](https://github.com/jaredsburrows/gradle-license-plugin).
  The format is supported via a standalone plugin [gradle-licenses](https://github.com/pilosus/gradle-licenses).

## [0.24.0] - 2021-08-27
### Added
- `--external-format` option to specify one of the supported
  external file formats: `csv` (default value), `cocoapods`.
- Support for `cocoapods` external file format ([Property list file](https://en.wikipedia.org/wiki/Property_list))
  generated by [CocoaPods Acknowledgements plugin](https://github.com/CocoaPods/cocoapods-acknowledgements).
  The format is supported via a standalone plugin
  [cocoapods-acknowledgements-licenses](https://github.com/pilosus/cocoapods-acknowledgements-licenses).
- `--external-options` option to specify a string of options in
  [EDN format](http://edn-format.org) to be used when parsing external files.
  See respective external-format plugin's documenations for available options.

### Removed
- `--external-csv-headers` option removed.
  Use `--external-options '{:skip-header true}'` (present by default) to reproduce the behaviour.
- The following short option names removed: `-p`, `-t`, `-o`, `-d`, `-m`, `-el`. Use respective full option names.


## [0.23.0] - 2021-08-26
### Added
- `--exclude-license` option for PCRE to remove packages with matching license names.

## [0.22.0] - 2021-08-15
### Added
- Better README documentation: clear project description, fancy
  badges, installation/usage improvements, FAQ section.

## [0.21.0] - 2021-08-15
### Added
- Permissive licenses support: ODC-BY
- Weak copyleft licenses support: ODbL

## [0.20.0] - 2021-08-14
### Added
- External CSV files with prefetched license names support with `--external` option.
- External CSV files with/without header line support with `--external-csv-headers` option.
- Permissive licenses support: CC0, CC-BY, WTFPL.

## [0.19.0] - 2021-08-13
### Added
- Support for GPL with linking exception licenses. Instead of
  `StrongCopyleft` these are recognised as `WeakCopyleft`.

## [0.18.0] - 2021-08-01

Release **breaks backward compatibility** by introducing fine-grained
copyleft detection and moving some licenses between permissive and
weak copyleft types.

### Removed
- License type `Copyleft` has been removed from result output in
  favour of more granular copyleft types: `WeakCopyleft` (weak or
  partial copyleft licenses like MPL, LGPL, or GPL with linking
  exception), `StrongCopyleft` (GPL), `NetworkCopyleft` (AGPL, OSL)

### Added
- `--fail` option is extended with the new values: `WeakCopyleft`, `StrongCopyleft`, `NetworkCopyleft`.
- Copyleft over the network (private use loophole fixed) licenses: RPSL, Sybase Open Watcom Public License
- Strong copyleft licenses: RPL, Sleepycat
- Weak copyleft licenses: GPL linking exception, Motosoto, Nokia Open
  Source License, Netscape Public License, Netizen Open Source
  License, BitTorrent Open Source License, Sun Public License, Eclipse
  Public License, Eurosym License, CPL, Common Development and
  Distribution License, Apple Public Source License, Microsoft
  Reciprocal License, SIL Open Font License
- Permissive licenses: AAL, Eiffel Forum License, MirOS, Intel Open
  Source License, Microsoft Public License, PostgreSQL License, Q
  Public License, Repoze Public License, UPL, UIUC, Vovida Software
  License, X.Net License

### Changed
- `--fail` option `Copyleft` is still suppported and is triggered for
  all licenses under newly introduced `WeakCopyleft`,
  `StrongCopyleft`, `NetworkCopyleft` types.
- Following licenses were moved from `Permissive` to `WeakCopyleft`: CeCILL-2.1, CeCILL-C, LGPL
- Following licenses were moved from `Copyleft` to `WeakCopyleft`: MPL
- Following licenses were moved from `Copyleft` to `StrongCopyleft`: GPL, IBM Public License
- Following licenses were moved from `Copyleft` to `NetworkCopyleft`: AGPL, OSL


## [0.17.0] - 2021-07-18
### Added
- Flag `--fails-only` to print only licenses found for `--fail` flags ([#57](https://github.com/pilosus/pip-license-checker/issues/57))

## [0.16.0] - 2021-07-15
### Added
- Show all licenses if more than one classifier specified ([#52](https://github.com/pilosus/pip-license-checker/issues/52))
- Support Artistic license ([#45](https://github.com/pilosus/pip-license-checker/issues/45))
- Support Zope Public License ([#46](https://github.com/pilosus/pip-license-checker/issues/46))

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

[Unreleased]: https://github.com/pilosus/pip-license-checker/compare/0.50.0...HEAD
[0.59.0]: https://github.com/pilosus/pip-license-checker/compare/0.49.0...0.50.0
[0.49.0]: https://github.com/pilosus/pip-license-checker/compare/0.48.0...0.49.0
[0.48.0]: https://github.com/pilosus/pip-license-checker/compare/0.47.0...0.48.0
[0.47.0]: https://github.com/pilosus/pip-license-checker/compare/0.46.1...0.47.0
[0.46.1]: https://github.com/pilosus/pip-license-checker/compare/0.46.0...0.46.1
[0.46.0]: https://github.com/pilosus/pip-license-checker/compare/0.45.0...0.46.0
[0.45.0]: https://github.com/pilosus/pip-license-checker/compare/0.44.0...0.45.0
[0.44.0]: https://github.com/pilosus/pip-license-checker/compare/0.43.0...0.44.0
[0.43.0]: https://github.com/pilosus/pip-license-checker/compare/0.42.1...0.43.0
[0.42.1]: https://github.com/pilosus/pip-license-checker/compare/0.42.0...0.42.1
[0.42.0]: https://github.com/pilosus/pip-license-checker/compare/0.42.0-SNAPSHOT...0.42.0
[0.42.0-SNAPSHOT]: https://github.com/pilosus/pip-license-checker/compare/0.41.1...0.42.0-SNAPSHOT
[0.41.1]: https://github.com/pilosus/pip-license-checker/compare/0.41.0...0.41.1
[0.41.0]: https://github.com/pilosus/pip-license-checker/compare/0.40.0...0.41.0
[0.40.0]: https://github.com/pilosus/pip-license-checker/compare/0.39.0...0.40.0
[0.39.0]: https://github.com/pilosus/pip-license-checker/compare/0.38.0...0.39.0
[0.38.0]: https://github.com/pilosus/pip-license-checker/compare/0.37.0...0.38.0
[0.37.0]: https://github.com/pilosus/pip-license-checker/compare/0.36.0...0.37.0
[0.36.0]: https://github.com/pilosus/pip-license-checker/compare/0.35.0...0.36.0
[0.35.0]: https://github.com/pilosus/pip-license-checker/compare/0.34.0...0.35.0
[0.34.0]: https://github.com/pilosus/pip-license-checker/compare/0.33.0...0.34.0
[0.33.0]: https://github.com/pilosus/pip-license-checker/compare/0.32.0...0.33.0
[0.32.0]: https://github.com/pilosus/pip-license-checker/compare/0.31.0...0.32.0
[0.31.0]: https://github.com/pilosus/pip-license-checker/compare/0.30.0...0.31.0
[0.30.0]: https://github.com/pilosus/pip-license-checker/compare/0.29.0...0.30.0
[0.29.0]: https://github.com/pilosus/pip-license-checker/compare/0.28.1...0.29.0
[0.28.1]: https://github.com/pilosus/pip-license-checker/compare/0.28.0...0.28.1
[0.28.0]: https://github.com/pilosus/pip-license-checker/compare/0.27.0...0.28.0
[0.27.0]: https://github.com/pilosus/pip-license-checker/compare/0.26.0...0.27.0
[0.26.0]: https://github.com/pilosus/pip-license-checker/compare/0.25.0...0.26.0
[0.25.0]: https://github.com/pilosus/pip-license-checker/compare/0.24.0...0.25.0
[0.24.0]: https://github.com/pilosus/pip-license-checker/compare/0.23.0...0.24.0
[0.23.0]: https://github.com/pilosus/pip-license-checker/compare/0.22.0...0.23.0
[0.22.0]: https://github.com/pilosus/pip-license-checker/compare/0.21.0...0.22.0
[0.21.0]: https://github.com/pilosus/pip-license-checker/compare/0.20.0...0.21.0
[0.20.0]: https://github.com/pilosus/pip-license-checker/compare/0.19.0...0.20.0
[0.19.0]: https://github.com/pilosus/pip-license-checker/compare/0.18.0...0.19.0
[0.18.0]: https://github.com/pilosus/pip-license-checker/compare/0.17.0...0.18.0
[0.17.0]: https://github.com/pilosus/pip-license-checker/compare/0.16.0...0.17.0
[0.16.0]: https://github.com/pilosus/pip-license-checker/compare/0.15.0...0.16.0
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
