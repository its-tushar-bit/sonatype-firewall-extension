/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render as rtlRender, within } from '@testing-library/react';
import { configureStore as reduxToolkitConfigureStore, getDefaultMiddleware } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import * as PropTypes from 'prop-types';
import reducers from '../../main/frontend/reduxConfig/reducers';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import { faker } from '@faker-js/faker';
import userEvent from '@testing-library/user-event';

export const WAIVER_CREATE_TIME = '2022-08-18';
export const WAIVER_EXPIRATION_TIME = '2023-08-18';

window.CLM = {
  path: '../brain/',
};
window.SpecUtil = {
  flushPromise: () => new Promise((resolve) => setTimeout(resolve, 0)),

  /**
   * This is used to test redux actions creators (mostly used for testing async actions).
   * This is factory function that takes state and creates redux store mock.
   * Use store.dispatch() to dispatch action under test
   * Use store.getActions() to test actions dispatched
   *
   *    var initialState = [
   *      {foo: 1, bar: 2},
   *      {foo: 1, bar: 1},
   *      {foo: 3, bar: 3}
   *    ];
   *    var store = SpecUtil.mockReduxStore(initialState);
   *    var sortFields: ['-foo', 'bar'];
   *    store.dispatch(actions.sortResults(sortFields));
   *
   *    expect(store.getActions().length).toBe(2);
   *
   *    // this action will update sortFields in the state
   *    expect(store.getActions()[0]).toEqual({
   *      type: 'SORT_RESULTS_REQUESTED',
   *      payload: {
   *        sortFields: sortFields
   *      }
   *    });
   *
   *    expect(store.getActions()[1]).toEqual({
   *      type: 'SORT_RESULTS_FULFILLED',
   *      payload: [
   *        {foo: 3, bar: 3},
   *        {foo: 1, bar: 1},
   *        {foo: 1, bar: 2}
   *      ]
   *    });
   *
   * @param state
   * @returns {dispatch: Function, getActions: Function}
   */
  mockReduxStore: function (state) {
    state = state || {};
    var actions = [];

    function getState() {
      return state;
    }

    function getActions() {
      return actions;
    }

    function dispatch(action) {
      if (typeof action === 'function') {
        return action(dispatch, getState);
      } else {
        actions.push(action);
        return action;
      }
    }

    return {
      dispatch: dispatch,
      getActions: getActions,
      getState: getState,
    };
  },

  /**
   * Deprecated! Please use axiosMockAdapter()!
   *
   * Returns a function that can be used to mock axios calls to the different http verbs.
   *
   * The returned function takes in an object of the form { httpVerb: callDefinitions },
   * where _httpVerb_ is either `post`, `put`, `get`, or `del`
   * —note that `delete` is a javascript reserved keyword, so we have to use `del` instead)—
   * and _callDefinitions_ is an object of the form {urlString: response},
   * where _urlString_ is a string representing the expected url,
   * and _response_ is any value that should be returned when the given url is requested with the given verb.
   *
   * Example usage:
   *    const mockAxiosCalls = axiosMockerGenerator(axios);
   *    mockAxiosCalls({
   *      get: {
   *        '/url/1': { data: 1 },
   *        '/url/2': Promise.resolve({ data: 2 })
   *      },
   *      post: {
   *        '/another/url': Promise.reject({ status: 404 })
   *      },
   *      put: {}, // passing an empty object to one of the verbs would result in a simple spy on it.
   *      del: {
   *        '/url/4': Promise.resolve({ data: 'success' }),
   *        '/url/5': Promise.reject({ status: 500 })
   *      }
   *    });
   * @param  axios
   * @returns Function
   * @deprecated - use axiosMockAdapter()
   */
  axiosMockerGenerator: function (axios) {
    function mockVerb(axiosInstance, method, urlMap) {
      const spy = jest.spyOn(axiosInstance, method);
      if (urlMap) {
        spy.mockImplementation(function (url) {
          const mock = urlMap[url];
          if (typeof mock === 'function') {
            return mock();
          } else {
            return mock;
          }
        });
      }
    }

    return function (responses) {
      responses = responses || {};

      if (responses.get) {
        mockVerb(axios, 'get', responses.get);
      }
      if (responses.post) {
        mockVerb(axios, 'post', responses.post);
      }
      if (responses.put) {
        mockVerb(axios, 'put', responses.put);
      }
      if (responses.del) {
        mockVerb(axios, 'delete', responses.del);
      }
    };
  },

  requestIdleCallbackInvokeImmediateJest: () => {
    window.requestIdleCallback = jest.fn().mockImplementation((cb) => {
      setTimeout(() => {
        cb();
      }, 0);
    });
  },
};

export function configureStore(opts) {
  return reduxToolkitConfigureStore({
    ...opts,
    middleware: getDefaultMiddleware({
      // we include non-serializable values (mostly Sets) in our state. We accept the risk, and don't want thousands
      // of error messages about it.
      serializableCheck: false,
    }),
  });
}

// render wrapper for React Testing Library
function render(
  ui,
  { preloadedState, store = configureStore({ reducer: reducers, preloadedState }), ...renderOptions } = {}
) {
  function Wrapper({ children }) {
    return <Provider store={store}>{children}</Provider>;
  }
  Wrapper.propTypes = {
    children: PropTypes.any,
  };
  return {
    ...rtlRender(ui, { wrapper: Wrapper, ...renderOptions }),
    store,
  };
}

// re-export everything
export * from '@testing-library/react';
// override render method
export { render };
// export userEvent
export { userEvent };

let mock;

/**
 * call this from beforeAll()
 *
 * Example:
 *
 * import { axiosMockAdapter } from 'TestRoot/SpecUtil';
 * let mock;
 * beforeAll(() => {
 *   mock = axiosMockAdapter();
 * });
 */
export function axiosMockAdapter(options) {
  mock = new MockAdapter(axios, options);
  return mock;
}

afterEach(() => {
  mock && mock.reset();
});

afterAll(() => {
  mock && mock.restore();
  mock = null;
});

/**
 * Gets the matched elements inside a node using RTL.
 *
 * Usage example:
 * ```
 * const popover = document.getElementById('edit-licenses-popover');
 * const myNode = queryByTextWithin(/Effective Licenses/, popover).first;
 * const myNodes = queryByTextWithin('Apache-1.1', '.iq-license-info-section').all;
 * ```
 *
 * This is an abbreviated way to use:
 * ```
 * within(document.querySelector(selector)).queryAllByText(regExp)
 * ```
 * @param  {(string|RegExp)} regexpOrString
 * @param  {(string|HTMLElement)} selectorOrDOMNode
 * @returns {{all: HTMLElement[], first: ?HTMLElement}}
 */
export const queryByTextWithin = (regexpOrString, selectorOrDOMNode) => {
  let nodes;

  if (!(regexpOrString instanceof RegExp || typeof regexpOrString === 'string')) {
    throw Error('queryByTextWithin - regexpOrString must be a RegExp object or a string');
  }

  const validateNodeType = (nodeToValidate) => {
    if (nodeToValidate instanceof HTMLCollection) {
      throw Error('queryByTextWithin - selectorOrDOMNode param must query or provide a single DOM element');
    } else if (!(nodeToValidate instanceof HTMLElement)) {
      throw Error('queryByTextWithin - selectorOrDOMNode param must be either a query string or a single DOM element');
    }
  };

  if (typeof selectorOrDOMNode === 'string') {
    const withinNode = document.querySelector(selectorOrDOMNode);
    validateNodeType(withinNode);
    nodes = within(withinNode).queryAllByText(regexpOrString);
  } else {
    validateNodeType(selectorOrDOMNode);
    nodes = within(selectorOrDOMNode).queryAllByText(regexpOrString);
  }

  return { all: nodes ?? [], first: nodes?.[0] };
};

/**
 * generates a list of a random length using the supplied itemGenerator
 *
 * @template T
 * @param itemGenerator {function(): T}
 * @param options {{ min: number, max: number }=}
 * @returns {T[]}
 */
export function generateList(itemGenerator, options) {
  const { min, max } = options ?? { min: 1, max: 10 };

  let numItems = faker.datatype.number({ min, max });

  const items = [];

  for (let i = 0; i < numItems; i++) {
    items.push(itemGenerator());
  }

  return items;
}

export function mockInterceptionObserver() {
  class IntersectionObserver {
    constructor() {
      this.root = null;
      this.rootMargin = '';
      this.thresholds = [];
    }

    disconnect() {
      return null;
    }

    observe() {
      return null;
    }

    takeRecords() {
      return [];
    }

    unobserve() {
      return null;
    }
  }

  window.IntersectionObserver = IntersectionObserver;
  global.IntersectionObserver = IntersectionObserver;
}

/* * Sets up the portal container structure in the document body.
 * The DOM is structured as follows:
body
└── div.nx-page (pageRoot)
    └── div#iq-content (contentRoot)
        └── div#iq-sidebar-container (sidebarContainerRoot)
 */
export const setupPortalContainer = () => {
  const pageRoot = global.document.createElement('div');
  pageRoot.setAttribute('class', 'nx-page');

  const contentRoot = global.document.createElement('div');
  contentRoot.setAttribute('id', 'iq-content');

  const sidebarContainerRoot = global.document.createElement('div');
  sidebarContainerRoot.setAttribute('id', 'iq-sidebar-container');

  const body = global.document.querySelector('body');
  body.appendChild(pageRoot);
  pageRoot.appendChild(contentRoot);
  contentRoot.appendChild(sidebarContainerRoot);
};

export const getSpecUtil = () => window.SpecUtil;
