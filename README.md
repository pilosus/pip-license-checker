# pip-license-checker

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
lein run piny 0.6.0
lein run aiostream

### scan all packages in requirements file
lein run -r resources/requirements.txt

aiohttp:3.7.2                  Apache Software License        Permissive
piny:0.6                       MIT License                    Permissive
aiostream                      GPLv3                          Copyleft
aiocache:0.11.1                                               Other
aiokafka:0.6                   Apache Software License        Permissive
aiopg:2.3.5                    Error                          ???
workflow-tools:0.5.0           Apache Software License        Permissive
Synx                           Other/Proprietary License      Other

### scan packages matching regex pattern
### e.g. all lines except containing "aio.*" packages
lein run -r resources/requirements.txt -m '(?!aio).*'

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
