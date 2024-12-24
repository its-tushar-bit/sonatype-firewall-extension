/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectAutomatedWaiversRevocationSlice,
  selectRevocations,
} from 'MainRoot/OrgsAndPolicies/automatedWaviersRevocationsSelector';

describe('automatedWaiversRevocationsSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        autoWaiverRevocations: {
          loading: false,
          loadError: null,
          data: null,
          serverData: null,
          isDirty: false,
          submitMaskState: null,
          submitError: null,
          deleteRevocationSubmitMaskState: null,
          deleteRevocationSubmitError: null,
        },
      },
    };
  });

  describe('automatedWaiversRevocationsSlice', () => {
    it('selects autoWaiverRevocations', () => {
      const emptyAppState = {
        orgsAndPolicies: {
          autoWaiverRevocations: null,
        },
      };

      const selected = selectAutomatedWaiversRevocationSlice(mockState);

      expect(selected).toEqual({
        loading: false,
        loadError: null,
        data: null,
        serverData: null,
        isDirty: false,
        submitMaskState: null,
        submitError: null,
        deleteRevocationSubmitMaskState: null,
        deleteRevocationSubmitError: null,
      });

      const emptySelected = selectAutomatedWaiversRevocationSlice(emptyAppState);
      expect(emptySelected).toEqual(null);
    });

    it('select revocations', () => {
      mockState = {
        orgsAndPolicies: {
          autoWaiverRevocations: {
            loading: false,
            loadError: null,
            data: {
              revocations: [
                {
                  autoPolicyWaiverId: 1,
                  autoPolicyWaiverRevocationId: 'revocation1',
                  createTime: '2021-01-01',
                  threatLevel: 7,
                  policyName: 'policy1',
                  componentDisplayName: 'component1',
                  vulnerabilityIdentifiers: 'vulnerability1',
                },
                {
                  autoPolicyWaiverId: 2,
                  autoPolicyWaiverRevocationId: 'revocation2',
                  createTime: '2021-01-02',
                  threatLevel: 8,
                  policyName: 'policy2',
                  componentDisplayName: 'component2',
                  vulnerabilityIdentifiers: 'vulnerability2',
                },
              ],
            },
            serverData: null,
            isDirty: false,
            submitMaskState: null,
            submitError: null,
            deleteRevocationSubmitMaskState: null,
            deleteRevocationSubmitError: null,
          },
        },
      };

      selectAutomatedWaiversRevocationSlice(mockState);

      const revocations = selectRevocations(mockState);
      expect(revocations).toEqual({
        revocations: [
          {
            autoPolicyWaiverId: 1,
            autoPolicyWaiverRevocationId: 'revocation1',
            createTime: '2021-01-01',
            threatLevel: 7,
            policyName: 'policy1',
            componentDisplayName: 'component1',
            vulnerabilityIdentifiers: 'vulnerability1',
          },
          {
            autoPolicyWaiverId: 2,
            autoPolicyWaiverRevocationId: 'revocation2',
            createTime: '2021-01-02',
            threatLevel: 8,
            policyName: 'policy2',
            componentDisplayName: 'component2',
            vulnerabilityIdentifiers: 'vulnerability2',
          },
        ],
      });
    });
  });
});
