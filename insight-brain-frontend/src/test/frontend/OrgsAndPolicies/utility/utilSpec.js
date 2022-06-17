/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  deriveEditRoute,
  deriveViewRoute,
  getOwnerName,
  getActionsOverride,
} from 'MainRoot/OrgsAndPolicies/utility/util';

describe('OrgsAndPolicies util', () => {
  describe('route derivation util', () => {
    let router;

    beforeEach(() => {
      router = {
        currentState: {
          name: 'management.view.organization',
        },
        currentParams: {
          organizationId: '123',
        },
      };
    });

    it('derives edit state with only to value provided', () => {
      const toMock = 'create-label';
      const actual = deriveEditRoute(router, toMock);

      expect(actual.to).toEqual(`management.edit.organization.${toMock}`);
      expect(actual.params).toEqual(router.currentParams);
    });

    it('derives edit state with to and params values provided', () => {
      const toMock = 'create-label';
      const paramsMock = { labelId: 'foo' };

      const actual = deriveEditRoute(router, toMock, paramsMock);

      expect(actual.to).toEqual(`management.edit.organization.${toMock}`);
      expect(actual.params).toEqual({ organizationId: '123', labelId: 'foo' });
    });

    it('derives view state with no options provided', () => {
      router.currentState = { name: 'management.edit.organization' };

      const actual = deriveViewRoute(router);

      expect(actual.to).toEqual('management.view.organization');
      expect(actual.params).toEqual(router.currentParams);
    });

    it('derives route with empty string as an input', () => {
      const actual = deriveEditRoute(router, '');

      expect(actual.to).toEqual('management.edit.organization');
      expect(actual.params).toEqual(router.currentParams);
    });

    it('derives route with no input', () => {
      const actual = deriveEditRoute(router);

      expect(actual.to).toEqual('management.edit.organization');
      expect(actual.params).toEqual(router.currentParams);
    });
  });

  describe('getOwnerName', () => {
    it('gets the owner Name', () => {
      const owners = [
        { publicId: 'owner1', name: 'owner 1' },
        { publicId: 'owner2', name: 'owner 2' },
      ];
      const ownerName = getOwnerName('owner1')(owners);

      expect(ownerName).toBe('owner 1');
    });
  });

  describe('getActionsOverride', () => {
    let ownerHierarchyIds, policy;

    beforeEach(() => {
      ownerHierarchyIds = ['appId', 'parentOrgId', 'grandParentOrgId', 'rootOrgId'];
      policy = {
        ownerId: 'grandParentOrgId',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: {
          someOtherOrg: { build: 'warn' },
        },
      };
    });

    it('returns null if policyActionsOverrideAllowed is false', () => {
      policy.policyActionsOverrideAllowed = false;
      expect(getActionsOverride(ownerHierarchyIds, policy)).toBe(null);
    });

    it('returns null if policyActionsOverrides is null', () => {
      policy.policyActionsOverrides = null;
      expect(getActionsOverride(ownerHierarchyIds, policy)).toBe(null);
    });

    it('returns null if there are no overrides for given hierarchy', () => {
      expect(getActionsOverride(ownerHierarchyIds, policy)).toBe(null);
    });

    it('returns null if the override is for the parent of the policy owner', () => {
      policy.policyActionsOverrides.rootOrgId = { build: 'warn' };
      expect(getActionsOverride(ownerHierarchyIds, policy)).toBe(null);
    });

    it('returns null if the override is for the policy owner', () => {
      policy.policyActionsOverrides.grandParentOrgId = { build: 'warn' };
      expect(getActionsOverride(ownerHierarchyIds, policy)).toBe(null);
    });

    it('returns override for parent owner, which is the child of the policy owner', () => {
      policy.policyActionsOverrides.parentOrgId = { build: 'warn' };
      expect(getActionsOverride(ownerHierarchyIds, policy)).toEqual({
        actionsOverride: { build: 'warn' },
        isCurrentOwnerOverride: false,
      });
    });

    it('returns override for current owner', () => {
      policy.policyActionsOverrides.appId = { build: 'warn', release: 'warn' };
      expect(getActionsOverride(ownerHierarchyIds, policy)).toEqual({
        actionsOverride: { build: 'warn', release: 'warn' },
        isCurrentOwnerOverride: true,
      });
    });

    it('ignores an override for the parent owner if there is an override for current owner', () => {
      policy.policyActionsOverrides.parentOrgId = { 'stage-release': 'fail' };
      policy.policyActionsOverrides.appId = { build: 'warn', release: 'warn' };
      expect(getActionsOverride(ownerHierarchyIds, policy)).toEqual({
        actionsOverride: { build: 'warn', release: 'warn' },
        isCurrentOwnerOverride: true,
      });
    });
  });
});
