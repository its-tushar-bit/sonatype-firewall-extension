<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Insight Brain Frontend

`insight-brain-frontend` is the module containing the front-end of [Nexus IQ Server](https://github.com/sonatype/insight-brain).

## Contents

* [ Front-end Development ](#front-end-development)
    * [ Requirements ](#requirements)
    * [ Workflow ](#workflow)
        * [ Setup ](#setup)
        * [ Building and monitoring the front-end ](#building-and-monitoring-the-front-end)
        * [ Building and monitoring front-end assets outside the main bundle ](#building-and-monitoring-front-end-assets-outside-the-main-bundle)
        * [ Running tests for the main bundle ](#running-tests-for-the-main-bundle)
        * [ Running tests for assets outside the main bundle ](#running-tests-for-assets-outside-the-main-bundle)
        * [ Re-installing packages with npm ](#re-installing-packages-with-npm)
* [ Note on Angular, Redux, & React ](#notes-on-angular-redux--react)

## Front-end development

### Requirements

The following tools should be installed locally to enable front-end development:

* **[Node.js](https://nodejs.org/)** is required to run webpack and npm.
    * The Maven build downloads its own copy of Node.  For best results, match the version used by Maven. Look for the `node.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
    * Homebrew users can install via: `brew install node`
    * If you do a lot of front-end development and need to switch Node versions frequently, you might also consider using [Node Version Manager (nvm)](https://github.com/nvm-sh/nvm) as an alternative route to installation.

* **[npm (Node Package Manager)](https://www.npmjs.com/)** is required if you want to add or remove dependencies from the project.
    * The Maven build downloads its own copy of npm.  For best results, match the version used by Maven. Look for the `npm.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
    * npm comes installed with Node, but it might not be the exact version we want.
    * To install an exact version: `npm install -g npm@<version>`
    * As with Node, you can gain more control over your npm installation(s) by using [nvm](https://github.com/nvm-sh/nvm).

### Workflow

#### Setup

First, [build](../readme.md#building) the `insight-brain` project and [deploy](../insight-brain-service/README.md#deploying-iq-server-locally) the server. You should deploy to port 8072. (For front-end development, we use webpack-dev-server. We have it configured to run on port 8070, and it will proxy to the server at 8072.)

#### Building and monitoring the front-end

Webpack will monitor your front-end assets and automatically compile them for you when you make changes. This allows for short feedback loops.

With your back-end server running on port 8072, you can launch the front-end on port 8070 by running npm start task, like so:

`npm start`

Note that when you navigate to `http://localhost:8070`, you will see a mostly-blank page with a link labelled "assets". This is normal; simply click the link to access the IQ Server login page.

#### Building and monitoring front-end assets outside the main bundle

Under the default npm start task, webpack will build and monitor the **main** front-end bundle. There are some other bundles that get deployed with IQ, such as e.g. [`cip-loader.js`](./src/main/frontend/cip/cip-loader-index.js), which powers the legacy application report. (For a full list of bundles that are deployed, see: [`webpack.config.js`](./webpack.config.js))

If you are developing these bundles, you'll want to use the following command:

`npm run start-all`

#### Running tests

Unit tests are written using the [Jasmine](https://jasmine.github.io/) BDD framework.

To run all JavaScript unit tests in the CLI and see the results there, simply run the `test` task:

`npm run test`

You can also run tests in 'watch' mode and view the reports in your browser (we use the [Jasmine Webpack Plugin](https://www.npmjs.com/package/jasmine-webpack-plugin) for this). To do so, use the `test-watch` task (note that you do NOT need a back-end server running):

`npm run test-watch`

You can then launch your browser, point it at `http://localhost:8235/`, and enjoy an interactive test runner environment. This means that as you make changes to your tests, the runner will automagically re-run your tests, and update the test report in the browser in real time.

In the browser, you can additionally filter the tests that you see by adding a matcher to the `spec` query param of the URL. For example, to execute all specs that begin with the word "dashboard", you would access the following URL:

`http://localhost:8235/?spec=dashboard`

#### Re-installing packages with npm

If you've already run the Maven build, you **don't** need to explicitly install npm dependencies - they've already been installed!

However, if you've been adding or removing packages, or if you just need a clean start for some reason, then you can always clear out `node_modules` and start fresh. Example syntax for a Unix based OS: `rm -rf node_modules && npm i`

## Notes on Angular, Redux, & React

* Historically, this front-end was built using AngularJS.
* In 2017, we introduced Redux for state management by way of [`ng-redux`](https://github.com/angular-redux/ng-redux).
* As of 2019, we have started migrating UI components from AngularJS to React. Brand new UI components should be implemented in React and backed with Redux.
* As of Feb 2020, we are working on a robust guide to make this transition, as well as front-end development in general, easier for all contributors to the IQ project. Stay tuned!
