# pip-license-checker

[![codecov](https://codecov.io/gh/pilosus/pip-license-checker/branch/main/graph/badge.svg?token=MXN6PDETET)](https://codecov.io/gh/pilosus/pip-license-checker)

Check Python PyPI package license


## Installation

1. Install [Leiningen](https://leiningen.org/)

2. Get the source code

```bash
git clone https://github.com/pilosus/pip-license-checker.git
cd pip-license-checker
```

## Usage

Option 1. Run the code with ``lein``:

```bash
### see usage message
lein run

### check a single package
lein run piny==0.6.0
lein run aiostream

### include pre-release and development versions
lein run --pre aiohttp

### scan all packages in requirements file
lein run -r resources/requirements.txt

aiohttp:3.7.2                  Apache Software License        Permissive
piny:0.6                       MIT License                    Permissive
aiostream                      GPLv3                          Copyleft
aiocache:0.11.1                                               Other
aiokafka:0.6                   Apache Software License        Permissive
aiopg:122.3.5                  Error                          ???
workflow-tools:0.5.0           Apache Software License        Permissive
Synx                           Other/Proprietary License      Other

### scan packages matching regex pattern
### e.g. all lines except containing "aio.*" packages
lein run -r resources/requirements.txt -e 'aio.*'

piny:0.6                       MIT License                    Permissive
workflow-tools:0.5.0           Apache Software License        Permissive
Synx                           Other/Proprietary License      Other
```

Option 2. Compile and run an ``uberjar`` standalone:


```bash
lein uberjar
cd target/uberjar
java -jar pip-license-checker-[version]-standalone.jar [args]
```

## Help

Run application with `lein run` or use `--help` option with standalone
jar for more details.

```
$ lein run

pip-license-checker - check Python PyPI package license

Usage:
pip-license-checker [options]... [package]...

Description:
  package	List of package names in format `name[specifier][version]`

  -r, --requirements REQUIREMENT_NAME  []   Requirement file name to read
  -f, --fail LICENSE_TYPE              #{}  Return non-zero exit code if license type is found
  -e, --exclude REGEX                       PCRE to exclude matching packages. Used only if [package]... or requirement files specified
  -p, --[no-]pre                            Include pre-release and development versions. By default, use only stable versions
  -t, --[no-]with-totals                    Print totals for license types
  -o, --[no-]totals-only                    Print only totals for license types
  -d, --[no-]table-headers                  Print table headers
  -m, --[no-]fails-only                     Print only packages of license types specified with --fail flags
  -h, --help                                Print this help message

Examples:
pip-license-checker django
pip-license-checker aiohttp==3.7.2 piny==0.6.0 django
pip-license-checker --pre 'aiohttp<4'
pip-license-checker --with-totals --table-headers --requirements resources/requirements.txt
pip-license-checker --totals-only -r file1.txt -r file2.txt -r file3.txt
pip-license-checker -r resources/requirements.txt django aiohttp==3.7.1 --exclude 'aio.*'
```

## Docker

App can also be run in docker container.

1. Build docker image

```bash
cd pip-license-checker
docker build -t pip-license-checker .
```

2. Run app in docker container, mount current host directory with
``requirements.txt`` file to container predefined volume directory
``/volume``

```bash
docker run -v `pwd`:/volume \
              -it --rm --name pip-check pip-license-checker \
              java -jar app.jar 'aiohttp>=3.6.1,<3.8' \
              -r /volume/requirements.txt
```

## Disclaimer

``pip-license-checker`` is provided on an "as-is" basis and makes no
warranties regarding any information provided through it, and
disclaims liability for damages resulting from using it. Using
``pip-license-checker`` does not constitute legal advice nor does it
create an attorney-client relationship.
