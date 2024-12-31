/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectAutomatedWaiversExclusionSlice,
  selectExclusions,
} from 'MainRoot/OrgsAndPolicies/automatedWaiversExclusionsSelector';

describe('automatedWaiversExclusionsSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        autoWaiverExclusions: {
          loading: false,
          loadError: null,
          data: null,
          serverData: null,
          isDirty: false,
          submitMaskState: null,
          submitError: null,
          deleteExclusionSubmitMaskState: null,
          deleteExclusionSubmitError: null,
        },
      },
    };
  });

  describe('automatedWaiversExclusionsSlice', () => {
    it('selects autoWaiverExclusions', () => {
      const emptyAppState = {
        orgsAndPolicies: {
          autoWaiverExclusions: null,
        },
      };

      const selected = selectAutomatedWaiversExclusionSlice(mockState);

      expect(selected).toEqual({
        loading: false,
        loadError: null,
        data: null,
        serverData: null,
        isDirty: false,
        submitMaskState: null,
        submitError: null,
        deleteExclusionSubmitMaskState: null,
        deleteExclusionSubmitError: null,
      });

      const emptySelected = selectAutomatedWaiversExclusionSlice(emptyAppState);
      expect(emptySelected).toEqual(null);
    });

    it('select exclusions', () => {
      mockState = {
        orgsAndPolicies: {
          autoWaiverExclusions: {
            loading: false,
            loadError: null,
            data: {
              exclusions: [
                {
                  autoPolicyWaiverId: 1,
                  autoPolicyWaiverExclusionId: 'exclusion1',
                  createTime: '2021-01-01',
                  threatLevel: 7,
                  policyName: 'policy1',
                  componentDisplayName: 'component1',
                  vulnerabilityIdentifiers: 'vulnerability1',
                },
                {
                  autoPolicyWaiverId: 2,
                  autoPolicyWaiverExclusionId: 'exclusion2',
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
            deleteExclusionSubmitMaskState: null,
            deleteExclusionSubmitError: null,
          },
        },
      };

      selectAutomatedWaiversExclusionSlice(mockState);

      const exclusions = selectExclusions(mockState);
      expect(exclusions).toEqual({
        exclusions: [
          {
            autoPolicyWaiverId: 1,
            autoPolicyWaiverExclusionId: 'exclusion1',
            createTime: '2021-01-01',
            threatLevel: 7,
            policyName: 'policy1',
            componentDisplayName: 'component1',
            vulnerabilityIdentifiers: 'vulnerability1',
          },
          {
            autoPolicyWaiverId: 2,
            autoPolicyWaiverExclusionId: 'exclusion2',
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
