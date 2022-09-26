/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectEntityId,
  selectOrgsAndPoliciesSlice,
  selectSelectedOwner,
  selectSelectedOwnerName,
  selectSelectedOwnerId,
  selectPoliciesByOwner,
  selectRootSlice,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('orgsAndPoliciesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            name: 'ownerName',
            id: 'ownerId',
          },
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

  describe('selectSelectedOwner', () => {
    it('selects selected owner object', () => {
      const selected = selectSelectedOwner(mockState);

      expect(selected).toEqual({ name: 'ownerName', id: 'ownerId' });
    });
  });

  describe('selectSelectedOwnerName', () => {
    it('selects selected owner name property', () => {
      const selected = selectSelectedOwnerName(mockState);

      expect(selected).toBe('ownerName');
    });

    it('selects All Repositories, when is a repositories page', () => {
      mockState.router.currentState.name = 'repositories';
      const selected = selectSelectedOwnerName(mockState);

      expect(selected).toBe('All Repositories');
    });
  });

  describe('selectSelectedOwnerId', () => {
    it('selects selected owner id property', () => {
      const selected = selectSelectedOwnerId(mockState);

      expect(selected).toBe('ownerId');
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

  describe('selectPoliciesByOwner', () => {
    it('is composed from the following selector', () => {
      expect(selectPoliciesByOwner.dependencies).toEqual([selectRootSlice]);
    });

    it('selects policiesByOwner', () => {
      const actualSelection = selectPoliciesByOwner.resultFunc({ policiesByOwner: null });

      expect(actualSelection).toBeNull();
    });
  });
});
