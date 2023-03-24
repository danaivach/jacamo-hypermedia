# What to Contribute?

Great, feel free to jump right into it:
1. Pick an existing issue — or create a new one that describes your contribution.
2. Create a new branch for your contribution. We use the following convention for naming branches:
    * `feat-YOUR-FEATURE`: when creating a branch to develop a new feature
    * `fix-YOUR-FIX`: when creating a branch to fix an issue
3. Open a pull request when ready.

## Before you start cranking

We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) and the [JaCaMo Style Guide](https://jacamo-lang.github.io/documentation/programming-style/index.html).

We use [Test-Driven Development](https://martinfowler.com/bliki/TestDrivenDevelopment.html). If you write new code, please make sure you also write tests with a decent code coverage.

We use [Conventional Changelog](https://github.com/conventional-changelog/conventional-changelog) to
generate changelogs from [Conventional Commit messages](https://www.conventionalcommits.org/).
For this to work, commit messages must be structured as follows:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

* `<type>`: A noun specifying the type of change, followed by a colon and a space. The types allowed are:
   * `feat`: A new feature
   * `fix`: A bug fix
   * `refactor`: Code change that neither fixes a bug or adds a feature (not relevant for end user)
   * `perf`: Change improves performance
   * `style`: Change does not affect the code (e.g., formatting, whitespaces)
   * `test`: Adding missing tests
   * `chore`: Change of build process or auxiliary tools
   * `docs`: Documentation only changes
* `<scope>`: Optional. A term of free choice specifying the place of the commit change, enclosed in parentheses. Examples:
   * `feat(binding-coap): ...`
   * `fix(cli): ...`
   * `docs: ...` (no scope, as it is optional)
* `<subject>`: A succinct description of the change, e.g., `add support for magic`
   * Use the imperative, present tense: "add", not "added" nor "adds"
   * Do not capitalize first letter: "add", not "Add"
   * No dot (.) at the end
* `<body>`: Optional. Can include the motivation for the change and contrast this with previous behavior.
   * Just as in the subject, use the imperative, present tense: "change" not "changed" nor "changes"
* `<footer>`: Optional. Can be used to automatically close GitHub Issues and to document breaking changes.
   * The prefix `BREAKING CHANGE: ` idicates API breakage (corresponding to a major version change) and everything after is a description what changed and what needs to be done to migrate
   * GitHub Issue controls such as `Fixes #123` or `Closes #4711` must come before a potential `BREAKING CHANGE: `.

Examples:
```
docs: improve how to contribute
```
```
feat(core): add support for general magic

Closes #110
```
```
feat(core): add support for general magic

Simplify the API by reducing the number of functions.

Closes #110
BREAKING CHANGE: Change all calls to the API to the new `do()` function.
```

**Acknowledgements:** This CONTRIBUTING.md document is based on a similar version contributed by
[Iori Mizutani](https://github.com/iomz) for [Interactions-HSG](http://github.com/interactions-hsg/).
