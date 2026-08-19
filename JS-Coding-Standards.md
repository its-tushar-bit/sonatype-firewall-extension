<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# Sonatype Insight Frontend Coding Standards

*Note: This guide applies only to the Insight team which works on the Nexus Lifecycle products.*

At Sonatype we value the stability and maintainability of the code base while stressing the ability of the developers to quickly understand, consume and update the code base. We adopt code practices which facilitate our developers in rapidly creating new features without jeopardizing the existing feature set. Below are a set of code guidelines that we enforce in order to ensure that our values are upheld. The guidelines are short and written with consideration that different developers approach problems differently; they focus on practices that we have found optimize our ability to do our job without stifiling the process of development. This document is maintained in source control and therefore fluid, we encourage pull requests so that our code guidelines improve with our team.

## Technology Stack
* [React](https://react.dev)
  * We use React 16 for our frontend framework
  * We utilize React components as they provide a best practice for building user interfaces with clear inputs and outputs
* [Redux](https://redux.js.org/)
  * We use Redux for state management
* [Jasmine](http://jasmine.github.io)
  * We use Jasmine for unit testing (legacy)
* [Jest](https://jestjs.io/)
  * We use Jest for unit testing
* [Selenide](http://selenide.org)
  * We use Selenide for functional testing

## Core Development
* We prefer native functions over library functions
  * While we have a preferred technological stack, we desire portable code that can be understood by any developer. As such we prefer the use of native functions over those provided by a framework
  * Likewise, we prefer utilizing our core stack over other libraries
  * As an example, we prefer the use of Array.prototype.forEach over $.each
* We use the IIFE design pattern
  * This allows for a defined execution context for each code block
  * This ensures privacy of code blocks
* We use strict mode for all javascript execution
  * We value failing fast and encourage all errors to be thrown
  * We value security and the enhancements enforced by strict more
* We prefer descriptive names for functions and variables rather than commenting
  * Of course, sometimes that may not be enough, and a comment would aide in understanding, left to developer's discretion

## Component-based application architecture

Components are the foundation of our React application. Each component should have a single responsibility and well-defined inputs (props) and outputs.

We follow "Component-based application architecture" while developing new functionality (and gradually refactoring existing code).

See `react/sharedComponents` for an example of component-based approach, file structure and naming convention.

## File structure
Since the application is a component tree, the directory structure should reflect the component tree, where each component is hosted in its own directory with the same name.

Here is the desired file structure for a typical component:
```
/components
  /FeatureName
    FeatureName.jsx           # Main component file
    FeatureName.scss          # Component styles (if not using _prefix)
    _FeatureName.scss         # Component styles (SCSS partial, prefixed with _)
    FeatureName.jestspec.jsx  # Jest tests
    FeatureNameSpec.jsx       # Legacy Jasmine tests (migrate to Jest)
    featureSelectors.js       # Redux selectors (separate file)
    featureSlice.js           # Redux slice (if needed)
    module.js                 # Module registration (if needed)
    index.js                  # Barrel exports (optional)
```

For simple components, the test file may be colocated:
```
/components
  /Checkbox
    Checkbox.jsx
    _Checkbox.scss
    Checkbox.jestspec.jsx
```

Top-level directories contain reusable utilities and patterns:
```
/util/                      # Cross-cutting utilities
/hooks/                     # Custom React hooks
/selectors/                 # Shared selectors
/services/                  # Service layer (API calls, etc.)
```

## Component Naming Standards

* The goal is to use consistent naming across:
  * Component/export name
  * File name
  * Directory name
  * Component template file
  * Component SCSS partial file (prefixed with `_`)

* We use camelCase naming convention for:
  * Component names
  * File names
  * Directory names
  * Hook names (e.g., `useFeatureData`)

* For JavaScript modules that are not React components, suffix with their type:
  * `service.js`, `util.js`, `actions.js`, `reducer.js`, `selectors.js`
  * This makes it immediately clear what type of module you're importing

## React Development

* Each React component should exist in its own directory
  * We value the ability to easily find where code resides
  * We understand that small code files encourages the decomposition of a problem
  * We follow other language practices of encapsulating one object into one file
* Each React component should have its own file
  * We value the ability to easily access code
  * This allows the component and its utilities to reside in the file system beside each other
* React components should use functional syntax with hooks
  * This is the modern React pattern and aligns with current best practices
  * Class components are legacy and should only be used when maintaining existing code
* Component logic should be organized logically within the component
  * Hooks at the top
  * Helper functions below
  * JSX at the bottom
```javascript
  function ComponentName({ prop }) {
    const [state, setState] = useState(initialState);
    const dispatch = useDispatch();

    const handleClick = () => {
      dispatch(someAction());
    };

    return <div onClick={handleClick}>Component</div>;
  }
```

### React Development - Hooks

* **Use hooks at the top of the component**
```javascript
  function ComponentName({ prop }) {
    const [state, setState] = useState(initialState);
    const dispatch = useDispatch();
    const selectData = useSelector(selectFeatureData);

    const handleClick = () => {
      dispatch(someAction());
    };

    return <div onClick={handleClick}>Component</div>;
  }
```

* **Keep helper functions below the hooks**
```javascript
  function ComponentName({ prop }) {
    const [state, setState] = useState(initialState);

    const computeValue = (value) => {
      // computation logic
      return result;
    };

    const handleClick = () => {
      setState(computeValue(state));
    };

    return <div onClick={handleClick}>Component</div>;
  }
```

### Redux State Management

* **Use Redux Toolkit** for new state management
  * Prefer `createSlice` and `createAsyncThunk` over manual reducer construction
* **Create dedicated selector files** (`featureSelectors.js`)
  * Use `createSelector` from Redux Toolkit for memoized selectors
  * Import selectors from dedicated files, not inline in components
* **Use dedicated action files** when actions are shared across multiple components

### Unit Testing

* **Jest** is the preferred testing framework for new tests
* **Jasmine** is maintained for legacy tests but new tests should use Jest
* Test files should mirror the source file structure
  * `ComponentName.jestspec.jsx` for Jest tests
  * `ComponentNameSpec.jsx` for legacy Jasmine tests

# HTML guidelines
* If all tag attributes fit in one line - they can be inline. Otherwise each attribute should be in its own line:
```html
<element attr1
         attr2
         attr3>
</element>
```

* If opening tag, HTML content (inner HTML)  and closing tag fit in one line - they can be inline. Otherwise opening and closing tags should be in their own line:
```html
<element>
    really long inner html
</element>
```

# Style guidelines
* One-off styles should be referenced by ID and do not neeed a styleguide example

# Selenide Development
* Typically our page objects contain a root object, in cases where that root selector is known (i.e. #someid) we should use that in subqueries directly
  * Replace root.$(".someclass") with $("#someid .someclass") to save roundtrips
* Rather than query for a list of elements and grab a certain one, tighten up the css selector to get the desired item back directly
  * Replace $$(".someclass").get(0) with $(".someclass:first-child") or $(".someclass:nth-child(7)") for example
* Use `#id` instead of `.class` selectors when possible to minimize dependency on class names. (which means adding new ids to HTML if needed)
