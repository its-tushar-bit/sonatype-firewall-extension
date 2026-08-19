/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, props } from 'ramda';

import {
  sortItemsByFields,
  sortItemsByFieldsWithNull,
  sortWaiversByComponent,
  sortColumn,
  getColumnDirection,
  defaultComparator,
  sortItemsByFieldsWithComparator,
} from 'MainRoot/util/sortUtils';
import { getComponentName, getComponentNameWithoutVersion } from 'MainRoot/util/componentNameUtils';
import { isWaiverAllVersionsOrExact } from 'MainRoot/util/waiverUtils';

describe('sortUtils specs', function () {
  const input = [
    {
      hash: '1',
      otherHash: '1',
      policyThreatLevel: 9,
      policyName: 'Policy 4',
      waived: true,
      legacyViolation: false,
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
      otherHash: '2',
      policyThreatLevel: 4,
      policyName: 'Policy 2',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'baz',
      derivedComponentName: 'junit.junit.4.8',
    },
    {
      hash: '2',
      otherHash: '2',
      policyThreatLevel: 3,
      policyName: 'Policy 3',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'baz',
      derivedComponentName: 'ant.ant.1.62',
    },
    {
      hash: '1',
      otherHash: '1',
      policyThreatLevel: 4,
      policyName: 'Policy 1',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'bar',
      derivedComponentName: 'junit.junit.4.12',
    },
    {
      hash: '3',
      otherHash: '3',
      policyThreatLevel: 4,
      policyName: 'Policy 5',
      waived: true,
      legacyViolation: false,
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
      otherHash: '1',
      policyThreatLevel: 8,
      policyName: 'Policy 6',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'bar',
      derivedComponentName: 'junit.junit.4.12',
    },
    {
      hash: '1',
      otherHash: null,
      policyThreatLevel: 9,
      policyName: 'Policy 9',
      waived: true,
      legacyViolation: false,
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
      otherHash: '3',
      policyThreatLevel: 5,
      policyName: 'Policy 7',
      waived: true,
      legacyViolation: false,
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
      otherHash: null,
      policyThreatLevel: 4,
      policyName: 'Policy 8',
      waived: true,
      legacyViolation: false,
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
      otherHash: '4',
      policyThreatLevel: 0,
      policyName: 'None',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'apache',
      derivedComponentName: 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29',
    },
    {
      hash: '5',
      otherHash: '5',
      policyThreatLevel: 3,
      policyName: 'Policy 11',
      waived: false,
      legacyViolation: true,
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
      otherHash: '5',
      policyThreatLevel: 2,
      policyName: 'Policy 10',
      waived: false,
      legacyViolation: false,
      componentIdentifier: 'foo',
      derivedComponentName: 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1',
    },
    {
      hash: '5',
      otherHash: '5',
      policyThreatLevel: 5,
      policyName: 'Policy 12',
      waived: false,
      legacyViolation: true,
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
      otherHash: '6',
      policyThreatLevel: 5,
      policyName: 'Policy 12',
      waived: false,
      legacyViolation: true,
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
      otherHash: '7',
      policyThreatLevel: 5,
      policyName: 'Policy 13',
      waived: false,
      legacyViolation: true,
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
      legacyViolation: false,
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

  describe('sortItemsByFieldsWithNull', function () {
    it("sorts by supplied properties (in descending order if prefixed with a '-')", function () {
      const fields = ['-otherHash', 'policyThreatLevel', 'derivedComponentName'];
      const result = sortItemsByFieldsWithNull(fields, input);

      expect(map(props(['otherHash', 'policyThreatLevel', 'derivedComponentName']))(result)).toEqual([
        [null, 4, 'org.springframework.spring-webmvc.4.3.16.RELEASE'],
        [undefined, 6, 'org.postgresql.postgresql.42.2.3'],
        [null, 9, 'junit.junit.4.12'],
        ['7', 5, 'org.postgresql.postgresql.42.2.3'],
        ['6', 5, 'org.postgresql.postgresql.42.2.2'],
        ['5', 2, 'com.fasterxml.jackson.core.jackson-databind.2.8.11.1'],
        ['5', 3, 'com.fasterxml.jackson.core.jackson-annotations.2.8.11.1'],
        ['5', 5, 'ognl.ognl.3.0.8'],
        ['4', 0, 'org.apache.tomcat.embed.tomcat-embed-core.8.5.29'],
        ['3', 4, 'junit.junit.4.12'],
        ['3', 5, 'xpp.xpp3_min.1.1.4c'],
        ['2', 3, 'ant.ant.1.62'],
        ['2', 4, 'junit.junit.4.8'],
        ['1', 4, 'junit.junit.4.12'],
        ['1', 8, 'junit.junit.4.12'],
        ['1', 9, 'junit.junit.4.12'],
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

      expect(sortItemsByFieldsWithNull(['-prop1.propA'], input)).toEqual([
        { prop1: { propA: null } },
        { prop1: { propA: '9' } },
        { prop1: { propA: '8' } },
        { prop1: { propA: '7' } },
        { prop1: { propA: '6' } },
        { prop1: { propA: '5' } },
        { prop1: { propA: '4' } },
        { prop1: { propA: '3' } },
        { prop1: { propA: '2' } },
        { prop1: { propA: '1' } },
      ]);

      expect(sortItemsByFieldsWithNull(['prop1.propA'], input)).toEqual([
        { prop1: { propA: '1' } },
        { prop1: { propA: '2' } },
        { prop1: { propA: '3' } },
        { prop1: { propA: '4' } },
        { prop1: { propA: '5' } },
        { prop1: { propA: '6' } },
        { prop1: { propA: '7' } },
        { prop1: { propA: '8' } },
        { prop1: { propA: '9' } },
        { prop1: { propA: null } },
      ]);
    });

    it('sorts null values to the beginning when sorting descending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: null }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFieldsWithNull(['-foo'], nullSortInput);
      expect(result).toEqual([{ foo: null }, { foo: '4' }, { foo: '3' }, { foo: '2' }, { foo: '1' }]);
    });

    it('sorts undefined values to the beginning when sorting descending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: undefined }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFieldsWithNull(['-foo'], nullSortInput);
      expect(result).toEqual([{ foo: undefined }, { foo: '4' }, { foo: '3' }, { foo: '2' }, { foo: '1' }]);
    });

    it('sorts null values to the end when sorting ascending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: null }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFieldsWithNull(['foo'], nullSortInput);
      expect(result).toEqual([{ foo: '1' }, { foo: '2' }, { foo: '3' }, { foo: '4' }, { foo: null }]);
    });

    it('sorts undefined values to the end when sorting ascending', () => {
      const nullSortInput = [{ foo: '3' }, { foo: '2' }, { foo: undefined }, { foo: '4' }, { foo: '1' }];
      const result = sortItemsByFieldsWithNull(['foo'], nullSortInput);
      expect(result).toEqual([{ foo: '1' }, { foo: '2' }, { foo: '3' }, { foo: '4' }, { foo: undefined }]);
    });

    it('returns the list unchanged if no properties to sort by are supplied', function () {
      const result = sortItemsByFieldsWithNull([], input);
      expect(result).toBe(input);
    });
  });

  describe('sortItemsByFields', function () {
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
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, 'colName', false, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with descending column and returns ascending column, default sort being ascending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, 'colName', true, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with ascending column and returns descending column, default sort being descending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, 'colName', false, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with undefined column and returns default column sorting, default sort being descending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, undefined, undefined, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with undefined column and returns default column sorting, default sort being ascending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, undefined, undefined, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with null column and returns default column sorting, default sort being descending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, null, null, '-colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['-colName']);
    });

    it('calls sort function with null column and returns default column sorting, default sort being ascending', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
      sortColumn(sortFunction, null, null, 'colName');
      expect(sortFunction).toHaveBeenCalledTimes(1);
      expect(sortFunction).toHaveBeenCalledWith(['colName']);
    });

    it('calls sort function with default column over the current column', function () {
      let sortFunction = jest.fn().mockName('sortFunction');
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

  describe('custom comparator', () => {
    const fields = ['value'];
    const data = [{ value: 'e' }, { value: 'c' }, { value: 'B' }, { value: 'D' }, { value: 'a' }];

    it('sorts ignoring case', () => {
      const caseInsensitiveComparator = (a, b) => defaultComparator(a.toLowerCase(), b.toLowerCase());

      const actual = sortItemsByFieldsWithComparator(caseInsensitiveComparator, fields, data);

      expect(actual.map((a) => a.value)).toEqual(['a', 'B', 'c', 'D', 'e']);
    });
  });

  describe('sortWaiversByComponent', () => {
    const getComponentNameFromItem = (item) => {
      return isWaiverAllVersionsOrExact(item)
        ? item.componentMatchStrategy === 'ALL_VERSIONS'
          ? `${getComponentNameWithoutVersion(item)} (all versions)`
          : getComponentName(item)
        : 'allComponents';
    };
    const data = [
      {
        createTime: 1667235892412,
        expiryTime: null,
        componentMatchStrategy: 'ALL_VERSIONS',
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'aopalliance',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'aopalliance',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '1.0',
            },
          ],
          name: 'aopalliance',
        },
        scope: 'Application - app1',
      },
      {
        createTime: 1667236133684,
        expiryTime: 1667883599999,
        componentMatchStrategy: 'ALL_VERSIONS',
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'org.springframework.security',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'spring-security-core',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '3.2.4.RELEASE',
            },
          ],
          name: 'spring-security-core',
        },
        scope: 'Organization - rootorg1',
      },
      {
        createTime: 1667236153015,
        expiryTime: 1668488399999,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: {
          parts: [
            {
              field: 'Group',
              value: 'org.springframework.security',
            },
            {
              value: ' : ',
            },
            {
              field: 'Artifact',
              value: 'spring-security-core',
            },
            {
              value: ' : ',
            },
            {
              field: 'Version',
              value: '3.2.4.RELEASE',
            },
          ],
          name: 'spring-security-core',
        },
        scope: 'Application - app1',
      },
      {
        createTime: 1667235862643,
        expiryTime: 1675054799999,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: {
          parts: [
            {
              field: 'Name',
              value: 'org.webjars angularjs',
            },
            {
              value: ' ',
            },
            {
              field: 'Version',
              value: '1.2.16',
            },
          ],
          name: 'org.webjars angularjs',
        },
        scope: 'Organization - rootorg1',
      },
      {
        createTime: 1667235851125,
        expiryTime: null,
        componentMatchStrategy: 'ALL_VERSIONS',
        displayName: {
          parts: [
            {
              field: 'Name',
              value: 'org.webjars angularjs',
            },
            {
              value: ' ',
            },
            {
              field: 'Version',
              value: '1.2.16',
            },
          ],
          name: 'org.webjars angularjs',
        },
        scope: 'Application - app1',
      },
      {
        createTime: 1667236842975,
        expiryTime: 1669870799999,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: {
          parts: [
            {
              field: 'Name',
              value: 'org.webjars jquery',
            },
            {
              value: ' ',
            },
            {
              field: 'Version',
              value: '1.10.2',
            },
          ],
          name: 'org.webjars jquery',
        },
        scope: 'Application - app1',
      },
      {
        createTime: 1667236885556,
        expiryTime: 1669957200000,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: {
          parts: [
            {
              field: 'Name',
              value: 'org.webjars jquery',
            },
            {
              value: ' ',
            },
            {
              field: 'Version',
              value: '1.10.2',
            },
          ],
          name: 'org.webjars jquery',
        },
        scope: 'Application - app1',
      },
      {
        createTime: 1667235915879,
        expiryTime: 1668488399999,
        componentMatchStrategy: 'ALL_COMPONENTS',
        displayName: null,
        scope: 'Organization - rootorg1',
      },
      {
        createTime: 1667235668371,
        expiryTime: 1675054799999,
        componentMatchStrategy: 'ALL_COMPONENTS',
        displayName: null,
        scope: 'Organization - rootorg1',
      },
      {
        createTime: 1667236066251,
        expiryTime: null,
        componentMatchStrategy: 'ALL_COMPONENTS',
        displayName: null,
        scope: 'Organization - Root Organization',
      },
      {
        createTime: 1667235648269,
        expiryTime: 1677646799999,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: null,
        scope: 'Application - app1',
      },
      {
        createTime: 1667235711117,
        expiryTime: 1677646899999,
        componentMatchStrategy: 'EXACT_COMPONENT',
        displayName: null,
        scope: 'Application - app1',
      },
    ].map((item) => ({
      ...item,
      componentName: getComponentNameFromItem(item), // add this prop in test just for visualizing results
    }));

    it('sorts ascending by component and asc by expiryTime by grouping exact and all version first, all components second and unknown third', () => {
      const actual = sortWaiversByComponent(['component', 'expiryTime'], data);
      expect(map(props(['componentName', 'expiryTime']))(actual)).toEqual([
        ['aopalliance : aopalliance (all versions)', null],
        ['org.springframework.security : spring-security-core (all versions)', 1667883599999],
        ['org.springframework.security : spring-security-core : 3.2.4.RELEASE', 1668488399999],
        ['org.webjars angularjs 1.2.16', 1675054799999],
        ['org.webjars angularjs (all versions)', null],
        ['org.webjars jquery 1.10.2', 1669870799999],
        ['org.webjars jquery 1.10.2', 1669957200000],
        ['allComponents', 1668488399999],
        ['allComponents', 1675054799999],
        ['allComponents', null],
        ['Unknown', 1677646799999],
        ['Unknown', 1677646899999],
      ]);
    });

    it('sorts descending by component and asc by expiryTime by grouping exact and all version first, all components second and unknown third', () => {
      const actual = sortWaiversByComponent(['-component'], data);
      expect(map(props(['componentName', 'expiryTime']))(actual)).toEqual([
        ['org.webjars jquery 1.10.2', 1669870799999],
        ['org.webjars jquery 1.10.2', 1669957200000],
        ['org.webjars angularjs 1.2.16', 1675054799999],
        ['org.webjars angularjs (all versions)', null],
        ['org.springframework.security : spring-security-core (all versions)', 1667883599999],
        ['org.springframework.security : spring-security-core : 3.2.4.RELEASE', 1668488399999],
        ['aopalliance : aopalliance (all versions)', null],
        ['allComponents', 1668488399999],
        ['allComponents', 1675054799999],
        ['allComponents', null],
        ['Unknown', 1677646799999],
        ['Unknown', 1677646899999],
      ]);
    });
  });
});
