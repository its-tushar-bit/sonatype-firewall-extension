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
        * [ Running tests ](#running-tests)
        * [ Re-installing packages with yarn ](#re-installing-packages-with-yarn)
* [ Supported browsers and resolution ](#supported-browsers-and-resolution)
* [ Helpful Utilities ](#helpful-utilities)
* [ Notes on Angular, Redux, & React ](#notes-on-angular-redux--react)
    * [Redux and React conventions in the IQ frontend project](#redux-and-react-conventions-in-the-iq-frontend-project)
* [ React Sample component ](#react-sample-component)
    * [Writing tests for React components](#writing-tests-for-react-components)
    * [How to add an Unsaved Changes modal warning to your page](#how-to-add-an-unsaved-changes-modal-warning-to-your-page)


## Front-end development

### Requirements

The following tools should be installed locally to enable front-end development:

* **[Node.js](https://nodejs.org/)** is required to run webpack and yarn.
    * The Maven build downloads its own copy of Node.  For best results, match the version used by Maven. Look for the `node.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
    * Homebrew users can install via: `brew install node`
    * If you do a lot of front-end development and need to switch Node versions frequently, you might also consider using [Node Version Manager (nvm)](https://github.com/nvm-sh/nvm) as an alternative route to installation.

* **[yarn](https://classic.yarnpkg.com/en/)** is required if you want to add or remove dependencies from the project.
    * The Maven build downloads its own copy of yarn.  For best results, match the version used by Maven. Look for the `yarn.version` property in [insight-brain-frontend/pom.xml](./pom.xml).
    * A different node package manager, npm, comes installed with Node. You can use npm to install yarn: `npm install -g yarn@<version>`

### Workflow

#### Setup

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

#### Running tests

Unit tests are written using the [Jasmine](https://jasmine.github.io/) BDD framework.

NOTE for MAC OS users: Before running tests locally, set Safari to always show scroll bars as our tests depend on scroll bars to be visible (refer to https://support.apple.com/en-nz/guide/mac-help/mchlp1225/mac for instructions.)

To run all JavaScript unit tests in the CLI and see the results there, simply run the `test` task:

`yarn run test`

You can also run tests in 'watch' mode and view the reports in your browser (we use the [Jasmine Webpack Plugin](https://www.npmjs.com/package/jasmine-webpack-plugin) for this). To do so, use the `test-watch` task (note that you do NOT need a back-end server running):

`yarn run test-watch`

You can then launch your browser, point it at `http://localhost:8235/`, and enjoy an interactive test runner environment. This means that as you make changes to your tests, the runner will automagically re-run your tests, and update the test report in the browser in real time.

In the browser, you can additionally filter the tests that you see by adding a matcher to the `spec` query param of the URL. For example, to execute all specs that begin with the word "dashboard", you would access the following URL:

`http://localhost:8235/?spec=dashboard`

#### Re-installing packages with yarn

If you've already run the Maven build, you **don't** need to explicitly install yarn dependencies - they've already been installed!

However, if you've been adding or removing packages, or if you just need a clean start for some reason, then you can always clear out `node_modules` and start fresh. Example syntax for a Unix based OS: `rm -rf node_modules && yarn install`.  Alternatively, if you don't want to delete `node_modules` and have to re-install everything, `yarn install --check-files` _should_ bring everything up to date as-needed.

## Supported browsers and resolution

As of January 2021, we currently support the latest desktop versions of Chrome, Firefox, Safari and Edge. See the [support help docs reference](https://help.sonatype.com/iqserver/product-information/system-requirements#SystemRequirements-BrowserRequirements) for the most up to date versions that are supported. When developing on the front end, make sure to test in all of these browsers and multiple operating systems (Linux, Windows, MacOS). [BrowserStack](https://www.browserstack.com) is a helpful utility for this, and there are BrowserStack licenses available for all IQ devs.

Our current minimum screen resolution is 1366 pixels.

## Helpful Utilities

There are several helpful utility files and functions that have been implemented to help with IQ front-end development. Here is a listing of some of them, all located in the `insight-brain-frontend/src/main/frontend/util` directory

* `jsUtil.js` - Helper functions for basic JavaScript manipulation and conversion. Capitalization, converting Sets to Arrays, and setting and looking up properties in nested objects
* `reduxUtil.js` - Several helper functions for creating Redux actions and reducers
* `urlUtil.js` - functions to help with getting and setting URLs and their parameters
* `validationUtil.js` - validation functions for form elements
* `componentIdentifierUtils.js` - Helper function to serialize component identifiers

If you find yourself implementing a simple pattern that is or may be reusable, please consider exporting it to a helper file in this directory.

## Notes on Angular, Redux, & React

Historically, the front-end of IQ was built using AngularJS. In 2017, we introduced Redux for state management by way of [`ng-redux`](https://github.com/angular-redux/ng-redux).

In 2018/2019, several developers from across Sonatype researched several different front-end frameworks to decide the company direction going forward. The consensus of that summit was to use React. Since then we have started migrating UI components in IQ from AngularJS to React.

If you want to become more familiar with React and/or Redux, consider any of the following tutorials:
* [Tutorial: Intro to React on reactjs.org](https://reactjs.org/tutorial/tutorial.html)
* [Redux basic tutorial](https://redux.js.org/basics/basic-tutorial)
* [React and Redux Tutorials on freecodecamp.org](https://www.freecodecamp.org/)

### Redux and React conventions in the IQ frontend project

* When naming Redux action types, you should prefix them with an appropriate context. For example, `ADVANCED_SEARCH_CONFIG_LOAD_REQUESTED` in `advancedSearchConfigActions.js`
* All new UI components should be implemented in React and backed with Redux. Any changes to existing pages should follow the patterns of the modified code.
* All React components should be Capitalized (`const AddWaiverPageContainer` in `AddWaiverPageContainer.jsx`) while actions and reducers should use camelCase (`export function loadAddWaiverData` in `waiverActions.js`)

## React Sample component

One example or template that you can use when creating new React components is the Add Waiver Page. There are other React examples in the IQ codebase that you can also inspect.
* First, a container component such as `AddWaiverPageContainer` is created. This container component is responsible for wiring any external state (most typically state stored in redux) and callbacks into the UI. Two important functions that should be created are [mapStateToProps](https://react-redux.js.org/using-react-redux/connect-mapstate) and [mapDispatchToProps](https://react-redux.js.org/using-react-redux/connect-mapdispatch). Here we use the [react-redux](https://react-redux.js.org/) library to follow a standard pattern for passing state to and from React components. These methods, along with the [connect](https://react-redux.js.org/api/connect) method from the same library, allow us to pass state between the Redux store and our React components in a conventional way. The container component then passes the needed properties to the presentational component, with code similar to `const AddWaiverPageContainer = connect(mapStateToProps, mapDispatchToProps)(AddWaiverPage);`
* The presentational component (`AddWaiverPage`) may have internal logic that helps it decide what to render. But it should not directly interact with the global state or actions. Instead, it should receive all data it needs, and all callbacks for user interaction that it supports, as React props. It is up to the container component to set these props to the correct data from the redux store and the correct action creators.
* Relevant actions (`waiverActions.js`) and reducers (`addWaiverReducer.js`) for the component are also created in separate files. Any new reducers should be added to `insight-brain-frontend/src/main/frontend/reduxConfig/reducers.js`
* Finally, in `waivers/module.js`, an Angular module is created pointing to the `AddWaiverPageContainer` component using [react2angular](https://www.npmjs.com/package/react2angular). This is what converts the React component into something that the rest of IQ (AngularJS) can interact with. There is a helper function called `withStoreProvider` that provides the redux store to the React components. It is wired into angular with code similar to
`.component('addWaiverPage', react2angular(withStoreProvider(AddWaiverPageContainer), [], ['$ngRedux', '$state']))`

We implement runtime type-safety in React components using the [prop-types](https://www.npmjs.com/package/prop-types) library and all properties should be appropriately typed. This is usually done at the bottom of each component, by specifying various `PropTypes` from the `prop-types` project.

### Writing tests for React components
It is IMPERATIVE that JavaScript unit tests be written for all front end code. Java integration and functional tests alone do not sufficiently ensure proper functionality.

JavaScript unit tests should be created for the container component, the presentational component, and all actions and reducers. Examples of how to wire up mock state and properties can be found in the tests for the Add Waiver Page.

There are several helper functions in `insight-brain-frontend/src/test/frontend/SpecUtil.js` to help you with writing unit tests for React components. Here are some of the most common:

* `mockNgRedux` and `mockReduxStore` provide helpful interfaces for interacting with a mock Redux store / state
* `axiosMockerGenerator` can help to interact with the various Axios HTTP verbs (get / post / put / delete)
* You should typically use [enzyme's](https://enzymejs.github.io/enzyme/docs/api/shallow.html) `shallow` and `mount` functions to test the DOM that your React component renders

### How to add an Unsaved Changes modal warning to your page
It is an established pattern to have a warning show up when the user is navigating away from a page in which they have unsaved changes. We call this warning the "Unsaved Changes Modal". The following are the conditions required to enable this modal for any given react page.

* Track the _"isDirty"_ state of your page as a boolean flag in the related redux state and keep it up to date.
* Configure the path to your _"isDirty"_ redux state in the router config for the related page: inside the `data` object add an `isDirty` property whose value is a string array representing the path to your _"isDirty"_ state flag, starting from the reducer name.

For example, if you store your the "isDirty" flag in the `addWaiver` reducer in a variable called `isAddWaiverPageDirty`, the router config should look like this:
```
.state('addWaiver', {
    ...
    data: {
        isDirty: ['addWaiver', 'isAddWaiverPageDirty']
    }
    ...
})
```

For any questions about front-end development, reach out to the `@iq-laurel-team` in `#iq-laurel` in Slack
