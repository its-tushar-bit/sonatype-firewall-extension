<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Insight Brain Frontend - Conventions & Patterns

A live document containing our _current_ best practices, approaches, patterns and conventions for developing UI in `insight-brain`.

## Contents

- [ Code Conventions ](#code-conventions)
  - [ Helpful Utilities ](#helpful-utilities)
  - [ Directory Structure ](#directory-structure)
  - [ React Sample component ](#react-sample-component)
  - [ Using Redux-Toolkit ](#using-redux-toolkit)
  - [ Miscellaneous ](#miscellaneous)
    - [How to add an Unsaved Changes modal warning to your page](#how-to-add-an-unsaved-changes-modal-warning-to-your-page)
- [ Notes on Styling ](#notes-on-styling)
- [ Testing ](#testing)
  - [ Writing tests for React components ](#writing-tests-for-react-components)
  - [ Mocking Rejected Promises ](#mocking-rejected-promises)

## Code Conventions

- All new UI components should be implemented in React and the state managed with Redux.
- Any changes to existing pages should follow the patterns of the modified code.
- All React components should be Capitalized — both the component definition _and_ the file where it lives. i.e. `DependencyTreePage` in `DependencyTreePage.jsx`.
- All the other files should use camelCase. i.e. `export function loadAddWaiverData` in `waiverActions.js`
- Prefer `default` exports for Components.
- Before building a visual component check if the [React Shared Components Library](https://gallery.sonatype.dev/) already has it.
- When building modals or popovers consider if it’s at all possible to make the component self-managed in terms of its display.
  - that is, the component itself should read the relevant state slice/prop and decide whether it should render or return null.
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

### Directory Structure

The directory structure should resemble the component tree. The top level components (usually representing the page), should have their own directory under `src/main/frontend`. If components consist of other components, they should be placed in the directory representing their parent component and so on. Directories should be named using `camelCase`.

In the example below, `HomePage.jsx` consists of `Header.jsx`, `Footer.jsx` and complex `Main.jsx`, which consists of other components.

```
frontend
    - homePage
        - HomePage.jsx
        - homePage.scss
        - Header.jsx
        - Footer.jsx
        - main
            - Main.jsx
            - dashboard
                - Dashboard.jsx
                - DashboardFilter.jsx

```

Generic reusable React components should be placed in `src/main/frontend/react`.

### React Sample component

One example or template that you can use when creating new React components is the [DependencyTreePage](https://github.com/sonatype/insight-brain/blob/main/insight-brain-frontend/src/main/frontend/DependencyTree/DependencyTreePage.jsx) component.

⚠️ Note, we no longer use `connect` HOC to create components connected to Redux store. Use redux hooks instead: `import { useSelector, useDispatch } from 'react-redux';`
Also, there is no need to provide a "container" wrapper for each connected component.

React components are used directly. Components that need to integrate with Redux use hooks (`useSelector`, `useDispatch`) directly.

We implement runtime type-safety in React components using the [prop-types](https://www.npmjs.com/package/prop-types) library and all properties should be appropriately typed. This is usually done at the bottom of each component, by specifying various `PropTypes` from the `prop-types` project.

### Using Redux-Toolkit

- There are places of the application where we have actions and reducers in separate files — this is the old approach. The new preferred approach is to use [redux-toolkit](https://redux-toolkit.js.org/).
- Files created using [redux-toolkit](https://redux-toolkit.js.org/) should be named `*Slice`. i.e. `OverviewSlice.js` or `PolicyViolationsSlice.js`.
- Slice files must have individual exports for both the reducer part and the actions.
- Before creating a utility function check if it already exists in `insight-brain-frontend/src/main/frontend/util/reduxToolkitUtil.js`.

### Miscellaneous

#### How to add an Unsaved Changes modal warning to your page

It is an established pattern to have a warning show up when the user is navigating away from a page in which they have unsaved changes. We call this warning the "Unsaved Changes Modal". The following are the conditions required to enable this modal for any given react page.

- Track the _"isDirty"_ state of your page as a boolean flag in the related redux state and keep it up to date.
- Configure the path to your _"isDirty"_ redux state in the router config for the related page: inside the `data` object add an `isDirty` property whose value is a string array representing the path to your _"isDirty"_ state flag, starting from the reducer name.

For example, if you store the "isDirty" flag in the `addWaiver` reducer in a variable called `isAddWaiverPageDirty`, the router config should look like this:

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
- Prefix classes with `iq-`

## Testing

- Every exported item should be unit tested — Be it a Component, a reducer, or a utility function, they _all_ need unit testing.
  - We no longer require testing async actions and selectors since with React Testing Library we test connected component as a whole, so actions and selectors are considered to be implementation details.
- Unit tests should be included in the PR along with the source code they're testing — do not break them down into separate PRs as that makes it harder to review.
- Every story should include functional testing.
- If you're testing a React component —`.jsx` extension— make sure that your corresponding spec file is also using the `.jsx` extension.
- Spec files should be named equal to the source file they’re testing and with the "Spec" suffix. i.e. for `Source.jsx` the corresponding spec file should be `SourceSpec.jsx` — this makes the files easier to find.
- Spec files should go under `insight-brain-frontend/src/test/{mirrored-path-to-component}`. This is similar to how maven projects are set up.
- Avoid using `setTimeout` in test files if possible, this increases test suite's runtime. The alternative is to use `jasmine's` `clock` for simulating time.

### Writing tests for React components

React Testing Library promotes testing of Redux connected components as a whole. Instead of writing separate unit test for components, async actions and reducers, we test components integrated with Redux as a whole. Action creators are considered to be implementation details and don't need tests. We still encourage providing unit tests for Reducers since they are extremely easy to test (pure functions) and usually contain important application logic.

For en example of a test with React Testing Library see `insight-brain-frontend/src/test/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfigurationSpec.jsx`

There are several helper functions in `insight-brain-frontend/src/test/frontend/SpecUtil.js` to help you with writing unit tests for React components. Here are some of the most common:

- `render` wrapper for React Testing Library API that configures Redux Store and Jasmine matchers.
- `axiosMockAdapter` can be used to mock Axios HTTP requests (get / post / put / delete)

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

⚠️ Note, `axiosMockerGenerator` is deprecated. Use `axiosMockAdapter` instead:

```
axiosMock.onGet(url).reply(() => Promise.reject('some error'));
```

In a nutshell: don't create the rejected promise until you need it.
