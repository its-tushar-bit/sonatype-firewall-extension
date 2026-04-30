# CLAUDE.md - Insight Brain Frontend

This file provides guidance to Claude Code (claude.ai/code) when working with the **insight-brain-frontend** module specifically.

## Module Overview

**insight-brain-frontend** is the frontend module for Nexus IQ Server, containing the user interface for all four Sonatype products: **Lifecycle**, **SBOM Manager**, **Firewall**, **Developer**, and **Guide**. It implements React with Redux state management.

This module contains **two separate SPAs**:
1. **Legacy IQ UI** (`src/main/frontend/index.jsx`) — the main IQ Server UI using UI Router, Redux, and `@sonatype/react-shared-components`.
2. **Guide SPA** (`src/main/frontend/guide/index.tsx`) — a separate app using React Router, TypeScript, `@guide/ui-core`, and Radix UI Themes. See the **"Guide SPA"** section below for complete directory structure conventions, naming rules, and architectural rationale.

## Key Responsibilities

- **User Interface**: Complete frontend UI for all IQ Server products
- **State Management**: Redux-based state management with both legacy and modern patterns
- **Component Library**: React components
- **Asset Management**: esbuild-based build system
- **Testing Infrastructure**: Jest with React Testing Library
- **Styling System**: SCSS-based styling with BEM conventions

## Architecture

### Framework Stack

#### Core Frameworks

- **React 19.x**: Component development with hooks
- **Redux 5.x + Redux Toolkit 2.x**: State management
- **@sonatype/react-shared-components**: Sonatype's shared React component library

#### Build Tools

- **esbuild**: Module bundling and asset processing (`esbuild.config.mjs`)
- **SASS**: CSS preprocessing with SCSS syntax
- **ESLint + Prettier**: Code linting and formatting

#### Testing Frameworks

- **Jest 29.x**: Testing framework
- **React Testing Library**: Component testing utilities

#### Development Tools

- **esbuild dev server**: Hot reload development server (`yarn start`)
- **yarn**: Package management
- **Node.js**: JavaScript runtime (version specified in pom.xml)

## Development Commands

### Prerequisites

Ensure you have the correct Node.js and yarn versions (check `pom.xml` for versions):

```bash
# Install Node.js (match version in pom.xml)
# Install yarn globally
npm install -g yarn@<version-from-pom>
```

### Development Server

```bash
# Start backend on port 8072 (from insight-brain-service directory)
mvn exec:java -Dexec.mainClass=com.sonatype.insight.brain.service.InsightBrainService -Dexec.args='server src/test/resources/config-dev.yml' -Ddw.server.applicationConnectors[0].port=8072

# Start frontend dev server (from insight-brain-frontend directory)
yarn start                     # Main bundle only (faster)
```

### Fast Frontend Development Loop with Functional Tests

To iterate on frontend changes without rebuilding `insight-brain-service`, you can run functional tests against the dev server:

1. Start the dev server: `yarn start` (serves on port 8070, proxies `/rest`, `/api`, `/ui`, `/policy-assets`, `/saml` to `localhost:8072`)
2. Run a functional test with `-Dfunctional-test-webpack-dev-server=true` from `insight-brain-java-functional-test/`

```bash
cd insight-brain-java-functional-test
mvn verify -Dit.test=SomeTest#someMethod -Dfunctional-test-webpack-dev-server=true
```

The test server starts on fixed port 8072 and the browser is pointed at the dev server on port 8070, so frontend changes are reflected instantly without any Java rebuild. Works with both local Chrome (`-Drun-functional-tests=local`) and the default Docker Selenium container.

### Testing

```bash
# Run all tests (Jest + lint)
yarn test

# Jest tests only
yarn jest
yarn jest-watch                # Watch mode

# Run specific test file
yarn jest -- <test-name>
```

## Key Service Patterns

### React Component Pattern

```jsx
// ComponentName.jsx
import React from 'react';
import PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import { NxButton } from '@sonatype/react-shared-components';
// Import selectors from dedicated selector files
import { selectFeatureData, selectFeatureLoading } from './featureSelectors';
import { someAction } from './featureSlice';

export default function ComponentName({ someProp }) {
  const dispatch = useDispatch();
  // Use dedicated selectors instead of inline state access
  const data = useSelector(selectFeatureData);
  const loading = useSelector(selectFeatureLoading);

  const handleClick = () => {
    dispatch(someAction());
  };

  return (
    // All CSS classes must use "iq-" prefix
    <div className="iq-component-name">
      <NxButton onClick={handleClick} disabled={loading}>
        {someProp}
      </NxButton>
      {data && (
        // Use BEM naming with iq- prefix
        <div className="iq-component-name__content">
          <span className="iq-component-name__label">Data:</span>
          <span className="iq-component-name__value">{data.name}</span>
        </div>
      )}
    </div>
  );
}

ComponentName.propTypes = {
  someProp: PropTypes.string.isRequired,
};
```

**Important**:

- Always create and use dedicated selectors in separate `featureSelectors.js` files rather than accessing state directly with `useSelector((state) => state.featureName.data)`. This approach provides better performance through memoization, improves testability, and makes state access patterns more reusable across components.
- All CSS class names must use the `iq-` prefix following BEM naming conventions (e.g., `iq-component-name`, `iq-component-name__element`, `iq-component-name--modifier`).

### Redux State Management Pattern

```javascript
// featureSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axios from 'axios';
import { getFeatureDataUrl } from 'MainRoot/util/CLMLocation';
import { Messages } from 'MainRoot/util/CommonServices';

const REDUCER_NAME = 'feature';

const initialState = {
  data: null,
  loading: false,
  error: null,
  isDirty: false,
};

// Async thunk for data fetching
export const fetchData = createAsyncThunk(`${REDUCER_NAME}/fetchData`, async (params, { rejectWithValue }) => {
  try {
    const response = await axios.get(getFeatureDataUrl(params.id));
    return response.data;
  } catch (error) {
    return rejectWithValue(error);
  }
});

// Separate reducer functions (not inline)
const fetchDataRequested = (state) => {
  state.loading = true;
  state.error = null;
};

const fetchDataFulfilled = (state, { payload }) => {
  state.loading = false;
  state.data = payload;
};

const fetchDataFailed = (state, { payload }) => {
  state.loading = false;
  state.error = Messages.getHttpErrorMessage(payload);
};

const setDirty = (state, action) => {
  state.isDirty = action.payload;
};

const clearError = (state) => {
  state.error = null;
};

const featureSlice = createSlice({
  name: REDUCER_NAME,
  initialState,
  reducers: {
    setDirty,
    clearError,
  },
  extraReducers: {
    [fetchData.pending]: fetchDataRequested,
    [fetchData.fulfilled]: fetchDataFulfilled,
    [fetchData.rejected]: fetchDataFailed,
  },
});

export const actions = {
  ...featureSlice.actions,
  fetchData,
};

export default featureSlice.reducer;
```

**Create separate selectors file (`featureSelectors.js`)**:

```javascript
// featureSelectors.js
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectFeatureSlice = prop('feature');

export const selectFeatureData = createSelector(selectFeatureSlice, prop('data'));
export const selectFeatureLoading = createSelector(selectFeatureSlice, prop('loading'));
export const selectFeatureError = createSelector(selectFeatureSlice, prop('error'));
export const selectFeatureIsDirty = createSelector(selectFeatureSlice, prop('isDirty'));
```

### SCSS Structure Pattern

```scss
// _componentName.scss
.iq-component-name {
  // Block styles
  display: flex;
  padding: 1rem;

  &__element {
    // Element styles (BEM)
    font-size: 1.2rem;
  }

  &--modifier {
    // Modifier styles (BEM)
    background-color: $primary-color;
  }

  // Nested components
  .iq-sub-component {
    margin-top: 0.5rem;
  }
}
```

## Styling Considerations

### SCSS Conventions

- **Prefix all classes with `iq-`**
- **Use BEM naming convention**: `block__element--modifier`
- **Use SCSS variables for colors and dimensions**: Check `_variables.scss` for existing variables before creating new ones to avoid duplication
- **Import new SCSS files**: Add `@use` statements to `scss/scss.scss` to include your new stylesheets in the build

### Dark Mode Support

**RSC components provide built-in dark mode support**. When creating custom components or overriding RSC colors, ensure dark mode compatibility:

```scss
// Import dark mode helpers
@use '~@sonatype/react-shared-components/scss-shared/nx-dark-mode-helpers';

.iq-custom-component {
  // Light mode styles
  background-color: var(--nx-swatch-blue-90);
  border-color: var(--nx-swatch-blue-70);

  // Dark mode overrides
  @include nx-dark-mode-helpers.dark-mode {
    background-color: var(--nx-swatch-blue-30);
    border-color: var(--nx-swatch-blue-20);
  }
}
```

**Best Practices**:

- **Use RSC CSS variables**: Prefer `var(--nx-color-*)` or `var(--nx-swatch-*)` over hardcoded colors (color palette details are available at https://gallery.sonatype.dev/#/pages/Color%20Palettes )
- **Test both themes**: Always verify components work in light and dark mode
- **Check existing variables**: Look in `_variables.scss` for theme-aware color definitions

## Testing Guidelines

### Test Categories

**Philosophy**: Use as few mocks as possible to ensure the system behaves as a user operating the system would expect. Prefer integration-style tests over heavily mocked unit tests.

### Jest Tests (Preferred)

```jsx
// ComponentName.jestspec.jsx
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import ComponentName from 'MainRoot/featureName/ComponentName';
import { getFeatureDataUrl } from 'MainRoot/util/CLMLocation';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('ComponentName', () => {
  let axiosMock, stateGoSpy, mockRouterState;

  // Default preloaded state that can be customized per test
  const defaultPreloadedState = {
    feature: {
      data: null,
      loading: false,
      error: null,
      isDirty: false,
    },
    router: {
      currentParams: { appId: 'test-app-id' },
      currentState: { name: 'feature.view' },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Spy on router actions for navigation testing
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

    // Mock router state context for URL generation
    mockRouterState = {
      href: jest.fn().mockImplementation((stateName) => {
        switch (stateName) {
          case 'feature.details':
            return '#/feature/details';
          case 'feature.edit':
            return '#/feature/edit';
          default:
            return '#/mocked-default-href';
        }
      }),
      get: jest.fn(),
      includes: jest.fn(),
    };
    jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);

    // Mock network requests with axiosMockAdapter
    axiosMock.onGet(getFeatureDataUrl('test-app-id')).reply(200, { id: 1, name: 'Test Feature' });
  });

  // Helper function to render with customizable state
  const renderComponent = (props = {}, preloadedState) => {
    return render(<ComponentName someProp="test" {...props} />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render component with initial state', () => {
    renderComponent();

    expect(screen.getByText('test')).toBeInTheDocument();
  });

  it('should handle network requests properly', async () => {
    renderComponent();

    // Wait for async operations to complete
    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(getFeatureDataUrl('test-app-id'));
    });

    expect(screen.getByText('Test Feature')).toBeInTheDocument();
  });

  it('should navigate when button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByRole('button', { name: 'Go to Details' }));

    // Test navigation using stateGo spy
    expect(stateGoSpy).toHaveBeenCalledWith('feature.details', {
      appId: 'test-app-id',
      featureId: 1,
    });
  });

  it('should generate correct href URLs', () => {
    renderComponent();

    const detailsLink = screen.getByRole('link', { name: 'View Details' });
    expect(detailsLink).toHaveAttribute('href', '#/feature/details');
  });

  it('should handle custom preloaded state', () => {
    const customState = {
      ...defaultPreloadedState,
      feature: {
        ...defaultPreloadedState.feature,
        data: { id: 2, name: 'Custom Feature' },
        loading: true,
      },
    };

    renderComponent({}, customState);

    expect(screen.getByText('Custom Feature')).toBeInTheDocument();
    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should handle network errors gracefully', async () => {
    axiosMock.onGet(getFeatureDataUrl('test-app-id')).reply(500, { message: 'Server Error' });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/error occurred/i)).toBeInTheDocument();
    });
  });
});
```

### Key Testing Patterns

**Import from SpecUtil.js** (not directly from @testing-library/react):

```javascript
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
```

**Prefer userEvent over fireEvent** for more realistic user interactions:

```javascript
import userEvent from '@testing-library/user-event';

// Preferred - more realistic user interactions
const user = userEvent.setup();
await userEvent.click(button);
await userEvent.type(input, 'text');
await userEvent.selectOptions(select, 'option1');

// Use fireEvent only when userEvent doesn't cover the interaction
fireEvent.scroll(window, { target: { scrollY: 100 } });
```

**Mock Network Requests** (preferred over Redux/state mocks):

```javascript
// Use axiosMockAdapter for all HTTP mocking
beforeAll(() => {
  axiosMock = axiosMockAdapter();
});

// Mock specific endpoints
axiosMock.onGet('/api/endpoint').reply(200, mockData);
axiosMock.onPost('/api/create').reply(201, createdData);
```

**Router State Mocking** for href testing:

```javascript
const mockRouterState = {
  href: jest.fn().mockImplementation((stateName) => {
    switch (stateName) {
      case 'target.state':
        return '#/target/path';
      default:
        return '#/default';
    }
  }),
  get: jest.fn(),
  includes: jest.fn(),
};
jest.spyOn(RouterStateContext, 'useRouterState').mockReturnValue(mockRouterState);
```

**Navigation Testing** with stateGo spy:

```javascript
const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

// In test
const user = userEvent.setup();
await user.click(navigationButton);
expect(stateGoSpy).toHaveBeenCalledWith('target.state', { param: 'value' });
```

**Flexible State Management**:

```javascript
const defaultPreloadedState = {
  /* base state */
};

const renderComponent = (props = {}, preloadedState) => {
  return render(<Component {...props} />, {
    preloadedState: preloadedState || defaultPreloadedState,
  });
};

// Use custom state when needed
renderComponent({}, { ...defaultPreloadedState, customProp: 'value' });
```

### Test File Organization

- **Jest tests**: `src/test/frontend/path/to/ComponentName.jestspec.jsx`
- **Test utilities**: `src/test/frontend/SpecUtil.js`

### Testing Anti-Patterns

**Avoid these patterns:**

- **`fireEvent` for user interactions** — use `userEvent` instead for realistic behavior
- **Direct Redux store mocking** — use `preloadedState` via the `render` helper from `SpecUtil`
- **`screen.getByTestId`** — prefer accessible queries (`getByRole`, `getByLabelText`)
- **Enzyme (`shallow`/`mount`)** — use React Testing Library exclusively

## Key Patterns and Utilities

### Unsaved Changes Modal

For pages with form data, implement unsaved changes warning using **selector functions** (preferred approach):

```javascript
// featureSelectors.js - Create a memoized selector for dirty state
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { equals } from 'ramda';

export const selectFeatureSlice = prop('feature');

export const selectFeatureData = createSelector(selectFeatureSlice, prop('data'));
export const selectFeatureOriginalData = createSelector(selectFeatureSlice, prop('originalData'));

// Memoized selector that computes dirtiness by comparing current vs original data
export const selectIsDirty = createSelector(
  selectFeatureData,
  selectFeatureOriginalData,
  (currentData, originalData) => currentData !== originalData
);

// For complex forms, combine multiple dirty checks
export const selectFormIsDirty = createSelector(selectFeatureSlice, (state) => {
  const { data, originalData, settings, originalSettings, isConfigDirty } = state;

  const isDataDirty = data !== originalData;
  const isSettingsDirty = settings !== originalSettings;

  return isDataDirty || isSettingsDirty || isConfigDirty;
});
```

```javascript
// In router configuration - use selector function instead of state path
import { selectIsDirty } from 'MainRoot/featureName/featureSelectors';

.state('featureName.edit', {
  // ... other config
  data: {
    isDirty: selectIsDirty  // Pass selector function directly
  }
})
```

**Legacy approach** (still supported but not preferred):

```javascript
// Only use this pattern for simple cases or when migrating existing code
.state('featureName', {
  data: {
    isDirty: ['featureName', 'isPageDirty']  // State path array
  }
})
```

**Benefits of Selector Approach**:

- **Memoization**: Selector only recalculates when dependencies change
- **Reusability**: Can be used across multiple components/routes
- **Complex Logic**: Can combine multiple state slices and perform complex comparisons
- **Performance**: Better than storing redundant dirty flags in state

### Shared Components

Always check **React Shared Components Library** before building custom components:

**For AI/Claude**: The RSC documentation website (https://gallery.sonatype.dev/) is JavaScript-rendered and not readable by static crawlers. To help with RSC usage, ask the human to provide the location of a local clone of the `@sonatype/react-shared-components` repository so you can examine the source code, component APIs, and examples directly.

**For Developers**: Browse https://gallery.sonatype.dev/ for available components and their usage examples.

- **NxButton, NxTable, NxForm**: Common UI components
- **NxLoadWrapper**: Loading states and error handling

## Common Development Tasks

### Adding a New Feature

1. **Create feature directory**: `src/main/frontend/newFeature/`
2. **Implement React components**: Use modern patterns and hooks
3. **Create Redux slice**: Use Redux Toolkit
4. **Add routing**: Configure routes
5. **Write tests**: Jest tests for new components
6. **Add styling**: SCSS with BEM conventions

## Migration Status

### Current State

- **React 19**: Required for all UI components
- **Jest**: Sole testing framework (Jasmine/Karma migration complete)
- **Redux Toolkit**: Preferred for state management
- **Legacy Redux**: Still present in some areas, migrate to Redux Toolkit when touching
- **esbuild**: Build system (migrated from Webpack)

### Testing Strategy

Frontend changes typically require updates to multiple test layers:

- **Unit/Integration Tests**: Jest and Jasmine tests in this directory (`src/test/frontend/`)
- **Java Functional Tests**: Located mostly in `insight-brain-java-functional-test/` directory
  - **When to update**: Any UI changes, new features, or modified user workflows
  - **Coverage**: End-to-end scenarios, cross-browser testing, API integration
  - **Important**: These tests are outside this frontend directory but are critical for validating frontend changes in production-like environments

## Guide SPA

### Overview

The Guide SPA is a **separate single-page application** that provides the Sonatype Guide product for self-hosted (on-prem) customers. It is completely independent from the legacy IQ Server UI — it has its own entry point, routing system, build bundle, and component library.

The SaaS version of Guide already exists as a Next.js application in the `seaworthy` repository. The self-hosted Guide SPA reuses the same shared UI components via `@guide/ui-core`.

### Why React Router (Not UI Router)

The legacy IQ Server UI uses `@uirouter/react` (a state-based router inherited from AngularJS patterns). The Guide SPA uses **React Router 7** instead:

1. **API parity with Next.js**: The Guide SaaS app uses Next.js, whose routing APIs (`useRouter`, `usePathname`, `useSearchParams`, `Link`) closely mirror React Router's APIs. This 1:1 mapping means shared components in `@guide/ui-core` work in both environments with a thin adapter layer.
2. **NavigationAdapter pattern**: `@guide/ui-core` defines a `NavigationAdapter` interface. Each host provides its own adapter:
   - **SaaS (seaworthy)**: `NextjsNavigationProvider` — wraps Next.js `useRouter`
   - **Self-hosted (insight-brain)**: `useReactRouterAdapter()` — wraps React Router hooks
   - Both conform to the same interface, so all shared components work identically in both environments.
3. **Minimal translation overhead**: Next.js and React Router share similar concepts (path-based routing, search params, `Link`). UI Router would have required a much more complex adapter with significant semantic mismatches.

### Guide SPA vs Legacy IQ UI

| Aspect | Legacy IQ Server UI | Guide SPA |
|--------|-------------------|-----------|
| Routing | UI Router (`@uirouter/react`) | React Router 7 (`react-router`) |
| Language | JavaScript (`.jsx`) | TypeScript (`.tsx`) |
| State management | Redux Toolkit | React Context + `@guide/ui-core` hooks |
| Component library | `@sonatype/react-shared-components` | `@guide/ui-core` + Radix UI Themes |
| Styling | SCSS with `iq-` prefix (BEM) | CSS Modules + Radix UI Themes |
| Entry point | `src/main/frontend/index.jsx` | `src/main/frontend/guide/index.tsx` |
| Build output | `target/.../assets/bundle.js` | `target/.../assets/guide/guide.js` |
| Test pattern | `*.jestspec.jsx` | `*.jestspec.tsx` |


### Testing Conventions for Guide Code

- **Test file location**: `src/test/frontend/guide/` mirrors the source structure under `src/main/frontend/guide/`
- **Test file naming**: `*.jestspec.tsx`
- **Test utilities**: Use `guide/test-utils/test-utils.tsx` which wraps components with `MemoryRouter`, `Theme`, and `NavigationProvider`
- **Path alias**: `GuideRoot/*` imports from guide source (e.g., `import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter'`)
- **Assertions**: Use React Testing Library queries (`getByRole`, `getByText`) — avoid `getByTestId`
- **User interactions**: Use `userEvent`, not `fireEvent`

### Shared UI Package (`@guide/ui-core`)

Published from the `seaworthy` repo (`ui/packages/ui-core`). Contains:
- **Components**: `Button`, `CVSSBadge`, `FilteredPageLayout`, `VulnerabilityResultCard`, etc.
- **Hooks**: `useNavigate`, `useAdapterPathname`, `useAdapterSearchParams`, `useLink`, `useForm`
- **Types**: `NavigationAdapter`, `LinkProps`, `FormProps`, `ReadonlySearchParams`
- **Adapters**: `NavigationProvider`, `GatingProvider`
- **Utilities**: formatters, date helpers, URL utils, constants

```typescript
import { NavigationProvider, Button } from '@guide/ui-core';
import type { NavigationAdapter } from '@guide/ui-core';
```

### Do NOT Mix Guide and Legacy UI Code

- Guide code must NOT import from `MainRoot/*`, Redux store, UI Router, or `@sonatype/react-shared-components`.
- Legacy IQ UI code must NOT import from `GuideRoot/*`.
- The two SPAs share no runtime state — they are separate bundles with separate entry points.

## Related Modules

- **insight-brain-service**: Backend REST API and business logic
- **insight-brain-data**: Database entities and data access
- **insight-brain-java-functional-test**: End-to-end testing
- **@sonatype/react-shared-components**: UI component library

## Important Files

- **`package.json`**: Dependencies and build scripts
- **`esbuild.config.mjs`**: Build configuration
- **`jest.config.js`**: Test configuration
- **`src/main/frontend/MainModule.js`**: Application entry point
- **`src/main/frontend/scss/scss.scss`**: Main stylesheet entry
- **`src/test/frontend/SpecUtil.js`**: Test utilities and helpers

---

**Note**: This frontend module serves all four Sonatype products and requires careful consideration of cross-product compatibility when making changes. Always test changes across different product contexts and deployment scenarios.
