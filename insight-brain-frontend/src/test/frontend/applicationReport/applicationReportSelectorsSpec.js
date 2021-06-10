/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectSelectedComponent,
  selectSelectedComponentIndex,
} from '../../../main/frontend/applicationReport/applicationReportSelectors';

describe('applicationReportSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
      },
    },
    applicationReport: {
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

  describe('selectSelectedComponent', () => {
    it('derives selectedComponent from router params hash and the selectedReport component list', () => {
      const expected = {
        derivedComponentName: 'My Component',
        hash: 'some-component-hash',
        componentIdentifier: { format: 'maven' },
        derivedDependencyType: 'transitive',
      };
      const actual = selectSelectedComponent(mockState);
      expect(actual).toEqual(expected);
    });

    it('returns undefined if the router hash does not have a matching component in the selectedReport component list', () => {
      const state = {
        ...mockState,
        router: {
          currentParams: {
            hash: 'i-just-typed-this-in-i-wonder-what-happens',
          },
        },
      };
      const actual = selectSelectedComponent(state);
      expect(actual).not.toBeDefined();
    });

    it('returns undefined if the selectedReport does not have a component list', () => {
      const state = {
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          selectedReport: {},
        },
      };
      const actual = selectSelectedComponent(state);
      expect(actual).not.toBeDefined();
    });
  });

  describe('selectSelectedComponentIndex', () => {
    it('derives selectedComponentIndex from selectedComponent and the selectedReport component list', () => {
      const expected = 1;
      const actual = selectSelectedComponentIndex(mockState);
      expect(actual).toEqual(expected);
    });
  });
});
