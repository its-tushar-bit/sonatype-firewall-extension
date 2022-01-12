<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Insight Brain Frontend - Convetions & Patterns

A live document containing our _current_ best practices, approaches, patterns and conventions for developing UI in `insight-brain`.

## Contents

- [ Pull-Request Best Practices ](#pull-request-best-practices)
  - [ Creating and Merging Pull-Requests ](#creating-and-merging-pull-requests)
  - [ Handling and Providing Feedback ](#handling-and-providing-feedback)
    - [ Reviewers - Providing Feedback ](#reviewers-providing-feedback)
    - [ Authors - Responding to Feedback ](#authors-responding-to-feedback)
- [ Code Conventions ](#code-conventions)
  - [ Helpful Utilities ](#helpful-utilities)
  - [ React Sample component ](#react-sample-component)
  - [ Using Redux-Toolkit ](#using-redux-toolkit)
  - [ Miscelaneous ](#miscelaneous)
    - [How to add an Unsaved Changes modal warning to your page](#how-to-add-an-unsaved-changes-modal-warning-to-your-page)
- [ Notes on Styling ](#notes-on-styling)
- [ Testing ](#testing)
  - [ Writing tests for React components ](#writing-tests-for-react-components)
  - [ Mocking Rejected Promises ](#mocking-rejected-promises)
  - [ Notes on Applitools ](#notes-on-applitools)

## Pull-Request Best Practices

### Creating and Merging Pull-Requests

- When creating a branch include the Jira ticket number in the name and a brief description of the goal. i.e. `CLM-666_enable-page-insights`.
- When creating a Pull-Request include:
  - Jira number in the ticket's name. i.e. CLM-666 Some Adjustments.
  - Link to Jira Ticket.
  - Screenshots if it's a visual change.
  - Any description or context that could aid the review process.
- Use merge instead of rebase.
  - Do not use commands that overwrite the git history (like rebase) when your branch is shared or in review.
  - Merge is also beneficial when it comes to automatic conflict resolution.
- Don't trust github green checkmarks - always check the Jenkins build directly.
- Analyze if the comments left by `sonatype-lift` have value or relevancy.
  - Some of lift's suggestions clash with our eslint suggestions — always go with eslint.
  - Mark the conversation as resolved when the comment has been addressed (or ignored).
- The Pull-Request needs a minimum of TWO approvals before being merged.
- Enable Visual Testing (Applitools) _after_ the PR has gotten two approvals but _before_ merging it.
- In order to be merged a PR needs the aforementioned TWO approvals and a passing CI build (Jenkins).
  - In the event that there's a pre-existing _Policy Violation_ Jenkins will report the build as failed. In this case you need to check that each individual step of the build has passed.
- After merging the Pull-Request verify that the [Master Snapshot Build](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/master-snapshot/) corresponding to your merge passes.

### Handling and Providing Feedback

The following section details the common etiquette for dealing with both providing and responding to feedback.

#### Reviewers - Providing Feedback

- Feedback should be provided on the relevant line(s) of code.
- Provide several comments with each detailing a single concern rather than a huge wall of text with several concerns.
- If possible use the code-suggestion feature.
- Distinguish between change-requests (code-quality changes) and minor comments (personal preferences)
  - Change-requests are comments that should be addressed.
  - Minor comments are suggestions that may not need to be addressed
- Mark the comments as resolved when they have been addressed.

#### Authors - Responding to Feedback

- Use the "Quote Reply" feature when replying to comments, and quote the relevant part. This will make review easier for people who rely on github emails.
- Use atomic fixes - one commit per feedback comment.
- Reply to the comment with the commit hash — github will automatically convert this hash into a link.

## Code Conventions

- All new UI components should be implemented in React and the state managed with Redux.
- Any changes to existing pages should follow the patterns of the modified code.
- All React components should be Capitalized — both the component definition _and_ the file where it lives. i.e. `DependencyTreePage` in `DependencyTreePage.jsx`.
- All the other files should use camelCase. i.e. `export function loadAddWaiverData` in `waiverActions.js`
- Prefer `default` exports for Components.
- Before building a visual component check if the [React Shared Components Library](https://gallery.sonatype.dev/) already has it.
- When building modals or popovers consider if it’s at all possible to make the component self-managed in terms of its display.
  - that is, the component itself should read the relevant state slice/prop and decide wether or not it should render or return null.
  - Consider creating a Slice dedicated only to the modal/popover.
  - See as example `insight-brain-frontend/src/main/frontend/componentDetails/overview/ComponentCoordinatesPopover/ComponentCoordinatesPopover.jsx`
  - The main benefit of this is that the consumer code doesn't need to pass around the prop that controls the rendering of the modal.
  - Another benefit is that the unit-testing of the rendering is done only in the modal spec file, and consumer code doesn't need to worry about re-testing this.
- When using an [`NxLoadWrapper`](https://gallery.sonatype.dev/#/pages/NxLoadWrapper) special attention should be given to the `loading` prop and the values it goes through while loading.
  - The usual use case is to show a loading indicator while some async request completes. This usually means that the `loading` flag goes from `false` to `true` and then `false` again — not loading, loading, fulfilled.
  - Note that `NxLoadWrapper` will render its content when `loading` is false.
  - Thus, if the flag goes through the values mentioned above, whatever content it renders will be mounted, then dismounted, and then mounted again.
  - This by itself isn't bad but can lead to double-requests if the content inside the load wrapper fires an async request.

### Helpful Utilities

There are several helpful utility files and functions that have been implemented to help with IQ front-end development. Here is a listing of some of them, all located in the `insight-brain-frontend/src/main/frontend/util` directory

- `jsUtil.js` - Helper functions for basic JavaScript manipulation and conversion. Capitalization, converting Sets to Arrays, and setting and looking up properties in nested objects
- `reduxUtil.js` - Several helper functions for creating Redux actions and reducers. Note, `createSlice` from Redux Toolkit should be the preferred approach for creating actions and reducers.
- `urlUtil.js` - functions to help with getting and setting URLs and their parameters
- `validationUtil.js` - validation functions for form elements
- `componentIdentifierUtils.js` - Helper function to serialize component identifiers

If you find yourself implementing a simple pattern that is or may be reusable, please consider exporting it to a helper file in this directory.

### React Sample component

One example or template that you can use when creating new React components is the [DependencyTreePage](https://github.com/sonatype/insight-brain/blob/master/insight-brain-frontend/src/main/frontend/DependencyTree/DependencyTreePage.jsx) component.

⚠️ Note, we no longer use `connect` HOC to create components connected to Redux store. Use redux hooks instead: `import { useSelector, useDispatch } from 'react-redux';`
Also, there is no need to provide a "container" wrapper for each connected component.

In `DependencyTree/module.js`, an Angular module is created pointing to the `DependencyTreePage` component using [react2angular](https://www.npmjs.com/package/react2angular). This is what converts the React component into something that the rest of IQ (AngularJS) can interact with. There are two helper functions `withStoreProvider` and `withRouterStateProvider` that provide the Redux store and RouterStateContext to the React components. The react component is converted to angular component with the code similar to
`react2angular(withStoreProvider(withRouterStateProvider(DependencyTreePage)), [], ['$ngRedux', '$state'])`

We implement runtime type-safety in React components using the [prop-types](https://www.npmjs.com/package/prop-types) library and all properties should be appropriately typed. This is usually done at the bottom of each component, by specifying various `PropTypes` from the `prop-types` project.

### Using Redux-Toolkit

- There are places of the application where we have actions and reducers in separate files — this is the old approach. The new preferred approach is to use [redux-toolkit](https://redux-toolkit.js.org/).
- Files created using [redux-toolkit](https://redux-toolkit.js.org/) should be named `*Slice`. i.e. `OverviewSlice.js` or `PolicyViolationsSlice.js`.
- Slice files must have individual exports for both the reducer part and the actions.
- Before creating a utility function check if it already exists in `insight-brain-frontend/src/main/frontend/util/reduxToolkitUtil.js`.

### Miscelaneous

#### How to add an Unsaved Changes modal warning to your page

It is an established pattern to have a warning show up when the user is navigating away from a page in which they have unsaved changes. We call this warning the "Unsaved Changes Modal". The following are the conditions required to enable this modal for any given react page.

- Track the _"isDirty"_ state of your page as a boolean flag in the related redux state and keep it up to date.
- Configure the path to your _"isDirty"_ redux state in the router config for the related page: inside the `data` object add an `isDirty` property whose value is a string array representing the path to your _"isDirty"_ state flag, starting from the reducer name.

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

## Notes on Styling

- Follow the [BEM naming convention](http://getbem.com/naming/)

## Testing

- Every exported item should be unit tested — Be it a Component, a reducer, or a utility function, they _all_ need unit testing.
  - We no longer require testing async actions and selectors since with React Testing Library we test connected component as a whole, so actions and selectors are considered to be implementation details.
- Unit tests should be included in the PR along with the source code they're testing — do not break them down into separate PRs as that makes it harder to review.
- Every story should include functional testing.
- If you're testing a React component —`.jsx` extension— make sure that your corresponding spec file is also using the `.jsx` extension.
- Spec files should be named equal to the source file they’re testing and with the "Spec" suffix. i.e. for `Source.jsx` the corresponding spec file should be `SourceSpec.jsx` — this makes the files easier to find.
- Avoid using `setTimeout` in test files if possible, this increases test suite's runtime. The alternative is to use `jasmine's` `clock` for simulating time.

### Writing tests for React components

⚠️ We no longer use Enzyme for testing. Please use **React Testing Library**.

React Testing Library promotes testing of Redux connected components as a whole. Instead of writing separate unit test for components, async actions and reducers, we test components integrated with Redux as a whole. Action creators are considered to be implementation details and don't need tests. We still encourage providing unit tests for Reducers since they are extremely easy to test (pure functions) and usually contain important application logic.

For en example of a test with React Testing Library see `insight-brain-frontend/src/test/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfigurationSpec.jsx`

There are several helper functions in `insight-brain-frontend/src/test/frontend/SpecUtil.js` to help you with writing unit tests for React components. Here are some of the most common:

- `render` wrapper for React Testing Library API that configures Redux Store and Jasmine matchers.
- `axiosMockerGenerator` can be used to mock Axios HTTP requests (get / post / put / delete)

### Mocking Rejected Promises

There's an [issue](https://github.com/jasmine/jasmine/issues/1590) with Jasmine and the handling of rejected promises that _sometimes_ shows up in our test executions.
The root of the issue is Jasmine's inability to handle rejected promises in a context that isn't the usual `then().catch()`; for that reason the following is suggested:

Instead of

```
spyOn(object, 'method').and.returnValue(Promise.reject('rejection'));
```

do:

```
spyOn(object, 'method').and.callFake(() => Promise.reject('rejection'));
```

And, instead of

```
mockAxiosCalls({
  put: {
    [url]: Promise.reject('some error'),
  },
});
```

do:

```
mockAxiosCalls({
  put: {
    [url]: () => Promise.reject('some error'),
  },
});
```

In a nutshell: don't create the rejected promise until you need it.

### Notes on Applitools

- Keep screenshots at a minimum.
  - Usually one screenshot per page of the application is enough, unless the page in question has very complex and visually-distinct states.
- Only enable Visual Testing (Applitools) _after_ the Pull-Request has gotten two approvals but _before_ merging it.
- Make sure that Applitools passes _before_ merging your Pull-Request.
- If there are any expected applitools changes, accept them, and remember to click save so the baseline is updated.
- If the changes you are seeing are NOT expected then it is a sign that something unintended is happening in the PR and needs to be addressed.
- If `master` has an Applitools failure notify the relevant person, or send a message to the Slack #IQ channel.
