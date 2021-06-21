/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolations from '../../../../main/frontend/componentDetails/violations/PolicyViolations';
import PolicyViolationsTable from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTable';

describe('PolicyViolations', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      violations: [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
      ],
      componentDetails: { hash: 'hash' },
      loadComponentDetails: jasmine.createSpy('loadComponentDetails'),
      loadError: null,
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolations, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolations, minimalProps);
  });

  describe('loadComponentDetails action', () => {
    it('Does not call loadComponentDetails if the componentDetails are loaded', () => {
      getMounted();
      expect(minimalProps.loadComponentDetails).not.toHaveBeenCalled();
    });

    it('calls loadComponentDetails if no componentDetails are loaded yet', () => {
      getMounted({ componentDetails: undefined });
      expect(minimalProps.loadComponentDetails).toHaveBeenCalled();
    });
  });

  describe('renders a PolicyViolationsTable', () => {
    it('renders an PolicyViolationsTable component passing the appropriate props', () => {
      let violationsTable;
      violationsTable = getShallow().find(PolicyViolationsTable);

      expect(violationsTable).toExist();
      expect(violationsTable).toHaveProp('violations', minimalProps.violations);
      expect(violationsTable).toHaveProp('loading', false);
      expect(violationsTable).toHaveProp('error', null);
      expect(violationsTable).toHaveProp('retryHandler', minimalProps.loadComponentDetails);

      violationsTable = getShallow({ loadError: null, violations: undefined }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('loading', true);

      violationsTable = getShallow({ loadError: 'some error' }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('error', 'some error');
    });

    it('orders the violations by policyThreatLevel to pass them to the table', () => {
      const originViolations = [
        {
          policyViolationId: 'policyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
        },
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
        },
        {
          policyViolationId: 'policyViolationId7',
          policyThreatLevel: 7,
          policyName: 'Security-Critical',
        },
      ];

      const expectedViolationsInOrder = [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
        },
        {
          policyViolationId: 'policyViolationId7',
          policyThreatLevel: 7,
          policyName: 'Security-Critical',
        },
        {
          policyViolationId: 'policyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
        },
      ];

      const violationsTable = getShallow({ violations: originViolations }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('violations', expectedViolationsInOrder);
    });
  });
});
