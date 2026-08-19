/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { sortOwnerByName, fuzzyFilter, flatEntries, getOwnerInfo } from 'MainRoot/OrgsAndPolicies/ownerSideNav/utils';
import { getOwnersMap } from './nLevelMockData';

describe('ownerSideNav utils', () => {
  describe('sortOwnerByName', () => {
    it('returns a sorted list of owners', () => {
      const owners = [
        {
          id: '1',
          name: 'Testing Org 5',
        },
        {
          id: '2',
          name: 'testing Org 2',
        },
        {
          id: '3',
          name: 'An organization',
        },
      ];
      const sortedList = sortOwnerByName(owners);
      expect(sortedList).toHaveLength(3);
      const nameOrder = sortedList.map((item) => item.name);
      expect(nameOrder).toEqual(['An organization', 'testing Org 2', 'Testing Org 5']);
    });

    it('returns an empty list if list is empty', () => {
      const sortedList = sortOwnerByName([]);
      expect(sortedList).toEqual([]);
    });
  });

  describe('flatEntries', () => {
    it('flatten the list of organizations, applications, repo managers and repositories', () => {
      const owners = getOwnersMap(4, false);
      const rootOrg = owners.ROOT_ORGANIZATION_ID;
      const repositoryManagerIds = ['repositoryManagerOne', 'repositoryManagerTwo'];
      const ownersMapWithRepositoryContainer = {
        ...owners,
        ROOT_ORGANIZATION_ID: rootOrg,
        REPOSITORY_CONTAINER_ID: { repositoryManagerIds },
        repositoryManagerOne: { id: 'repositoryManagerOne', name: 'repositoryManagerOne', type: 'repository_manager' },
        repositoryManagerTwo: { id: 'repositoryManagerTwo', name: 'repositoryManagerTwo', type: 'repository_manager' },
        repositoryOne: { id: 'repositoryOne', name: 'repositoryOne', type: 'repository' },
        repositoryTwo: { id: 'repositoryTwo', name: 'repositoryTwo', type: 'repository' },
        repositoryThree: { id: 'repositoryThree', name: 'repositoryThree', type: 'repository' },
      };
      const flatten = flatEntries(ownersMapWithRepositoryContainer, {
        applications: [],
        organizations: [],
        repositories: [],
        repositoryManagers: [],
      });
      expect(flatten.applications.length).toBe(16);
      expect(flatten.organizations.length).toBe(5);
      expect(flatten.repositoryManagers.length).toBe(2);
      expect(flatten.repositories.length).toBe(3);
    });
  });

  describe('fuzzyFilter', () => {
    it('ignores case', () => {
      expect(fuzzyFilter([{ name: 'FOO' }], 'foo', 'name')).toEqual([{ name: 'FOO' }]);
    });

    it('is fuzzy', () => {
      expect(fuzzyFilter([{ name: 'test-name' }], 'test', 'name')).toEqual([{ name: 'test-name' }]);
      expect(fuzzyFilter([{ name: 'app-test-server' }], 'test', 'name')).toEqual([{ name: 'app-test-server' }]);
      expect(fuzzyFilter([{ name: 'quality-test' }], 'test', 'name')).toEqual([{ name: 'quality-test' }]);
      expect(fuzzyFilter([{ name: 'testing-app' }], 'testung-app', 'name')).toEqual([{ name: 'testing-app' }]);
    });

    it('is not too fuzzy', () => {
      expect(fuzzyFilter([{ name: 'pi-parent' }], 'iq-server', 'name')).toEqual([]);
      expect(fuzzyFilter([{ name: 'Bamboo Plugin' }], 'foo', 'name')).toEqual([]);
      expect(fuzzyFilter([{ name: 'mongobee' }], 'bingo', 'name')).toEqual([]);
      expect(fuzzyFilter([{ name: 'zamarchive-webapp' }], 'adma', 'name')).toEqual([]);
    });

    it('optionally extracts identifier property of results', () => {
      expect(
        fuzzyFilter(
          [
            { id: 'id-1', name: 'name-1' },
            { id: 'id-2', name: 'name-2' },
            { id: 'id-3', name: 'other' },
          ],
          'name',
          'name',
          'id'
        )
      ).toEqual(['id-1', 'id-2']);
    });
  });

  describe('getOwnerInfo', () => {
    it('flatten the list of organizations, applications and repo managers', () => {
      expect(getOwnerInfo({ type: 'repository', id: 'rid' })).toEqual(['parentId', { repositoryId: 'rid' }]);
      expect(getOwnerInfo({ type: 'repository_manager', id: 'rmid' })).toEqual([
        'parentId',
        { repositoryManagerId: 'rmid' },
      ]);
      expect(getOwnerInfo({ type: 'repository_container', id: 'rcid' })).toEqual([
        'parentId',
        { repositoryContainerId: 'rcid' },
      ]);
      expect(getOwnerInfo({ type: 'organization', id: 'oid' })).toEqual([
        'parentOrganizationId',
        { organizationId: 'oid' },
      ]);
    });
  });
});
