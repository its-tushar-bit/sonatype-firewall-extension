/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, props } from 'ramda';

import { sortItemsByFields, sortColumn, getColumnDirection } from '../../../main/frontend/util/sortUtils';

describe('sortUtils specs', function () {
  describe('sortItemsByFields', function () {
    const input = [
      {
        hash: '1',
        policyThreatLevel: 9,
        policyName: 'Policy 4',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'bar',
        displayName: {
          parts: [
            { field: 'Group', value: 'junit' },
            { value: ' : ' },
            { field: 'Artifact', value: 'junit' },
            { value: ' : ' },
            { field: 'Version', value: '4.12' },
          ],
        },
        derivedComponentName: 'junit.junit.4.12',
      },
      {
        hash: '2',
        policyThreatLevel: 4,
        policyName: 'Policy 2',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'baz',
        derivedComponentName: 'junit.junit.4.8',
      },
      {
        hash: '2',
        policyThreatLevel: 3,
        policyName: 'Policy 3',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'baz',
        derivedComponentName: 'ant.ant.1.62',
      },
      {
        hash: '1',
        policyThreatLevel: 4,
        policyName: 'Policy 1',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'bar',
        derivedComponentName: 'junit.junit.4.12',
      },
      {
        hash: '3',
        policyThreatLevel: 4,
        policyName: 'Policy 5',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            { field: 'Group', value: 'junit' },
            { value: ' : ' },
            { field: 'Artifact', value: 'junit' },
            { value: ' : ' },
            { field: 'Version', value: '4.12' },
          ],
        },
        derivedComponentName: 'junit.junit.4.12',
      },
      {
        hash: '1',
        policyThreatLevel: 8,
        policyName: 'Policy 6',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'bar',
        derivedComponentName: 'junit.junit.4.12',
      },
      {
        hash: '1',
        policyThreatLevel: 9,
        policyName: 'Policy 9',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'bar',
        displayName: {
          parts: [
            { field: 'Group', value: 'junit' },
            { value: ' : ' },
            { field: 'Artifact', value: 'junit' },
            { value: ' : ' },
            { field: 'Version', value: '4.12' },
          ],
        },
        derivedComponentName: 'junit.junit.4.12',
      },
      {
        hash: '3',
        policyThreatLevel: 5,
        policyName: 'Policy 7',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            { field: 'Group', value: 'xpp' },
            { value: ' : ' },
            { field: 'Artifact', value: 'xpp3_min' },
            { value: ' : ' },
            { field: 'Version', value: '1.1.4c' },
          ],
        },
        derivedComponentName: 'xpp.xpp3_min.1.1.4c',
      },
      {
        hash: '3',
        policyThreatLevel: 4,
        policyName: 'Policy 8',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'qux',
        displayName: {
          parts: [
            { field: 'Group', value: 'org.springframework' },
            { value: ' : ' },
            { field: 'Artifact', value: 'spring-webmvc' },
            { value: ' : ' },
            { field: 'Version', value: '4.3.16.RELEASE' },
          ],
        },
        derivedComponentName: 'org.springframework.spring-webmvc.4.3.16.RELEASE',
      },
      {
        hash: '4',
        policyThreatLevel: 0,
        policyName: 'None',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'apache',
        derivedComponentName: 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29',
      },
      {
        hash: '5',
        policyThreatLevel: 3,
        policyName: 'Policy 11',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo',
        displayName: {
          parts: [
            { field: 'Group', value: 'com.fasterxml' },
            { value: ' : ' },
            { field: 'Artifact', value: 'jackson.core.jackson-annotations' },
            { value: ' : ' },
            { field: 'Version', value: '2.8.11.1' },
          ],
        },
        derivedComponentName: 'com.fasterxml.jackson.core.jackson-annotations.2.8.11.1',
      },
      {
        hash: '5',
        policyThreatLevel: 2,
        policyName: 'Policy 10',
        waived: false,
        grandfathered: false,
        componentIdentifier: 'foo',
        derivedComponentName: 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1',
      },
      {
        hash: '5',
        policyThreatLevel: 5,
        policyName: 'Policy 12',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo',
        displayName: {
          parts: [
            { field: 'Group', value: 'ognl' },
            { value: ' : ' },
            { field: 'Artifact', value: 'ognl' },
            { value: ' : ' },
            { field: 'Version', value: '3.0.8' },
          ],
        },
        derivedComponentName: 'ognl.ognl.3.0.8',
      },
      {
        hash: '6',
        policyThreatLevel: 5,
        policyName: 'Policy 12',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo2',
        displayName: {
          parts: [
            { field: 'Group', value: 'org.postgresql' },
            { value: ' : ' },
            { field: 'Artifact', value: 'postgresql' },
            { value: ' : ' },
            { field: 'Version', value: '42.2.2' },
          ],
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.2',
      },
      {
        hash: '7',
        policyThreatLevel: 5,
        policyName: 'Policy 13',
        waived: false,
        grandfathered: true,
        componentIdentifier: 'foo3',
        displayName: {
          parts: [
            { field: 'Group', value: 'org.postgresql' },
            { value: ' : ' },
            { field: 'Artifact', value: 'postgresql' },
            { value: ' : ' },
            { field: 'Version', value: '42.2.3' },
          ],
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.3',
      },
      {
        hash: '7',
        policyThreatLevel: 6,
        policyName: 'Policy 14',
        waived: true,
        grandfathered: false,
        componentIdentifier: 'foo3',
        displayName: {
          parts: [
            { field: 'Group', value: 'org.postgresql' },
            { value: ' : ' },
            { field: 'Artifact', value: 'postgresql' },
            { value: ' : ' },
            { field: 'Version', value: '42.2.3' },
          ],
        },
        derivedComponentName: 'org.postgresql.postgresql.42.2.3',
      },
    ];

    it("sorts by supplied properties (in descending order if prefixed with a '-')", function () {
      const fields = ['-policyThreatLevel', 'policyName', 'derivedComponentName'];
      const result = sortItemsByFields(fields, input);
      expect(map(props(['policyThreatLevel', 'policyName', 'derivedComponentName']))(result)).toEqual([
        [9, 'Policy 4', 'junit.junit.4.12'],
        [9, 'Policy 9', 'junit.junit.4.12'],
        [8, 'Policy 6', 'junit.junit.4.12'],
        [6, 'Policy 14', 'org.postgresql.postgresql.42.2.3'],
        [5, 'Policy 12', 'ognl.ognl.3.0.8'],
        [5, 'Policy 12', 'org.postgresql.postgresql.42.2.2'],
        [5, 'Policy 13', 'org.postgresql.postgresql.42.2.3'],
        [5, 'Policy 7', 'xpp.xpp3_min.1.1.4c'],
        [4, 'Policy 1', 'junit.junit.4.12'],
        [4, 'Policy 2', 'junit.junit.4.8'],
        [4, 'Policy 5', 'junit.junit.4.12'],
        [4, 'Policy 8', 'org.springframework.spring-webmvc.4.3.16.RELEASE'],
        [3, 'Policy 11', 'com.fasterxml.jackson.core.jackson-annotations.2.8.11.1'],
        [3, 'Policy 3', 'ant.ant.1.62'],
        [2, 'Policy 10', 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1'],
        [0, 'None', 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29'],
      ]);
    });

    it('understands nested field names using the dot notation', function () {
      const input = [
        { prop1: { propA: '1' } },
        { prop1: { propA: '2' } },
        { prop1: { propA: '3' } },
        { prop1: { propA: '4' } },
        { prop1: { propA: '5' } },
        { prop1: { propA: null } },
        { prop1: { propA: '6' } },
        { prop1: { propA: '7' } },
        { prop1: { propA: '8' } },
        { prop1: { propA: '9' } },
      ];

      expect(sortItemsByFields(['-prop1.propA'], input)).toEqual([
        { prop1: { propA: '9' } },
        { prop1: { propA: '8' } },
        { prop1: { propA: '7' } },
        { prop1: { propA: '6' } },
        { prop1: { propA: '5' } },
        { prop1: { propA: '4' } },
        { prop1: { propA: '3' } },
        { prop1: { propA: '2' } },
        { prop1: { propA: '1' } },
        { prop1: { propA: null } },
      ]);

      expect(sortItemsByFields(['prop1.propA'], input)).toEqual([
        { prop1: { propA: null } },
        { prop1: { propA: '1' } },
        { prop1: { propA: '2' } },
        { prop1: { propA: '3' } },
        { prop1: { propA: '4' } },
        { prop1: { propA: '5' } },
        { prop1: { propA: '6' } },
        { prop1: { propA: '7' } },
        { prop1: { propA: '8' } },
        { prop1: { propA: '9' } },
      ]);
    });

    it('sorts null values to the end when sorting descending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: null }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFields(['-foo'], nullSortInput);
      expect(result).toEqual([{ foo: '4' }, { foo: '3' }, { foo: '2' }, { foo: '1' }, { foo: null }]);
    });

    it('sorts undefined values to the end when sorting descending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: undefined }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFields(['-foo'], nullSortInput);
      expect(result).toEqual([{ foo: '4' }, { foo: '3' }, { foo: '2' }, { foo: '1' }, { foo: undefined }]);
    });

    it('sorts null values to the beginning when sorting ascending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: null }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFields(['foo'], nullSortInput);
      expect(result).toEqual([{ foo: null }, { foo: '1' }, { foo: '2' }, { foo: '3' }, { foo: '4' }]);
    });

    it('sorts undefined values to the beginning when sorting ascending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: undefined }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFields(['foo'], nullSortInput);
      expect(result).toEqual([{ foo: undefined }, { foo: '1' }, { foo: '2' }, { foo: '3' }, { foo: '4' }]);
    });

    it('returns the list unchanged if no properties to sort by are supplied', function () {
      const result = sortItemsByFields([], input);
      expect(result).toBe(input);
    });
  });

  describe('sortColumn', function () {
    it('calls sort function with descending column when passed column is ascending, default sort being ascending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, 'colName', false, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with descending column and returns ascending column, default sort being ascending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, 'colName', true, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with ascending column and returns descending column, default sort being descending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, 'colName', false, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with undefined column and returns default column sorting, default sort being descending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, undefined, undefined, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with undefined column and returns default column sorting, default sort being ascending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, undefined, undefined, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with null column and returns default column sorting, default sort being descending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, null, null, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with null column and returns default column sorting, default sort being ascending', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, null, null, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with default column over the current column', function () {
      let sortFunction = jasmine.createSpy('sortFunction');
      sortColumn(sortFunction, 'foobar', false, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });
  });

  describe('getColumnDirection', function () {
    it('returns column sort direction as asc for a sorted column in ascending order', function () {
      let sortDir = getColumnDirection('colName', false, 'colName');
      expect(sortDir).toEqual('asc');
    });

    it('returns column sort direction as desc for a sorted column in descending order', function () {
      let sortDir = getColumnDirection('colName', true, 'colName');
      expect(sortDir).toEqual('desc');
    });

    it('returns column sort direction as null for an unsorted (undefined) column', function () {
      let sortDir = getColumnDirection(undefined, undefined, 'colName');
      expect(sortDir).toEqual(null);
    });

    it('returns column sort direction as null for an unsorted (null) column', function () {
      let sortDir = getColumnDirection(null, null, 'colName');
      expect(sortDir).toEqual(null);
    });

    it('returns null if column name does not match current column', function () {
      let sortDir = getColumnDirection('colName', false, 'foobar');
      expect(sortDir).toEqual(null);
    });
  });
});
