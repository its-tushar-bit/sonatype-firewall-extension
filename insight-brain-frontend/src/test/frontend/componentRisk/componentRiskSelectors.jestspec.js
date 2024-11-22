/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectComponentRisk } from 'MainRoot/dashboard/results/componentRisk/componentRiskSelectors';

describe('componentRiskSelectors', () => {
  const componentRiskDetails = {
    applicationComponents: [],
    component: {},
    componentName: 'my-component-name',
    loading: true,
    loadError: 'Error',
    totalRisk: 10,
  };

  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
      },
    },
    componentRiskDetails,
  };

  describe('selectComponentRisk', () => {
    it('returns component risk detail', () => {
      const actual = selectComponentRisk(mockState);
      expect(actual).toEqual(componentRiskDetails);
    });
  });
});
