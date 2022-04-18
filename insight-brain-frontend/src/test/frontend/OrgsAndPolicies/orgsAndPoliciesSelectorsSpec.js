/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectEntityId,
  selectOrgsAndPoliciesSlice,
  selectOwnerName,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('orgsAndPoliciesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        root: {
          ownerName: 'ownerName',
        },
      },
      router: {
        currentState: {
          name: 'management.view.application',
        },
        currentParams: { organizationId: 'orgId', applicationPublicId: 'alpine-test' },
      },
    };
  });

  describe('selectOrgsAndPoliciesSlice', () => {
    it('selects orgsAndPolicies', () => {
      const appState = {
        orgsAndPolicies: null,
      };

      const selected = selectOrgsAndPoliciesSlice(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectOwnerName', () => {
    it('returns ownerName', () => {
      expect(selectOwnerName(mockState)).toBe('ownerName');
    });
  });

  describe('selectEntityId', () => {
    it('returns app id', () => {
      expect(selectEntityId(mockState)).toBe('alpine-test');
    });

    it('returns org id', () => {
      mockState.router.currentState.name = 'management.view.organization';
      expect(selectEntityId(mockState)).toBe('orgId');
    });
  });
});
