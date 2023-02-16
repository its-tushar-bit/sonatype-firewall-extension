/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { sortOwnerByName, fuzzyFilter, flatEntries } from 'MainRoot/OrgsAndPolicies/ownerSideNav/utils';
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
      expect(sortedList).toHaveSize(3);
      const nameOrder = sortedList.map((item) => item.name);
      expect(nameOrder).toEqual(['An organization', 'testing Org 2', 'Testing Org 5']);
    });

    it('returns an empty list if list is empty', () => {
      const sortedList = sortOwnerByName([]);
      expect(sortedList).toEqual([]);
    });
  });

  describe('flatEntries', () => {
    it('flatten the list of organizations and applications', () => {
      const owners = getOwnersMap(4);
      const flatten = flatEntries(owners, { applications: [], organizations: [] });
      expect(flatten.applications.length).toBe(16);
      expect(flatten.organizations.length).toBe(5);
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
});
