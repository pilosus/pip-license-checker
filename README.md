# pip-license-checker

[![codecov](https://codecov.io/gh/pilosus/pip-license-checker/branch/main/graph/badge.svg?token=MXN6PDETET)](https://codecov.io/gh/pilosus/pip-license-checker)
[![Docker Image Version (latest semver)](https://img.shields.io/docker/v/pilosus/pip-license-checker?color=blue&label=docker%20image&sort=semver)](https://hub.docker.com/r/pilosus/pip-license-checker/)
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.vrs/pip-license-checker.svg)](https://clojars.org/org.clojars.vrs/pip-license-checker)

License compliance tool. Detect license names and types for Python
PyPI packages. Identify license types for given license names obtained
by third-party tools. Great coverage of free/libre and open source
licenses of all types:
[public domain](https://en.wikipedia.org/wiki/Public-domain-equivalent_license),
[permissive](https://en.wikipedia.org/wiki/Permissive_software_license),
[copyleft](https://en.wikipedia.org/wiki/Copyleft).

Check Python dependencies, including in `requirements.txt` format for
`pip` package installer, without Python and its tooling
presence. Check license types for dependencies and their licenses
obtained by third-party tools (e.g. JavaScript's
[license-checker](https://www.npmjs.com/package/license-checker))


## Installation

You can install `pip-license-checker` either by pulling a Docker
image, builing from the source code or plugging-in GitHub Action to
your CI pipeline.

### I. Docker

There are two options for getting a docker image:

1. Pulling an official image from [Docker Hub](https://hub.docker.com/r/pilosus/pip-license-checker/)

```
docker pull pilosus/pip-license-checker:0.25.0
```

Use specific version tag (it's matching version of the tool in the repo) or just `latest`.


2. Building a docker image yourself

```bash
git clone https://github.com/pilosus/pip-license-checker.git
cd pip-license-checker
docker build -t pilosus/pip-license-checker .
```

### II. Compiling from source code

1. Install [Leiningen](https://leiningen.org/)

2. Get the source code

```bash
git clone https://github.com/pilosus/pip-license-checker.git
cd pip-license-checker
```

It's enough to start using the tool with `lein`. But you can
optionally compile a standalone jar-file too:

3. (Optional) Compile uberjar file

```bash
lein uberjar
cd target/uberjar
java -jar pip-license-checker-[version]-standalone.jar [args] [options]
```

### III. GitHub Action for CI integration

If your project is hosted by GitHub, try the [GitHub
Action](https://github.com/pilosus/action-pip-license-checker) based
on `pip-license-checker`.


## Usage


### Docker

```bash
docker run -it --rm pilosus/pip-license-checker:0.25.0 \
  java -jar app.jar 'aiostream==0.4.3' 'pygit2' 'aiohttp>3.7.1'
```

In case of checking files (e.g. with `--requirements` or `--external`
tool's options) mount a host directory containing the files with
docker's `-v` option:


```bash
docker run -v `pwd`:/volume \
    -it --rm pilosus/pip-license-checker:0.25.0 \
    java -jar app.jar --exclude 'pylint.*' \
    --requirements '/volume/requirements.txt' \
    --external '/volume/licenses.csv' \
    --fail StrongCopyleft --fails-only
```

### Command line tool

Examples below assume you are using `lein` tool. If you'd like to use
standalone jar file, just substitute `lein run` with `java -jar
pip-license-checker-[version]-standalone.jar`.


```bash
### see usage message
lein run

### check a single package
lein run piny==0.6.0
lein run aiostream

### include pre-release and development versions
lein run --pre aiohttp

### scan all packages in requirements file
lein run --requirements resources/requirements.txt

aiohttp:3.7.2                       Apache Software License                                 Permissive
piny:0.6.0                          MIT License                                             Permissive
aiostream:0.4.3                     GPLv3                                                   StrongCopyleft
mo-collections:4.30.21121           Mozilla Public License 2.0 (MPL 2.0)                    WeakCopyleft
aiocache:0.11.1                     BSD 3-Clause "New" or "Revised" License                 Permissive
aiokafka:0.6.0                      Apache Software License                                 Permissive
aiopg:122.3.5                       Error                                                   Error
telegram-bot-framework:3.15.2       GNU Affero General Public License v3 or later (AGPLv3+) NetworkCopyleft
aio-throttle:1.6.2                  MIT License                                             Permissive
workflow-tools:0.6.0                Apache Software License                                 Permissive
Synx:0.0.3                          Other/Proprietary License                               Other

### scan packages matching regex pattern
### e.g. all lines except containing "aio.*" packages
lein run --requirements resources/requirements.txt --exclude 'aio.*'

piny:0.6                       MIT License                    Permissive
workflow-tools:0.5.0           Apache Software License        Permissive
Synx                           Other/Proprietary License      Other
```

## Help

Run application with `lein run` or use `--help` option with standalone
jar for more details.

```bash
lein run

pip-license-checker - license compliance tool to identify dependencies license names and types.

Usage:
pip-license-checker [options]... [package]...

Description:
  package	List of package names in format `name[specifier][version]`

  -r, --requirements REQUIREMENT_NAME        []                                      Requirement file name to read
  -x, --external FILE_NAME                   []                                      CSV file with prefetched license data in format: package-name,license-name[,...]
      --external-format LICENSE_FILE_FORMAT  csv                                     External file format: csv, cocoapods, gradle
      --external-options OPTS_EDN_STRING     {:skip-header true, :skip-footer true}  String of options map in EDN format
  -f, --fail LICENSE_TYPE                    #{}                                     Return non-zero exit code if license type is found
  -e, --exclude REGEX                                                                PCRE to exclude packages with matching names
      --exclude-license REGEX                                                        PCRE to exclude packages with matching license names
      --[no-]pre                                                                     Include pre-release and development versions. By default, use only stable versions
      --[no-]with-totals                                                             Print totals for license types
      --[no-]totals-only                                                             Print only totals for license types
      --[no-]table-headers                                                           Print table headers
      --[no-]fails-only                                                              Print only packages of license types specified with --fail flags
  -h, --help                                                                         Print this help message

Examples:
pip-license-checker django
pip-license-checker aiohttp==3.7.2 piny==0.6.0 django
pip-license-checker --pre 'aiohttp<4'
pip-license-checker --with-totals --table-headers --requirements resources/requirements.txt
pip-license-checker --totals-only -r file1.txt -r file2.txt -r file3.txt
pip-license-checker -r resources/requirements.txt django aiohttp==3.7.1 --exclude 'aio.*'
pip-license-checker -x resources/external.csv --exclude-license '(?i).*(?:mit|bsd).*'
pip-license-checker -x resources/external.csv --external-options '{:skip-header false}'
pip-license-checker -x resources/external.cocoapods --external-format cocoapods'
```

### License types

The following valid license types are available (to be used with `--fail` option):

- `NetworkCopyleft`
- `StrongCopyleft`
- `WeakCopyleft`
- `Copyleft` (includes all of above)
- `Permissive`
- `Other`
- `Error`

### External file formats

The following valid external file formats are available (to be used with `--external-format` option):

- `csv`
- `cocoapods`
- `gradle`

### External file options

The following valid external file options are available (to be used
with `--external-options` option) for the external formats:

- `csv`: `'{:skip-header [boolean]}'` -- skip the first (header) line of the `csv` file or not.
- `cocoapods`: see the [documentation](https://github.com/pilosus/cocoapods-acknowledgements-licenses#options).
- `gradle`: see the [documentation](https://github.com/pilosus/gradle-licenses)

## FAQ

### Q1. Does the tool consider the Python package's version? What if a package changes its license over time?

The tool resolves the version for Python packages just `pip` package
manager does. It also checks the license only for the resolved version
of the package.

### Q2. How do I check all Python dependencies for my project, both explicit and transitive ones?

`pip-license-checker` checks only explicitly defined dependencies,
without
[transitive](https://en.wikipedia.org/wiki/Transitive_dependency)
ones. The easiest way to check all dependencies is to get them by
the `pip` as the list and then run the tool with that list:

```bash
pip freeze > requirements-all.txt
lein run -r requirements-all.txt
```

### Q3. Does the tool consider PEP-508 extras and markers specified for requirements?

[PEP508](https://www.python.org/dev/peps/pep-0508/) indeed allows
specifying extra packages to be installed for the package as well as
markers describing the rules when the dependency should be used:

```
requests[security];python_version<"3.9"
```

The tool *ignores* both extras and markers. Use the recipe for the
**Q2** if you need extras/markers to have an effect on the final list
of dependencies to be checked.


## Disclaimer

`pip-license-checker` is provided on an "as-is" basis and makes no
warranties regarding any information provided through it, and
disclaims liability for damages resulting from using it. Using
`pip-license-checker` does not constitute legal advice nor does it
create an attorney-client relationship.
