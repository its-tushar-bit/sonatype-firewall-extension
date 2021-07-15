/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectComponentDetailsViolationsSlice,
  selectComponentViolations,
} from '../../../../main/frontend/componentDetails/violations/PolicyViolationsSelectors';

describe('PolicyViolationsSelectors', () => {
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
    },
    componentDetailsPolicyViolations: {
      violations: ['violation1', 'violation2'],
      waivers: ['waiver1'],
      loading: false,
      loadError: 'error during last load',
    },
  };

  describe('selectComponentDetailsViolationsSlice', () => {
    it('selects the componentDetailsViolations slice of the state', () => {
      const expectedSelection = {
        violations: ['violation1', 'violation2'],
        waivers: ['waiver1'],
        loading: false,
        loadError: 'error during last load',
      };

      const actualSelection = selectComponentDetailsViolationsSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectComponentViolations', () => {
    it('returns the violations currently contained in the componentDetailsViolations slice', () => {
      const expectedSelection = ['violation1', 'violation2'];

      const actualSelection = selectComponentViolations(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
