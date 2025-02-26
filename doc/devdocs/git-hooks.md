<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Git Hooks

## What are Git Hooks

Git hooks are scripts that run automatically at certain points in the Git workflow. These scripts can be used to enforce certain rules or perform actions when certain Git events occur, e.g. before pushing to a remote repository.

## How insight-brain uses Git Hooks

`insight-brain` makes use of the `pre-push` and `pre-commit` Git Hooks.

- The `pre-push` Git Hook runs a license check (ensure all source code files have a license header) before pushing to a remote repository. If the license check fails, the push will be aborted.
- The `pre-commit` Git Hook runs a front-end formatter (Prettier) on all front-end files before committing. If the formatter fails, the commit will be aborted.

## How to disable Git Hooks

To disable the `pre-push` Git Hook for the `insight-brain` repository, you can run the following command

```bash
git push --no-verify
```

To disable the `pre-push` Git Hook globally, you can run the following command to create an alias for `git push --no-verify`

```bash
git config --global alias.pushnv "push --no-verify"
```

Once the above alias is created, you can use `git pushnv`
