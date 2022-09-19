/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { sortWith, prop } from 'ramda';
import {
  deriveEditRoute,
  deriveViewRoute,
  getOwnerName,
  getActionsOverride,
  sortByThreatLevel,
  policiesComparator,
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

  describe('sortByThreatLevel', () => {
    it('return a sorted list of license threat groups', () => {
      const licenseThreatGroups = [
        {
          id: '542783ebfbc54698962875340a4f805b',
          name: 'Banned',
          threatLevel: 1,
          licenses: [],
        },
        {
          id: '7dea5f29e910404f86d76d32c0a31fdc',
          name: 'Liberal',
          threatLevel: 0,
          licenses: [],
        },
        {
          id: '542783ebfbc54698962875340a4f805b',
          name: 'Banned',
          threatLevel: 10,
          licenses: [],
        },
        {
          id: '7dea5f29e910404f86d76d32c0a31fdc',
          name: 'Liberal2',
          threatLevel: 0,
          licenses: [],
        },
        {
          id: '7c6ad1eeefa848f5ae434464f0132599',
          name: 'Commercial',
          threatLevel: 7,
          licenses: [],
        },
      ];

      const sortedList = sortByThreatLevel(licenseThreatGroups);
      expect(sortedList).toHaveSize(5);
      expect(sortedList[0]).toEqual({
        id: '542783ebfbc54698962875340a4f805b',
        name: 'Banned',
        threatLevel: 10,
        licenses: [],
      });
      expect(sortedList[1]).toEqual({
        id: '7c6ad1eeefa848f5ae434464f0132599',
        name: 'Commercial',
        threatLevel: 7,
        licenses: [],
      });
      expect(sortedList[2]).toEqual({
        id: '542783ebfbc54698962875340a4f805b',
        name: 'Banned',
        threatLevel: 1,
        licenses: [],
      });
      expect(sortedList[3]).toEqual({
        id: '7dea5f29e910404f86d76d32c0a31fdc',
        name: 'Liberal2',
        threatLevel: 0,
        licenses: [],
      });
      expect(sortedList[4]).toEqual({
        id: '7dea5f29e910404f86d76d32c0a31fdc',
        name: 'Liberal',
        threatLevel: 0,
        licenses: [],
      });
    });

    it('Returns an empty list if list is empty', () => {
      const licenseThreatGroups = [];

      const sortedList = sortByThreatLevel(licenseThreatGroups);
      expect(sortedList).toEqual([]);
    });
  });

  describe('policiesComparator', () => {
    let policiesByOwner;

    beforeEach(() => {
      policiesByOwner = [
        {
          inherited: false,
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'organization',
          policies: [
            {
              actions: { build: 'warn', develop: 'warn', operate: 'fail', release: 'fail', source: 'fail' },
              enforcementAction: { build: 'warn', develop: 'warn', operate: 'fail', release: 'fail', source: 'fail' },
              hasLocalActionsOverrides: undefined,
              id: '1f5efb56c0784d7e8c084432ee1f08ac',
              name: 'Root custom',
              threatLevel: 7,
            },
            {
              actions: {},
              enforcementAction: {},
              hasLocalActionsOverrides: undefined,
              id: '7fb4196ab691480ead0f718b6e60a116',
              name: 'License-Commercial',
              threatLevel: 7,
            },
            {
              actions: { proxy: 'fail' },
              enforcementAction: { proxy: 'fail' },
              hasLocalActionsOverrides: undefined,
              id: 'd0b7cb708f554f558b921fbe278bf63c',
              name: 'Security-Namespace Conflict',
              threatLevel: 10,
            },
            {
              actions: { build: 'fail', develop: 'fail' },
              enforcementAction: { build: 'fail', develop: 'fail' },
              hasLocalActionsOverrides: undefined,
              id: '1334f90601014fcda903049dc2789aad',
              name: 'Architecture-Quality',
              threatLevel: 8,
            },
          ],
          policyTags: [],
        },
      ];
    });

    it('sorts policies by "name" key in asc order', () => {
      const key = 'name';
      const customSort = sortWith(policiesComparator(prop(key), key));

      const sorted = customSort(policiesByOwner[0].policies);
      const nameOrder = sorted.map((item) => item.name);

      expect(nameOrder).toEqual([
        'Architecture-Quality',
        'License-Commercial',
        'Root custom',
        'Security-Namespace Conflict',
      ]);
    });

    it('sorts policies by "threatLevel" and "name" keys in asc order if key is "threatLevel"', () => {
      const key = 'threatLevel';
      const customSort = sortWith(policiesComparator(prop(key), key));

      const sorted = customSort(policiesByOwner[0].policies);
      const order = sorted.map((item) => ({ threatLevel: item.threatLevel, name: item.name }));

      expect(order).toEqual([
        {
          threatLevel: 7,
          name: 'License-Commercial',
        },
        {
          threatLevel: 7,
          name: 'Root custom',
        },
        {
          threatLevel: 8,
          name: 'Architecture-Quality',
        },
        {
          threatLevel: 10,
          name: 'Security-Namespace Conflict',
        },
      ]);
    });

    it('sorts policies by stage name key in asc order', () => {
      const key = 'develop';
      const customSort = sortWith(policiesComparator(prop(key), key));

      const sorted = customSort(policiesByOwner[0].policies);
      const nameOrder = sorted.map((item) => item.name);

      expect(nameOrder).toEqual([
        'Architecture-Quality',
        'Root custom',
        'License-Commercial',
        'Security-Namespace Conflict',
      ]);
    });

    it('sorts policies by stage name key in asc order taking in account hasLocalActionsOverrides flag', () => {
      policiesByOwner[0].policies[1] = {
        actions: {},
        enforcementAction: { develop: 'warn' },
        hasLocalActionsOverrides: true,
        id: '7fb4196ab691480ead0f718b6e60a116',
        name: 'License-Commercial',
        threatLevel: 7,
      };
      const key = 'develop';
      const customSort = sortWith(policiesComparator(prop(key), key));

      const sorted = customSort(policiesByOwner[0].policies);
      const nameOrder = sorted.map((item) => item.name);

      expect(nameOrder).toEqual([
        'Architecture-Quality',
        'License-Commercial',
        'Root custom',
        'Security-Namespace Conflict',
      ]);
    });
  });
});
