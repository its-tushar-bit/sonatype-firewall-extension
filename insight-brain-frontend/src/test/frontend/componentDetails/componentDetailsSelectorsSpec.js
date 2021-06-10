/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetails,
  selectComponentPagination,
} from '../../../main/frontend/componentDetails/componentDetailsSelectors';

describe('componentDetailsSelectors', () => {
  describe('selectComponentDetails', () => {
    const mockState = {
      router: {
        currentParams: {
          hash: 'some-component-hash',
        },
      },
      applicationReport: {
        metadata: {
          application: {
            name: 'The App',
            organization: {
              name: 'The Org',
            },
          },
          reportTime: 1623135382098,
          reportTitle: 'Title of Report',
        },
        selectedReport: {
          displayedEntries: [
            {
              hash: 'a-component-hash',
            },
            {
              derivedComponentName: 'My Component',
              hash: 'some-component-hash',
              componentIdentifier: { format: 'maven' },
              derivedDependencyType: 'transitive',
            },
            {
              hash: 'another-component-hash',
            },
          ],
        },
      },
    };
    it('derives componentDetails from the selectedReport metadata, and the selectedComponent', () => {
      const expected = {
        name: 'My Component',
        hash: 'some-component-hash',
        componentIdentifier: { format: 'maven' },
        dependencyType: 'transitive',
        isInnerSource: false,
        format: 'maven',
        metadata: {
          applicationName: 'The App',
          organizationName: 'The Org',
          reportTime: 1623135382098,
          reportTitle: 'Title of Report',
        },
      };
      const actual = selectComponentDetails(mockState);
      expect(actual).toEqual(expected);
    });
  });

  describe('selectComponentPagination', function () {
    const mockState = {
      router: {
        currentParams: {
          hash: 'some-component-hash',
        },
        currentState: {
          name: 'router-state-name',
        },
      },
      applicationReport: {
        selectedReport: {
          displayedEntries: [
            {
              hash: 'a-component-hash',
            },
            {
              hash: 'some-component-hash',
            },
            {
              hash: 'another-component-hash',
            },
          ],
        },
      },
    };
    // we need to use the uiRouterState from props because it is passed by context
    // so we mock it here and pass it in as the props second selector argument
    // the router state is used to determine the HREF of next and prev components
    const uiRouterState = {
      href(routeName, { hash }) {
        return `${routeName}.${hash}`;
      },
    };

    it('derives pagination from selected report component list, selectedComponentIndex and currentRouteName', () => {
      const expected = {
        next: 'router-state-name.another-component-hash',
        prev: 'router-state-name.a-component-hash',
        currentPage: 2,
        pageCount: 3,
      };
      const actual = selectComponentPagination(mockState, { uiRouterState });
      expect(actual).toEqual(expected);
    });

    it('returns a prev property of null when there are no components before the current in the list', () => {
      const state = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'a-component-hash',
          },
        },
      };
      const expected = {
        next: 'router-state-name.some-component-hash',
        prev: null,
        currentPage: 1,
        pageCount: 3,
      };
      const actual = selectComponentPagination(state, { uiRouterState });
      expect(actual).toEqual(expected);
    });

    it('returns a next property of null when there is no more components after the current in the list', () => {
      const state = {
        ...mockState,
        router: {
          ...mockState.router,
          currentParams: {
            hash: 'another-component-hash',
          },
        },
      };
      const expected = {
        next: null,
        prev: 'router-state-name.some-component-hash',
        currentPage: 3,
        pageCount: 3,
      };
      const actual = selectComponentPagination(state, { uiRouterState });
      expect(actual).toEqual(expected);
    });
  });
});
