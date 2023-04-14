## Contributing

### General rules

1. Before writing any *code* take a look at the existing
   [issues](https://github.com/pilosus/pip-license-checker/issues?q=).
   If none of them is about the changes you want to contribute, open
   up a new issue. Fixing a typo requires no issue though, just submit
   a Pull Request.

2. If you're looking for an open issue to fix, check out
   labels `help wanted` and `good first issue` on GitHub.

3. If you plan to work on an issue open not by you, write about your
   intention in the comments *before* you start working.


### Development rules

1. Follow the GitHub [fork & pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork) flow.

2. Install [Leiningen](https://leiningen.org/)

3. Make changes to the code.

4. Make sure code formatters, linters, tests and code coverage pass locally:

```
# Code formatting
$ lein cljfmt fix

# Linters
$ lein clj-kondo

# Tests with a code coverage report
$ lein cloverage
```
Code coverage *is not expected to be lower than in the main branch*
(unless you have very solid grounds to let it drop a bit - to be
dicussed on the code review).

5. Open a pull request, refer to the issue you solve.

6. Make sure GitHub Checks (Actions) pass. They should if you followed
   p.4.

### Release management

#### Checlist

1. Follow the [SemVer](https://semver.org/) conventions for the release number.
2. Update the [project version](https://github.com/pilosus/pip-license-checker/blob/main/project.clj).
3. Update the [CHANGELOG](https://github.com/pilosus/action-pip-license-checker/blob/main/CHANGELOG.md).
4. Update the [README](https://github.com/pilosus/action-pip-license-checker/blob/main/README.md) if needed.
5. Merge the changes to the `main` branch.
6. Push a version-specific tag, e.g. `2.1.9`:

```
$ git tag 2.1.9
$ git push origin 2.1.9
```

7. Draft a new release on the
   [GitHub](https://github.com/pilosus/pip-license-checker/releases/new). Make
   sure the version-specific tag and the changelog are used for the
   release notes. Start a discussion thread for the release. Publish
   links to the testing if needed (may be a good idea for release
   candidates).

#### Artifacts

1. Clojars package is to be uploaded automatically as a part of the GitHub Actions CI/CD
2. Docker image is built and uploaded to the [Docker Hub](https://hub.docker.com/r/pilosus/pip-license-checker/)
   using its `Automated Builds` triggers for SemVer tags and the `main`
   branch pushes.
