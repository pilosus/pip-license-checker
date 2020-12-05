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
# see usage message
lein run

# check a single package
lein run piny 0.6.0
lein aiostream

# scan all packages in requirements file
lein -r /path/to/requirements.txt
```

Option 2. Compile and run an ``uberjar`` standalone:


```bash
lein uberjar
cd target/uberjar
java -jar pip-license-checker-[version]-standalone.jar [args]
```
