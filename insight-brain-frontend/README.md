<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Insight Brain Frontend

`insight-brain-frontend` is the module containing the front-end of [Nexus IQ Server](https://github.com/sonatype/insight-brain).

## Contents

- [ Front-end Development ](#front-end-development)
  - [ Requirements ](#requirements)
  - [ Workflow ](#workflow)
    - [ Setup ](#setup)
    - [ Building and monitoring the front-end ](#building-and-monitoring-the-front-end)
    - [ Building and monitoring front-end assets outside the main bundle ](#building-and-monitoring-front-end-assets-outside-the-main-bundle)
    - [ Running tests ](#running-tests)
    - [ Linting and Formatting ](#linting-and-formatting)
    - [ Re-installing packages with yarn ](#re-installing-packages-with-yarn)
- [ Supported browsers and resolution ](#supported-browsers-and-resolution)
- [ Code Conventions & Patterns ](#code-conventions--patterns)

## Front-end development

### Requirements

The following tools should be installed locally to enable front-end development:

- **[Node.js](https://nodejs.org/)** is required to run webpack and yarn.

  - The Maven build downloads its own copy of Node. For best results, match the version used by Maven. Look for the `node.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
  - Homebrew users can install via: `brew install node`
  - If you do a lot of front-end development and need to switch Node versions frequently, you might also consider using [Node Version Manager (nvm)](https://github.com/nvm-sh/nvm) as an alternative route to installation.

- **[yarn](https://classic.yarnpkg.com/en/)** is required if you want to add or remove dependencies from the project.
  - The Maven build downloads its own copy of yarn. For best results, match the version used by Maven. Look for the `yarn.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
  - A different node package manager, npm, comes installed with Node. You can use npm to install yarn: `npm install -g yarn@<version>`

### Workflow

#### Setup

NOTE: For M1/ARM64 architectures, if you encounter error "Node Sass does not yet support your current environment: OS X Unsupported architecture (arm64) with Node.js 12.x", you can run the following from insight-brain-frontend directory as a workaround and then continue the steps below: `npm install node-sass@npm:sass`

First, [build](../readme.md#building) the `insight-brain` project, and then [deploy](../insight-brain-service/README.md#deploying-iq-server-locally) it to port 8072 using the following command (executed from the `insight-brain-service` directory):

`mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.service.InsightBrainService -Dexec.args='server src/test/resources/config-dev.yml' -Ddw.server.applicationConnectors[0].port=8072`

You will probably notice that this is the same command that's used to normally [deploy](../insight-brain-service/README.md#deploying-iq-server-locally) the server locally but with a flag that tells it to run on port 8072.

#### Building and monitoring the front-end

Webpack will monitor your front-end assets and automatically compile them for you when you make changes. This allows for short feedback loops.

With your back-end server running on port 8072, you can launch the front-end on port 8070 by running the yarn start task, like so:

`yarn start`

Note that when you navigate to `http://localhost:8070`, you will see a mostly-blank page with a link labelled "assets". This is normal; simply click the link to access the IQ Server login page.

#### Building and monitoring front-end assets outside the main bundle

Under the default yarn start task, webpack will build and monitor the **main** front-end bundle. There are some other bundles that get deployed with IQ, such as e.g. [`cip-loader.js`](./src/main/frontend/cip/cip-loader-index.js), which powers the legacy application report. (For a full list of bundles that are deployed, see: [`webpack.config.js`](./webpack.config.js))

If you are developing these bundles, you'll want to use the following command:

`yarn run start-all`

#### IQ Product License

The Nexus IQ Server requires a license to run.
You can find licenses and their descriptions on Confluence at [Product Licensing](https://docs.sonatype.com/display/ProdMgmt/Product+Licensing).
Most developers will want a license with a name similar to `2021-sonatype-internal-rm-lc-fw-fwfa-adp-alp-iacp-1000apps-1000rm_users-1000lc_users-1000fw_users.lic`.
That will enable all the functionality you're likely to need.

#### Running tests

There are two groups of unit tests: those written in the older [Jasmine](https://jasmine.github.io/) BDD framework, and
those written in [Jest](https://jestjs.io/), a newer successor to Jasmine. Generally, new tests should be written in
Jest, and hopefully the Jasmine tests will gradually be ported over and Jasmine eventually removed. Jasmine runs tests
within a headless browser instance while Jest runs them within node.js with [jsdom](https://github.com/jsdom/jsdom).

NOTE for MAC OS users: Before running Jasmine tests locally, set Safari to always show scroll bars as our tests depend on scroll bars to be visible (refer to https://support.apple.com/en-nz/guide/mac-help/mchlp1225/mac for instructions.)

To run all JavaScript unit tests in the CLI and see the results there, simply run the `test` task:

`yarn run test`

Jest and Jasmine also each have a 'watch' mode which repeated re-execution. Jest's watch mode can be activated with the
`jest-watch` npm script:

`yarn run jest-watch`

For Jasmine's watch mode, use the `test-watch` task:

`yarn run test-watch`

In these interactive environments, as you make changes to your tests, the runner will automatically re-run your tests.
The test report can be seen in the your terminal.

If you want to limit which tests run, you can change any `it` test function to `fit` to run only that test, or any
`describe` to `fdescribe` to only run the contained tests. Additionally, the jest runner has various keyboard driven
commands that may be issued to the test runner in order to manage filters. Be aware that Jest's test filtering appears
to be somewhat unreliable – always double check that it actually ran what you wanted it to.

Both suites of tests live within the `src/test/frontend` directory. Jest tests must be placed within files whose names
end in ".jestspec.js" or ".jestspec.jsx". Jasmine tests must be placed within files whose names end in "Spec.js",
"spec.js", "Spec.jsx", or "spec.jsx", without that "spec" token being part of the word "jestspec".

Examples:

- vulnerabilitySearchReducerSpec.js - Jasmine test
- roleEditorPermissionListSpec.jsx - Jasmine test
- fuzzy.filter.spec.js - Jasmine test
- AddWaiverPage.jestspec.jsx - Jest test
- waiverActions.jestspec.js - Jest test

A special note for testing `NxToolip`: There is a delay when rendering a tooltip from `NxToolip`. In order to test it properly call `requestIdleCallbackInvokeImmediate` found in `SpecUtil.js` before you call the `render` function from RTL. See `react-shared-components/components/NxTooltip/updateBatcher.js` for details on requestIdleCallback usage.

#### Linting and Formatting

We use **[ESLint](https://eslint.org/)** and **[Prettier](https://prettier.io/)** for linting and formatting.

Both have plugins for all the major IDEs and Editors which very much enhance developer experience:

- https://eslint.org/docs/user-guide/integrations
- https://prettier.io/ under **Editor Support**

_Suggestion: installing the plugin for your IDE of choice and configuring prettier to format the files on every save is actually very nice when you get used to it._

The files are auto-formatted in a pre-commit hook, so after commiting files will change to match the prettier formatting style after every commit.

If you need to run the formatter on your files manually you can run `yarn format -- ./path/to/files` if nessecary.

#### Re-installing packages with yarn

If you've already run the Maven build, you **don't** need to explicitly install yarn dependencies - they've already been installed!

However, if you've been adding or removing packages, or if you just need a clean start for some reason, then you can always clear out `node_modules` and start fresh. Example syntax for a Unix based OS: `rm -rf node_modules && yarn install`. Alternatively, if you don't want to delete `node_modules` and have to re-install everything, `yarn install --check-files` _should_ bring everything up to date as-needed.

## Supported browsers and resolution

As of January 2021, we currently support the latest desktop versions of Chrome, Firefox, Safari and Edge. See the [support help docs reference](https://help.sonatype.com/iqserver/product-information/system-requirements#SystemRequirements-BrowserRequirements) for the most up to date versions that are supported. When developing on the front end, make sure to test in all of these browsers and multiple operating systems (Linux, Windows, MacOS). [BrowserStack](https://www.browserstack.com) is a helpful utility for this, and there are BrowserStack licenses available for all IQ devs.

Our current minimum screen resolution is 1366 pixels.

## React & Redux

This project uses React for the frontend and Redux for state management.

If you want to become more familiar with React and/or Redux, consider any of the following tutorials:

- [Tutorial: Intro to React on reactjs.org](https://reactjs.org/tutorial/tutorial.html)
- [Redux basic tutorial](https://redux.js.org/basics/basic-tutorial)
- [React and Redux Tutorials on freecodecamp.org](https://www.freecodecamp.org/)
- [Redux toolkit tutorial](https://redux-toolkit.js.org/tutorials/quick-start)

## Code Conventions & Patterns

A more in-depth document describing the _current_ code conventions & patterns used by the IQ UI can be found at [CODE_CONVENTIONS.md](https://github.com/sonatype/insight-brain/blob/main/insight-brain-frontend/CODE_CONVENTIONS.md).
We encourage any folks looking to do UI development in IQ to read the aforementioned document.

For any questions about front-end development, reach out to the `@iq-laurel-team`, `@iq-voyage-team` in `#iq-laurel` in Slack
