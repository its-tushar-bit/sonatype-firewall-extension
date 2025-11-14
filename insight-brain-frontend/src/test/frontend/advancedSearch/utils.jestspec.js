/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildSearchQuery } from 'MainRoot/advancedSearch/utils';

describe('buildSearchQuery', () => {
  it('returns empty string for empty array', () => {
    expect(buildSearchQuery([])).toBe('');
  });

  it('throws error for null or undefined input', () => {
    expect(() => buildSearchQuery(null)).toThrow();
    expect(() => buildSearchQuery(undefined)).toThrow();
  });

  it('skips items with missing field value', () => {
    const searchItems = [
      { field: { value: '' }, value: 'test', operator: 'OR', isExactMatch: false },
      { field: { value: 'componentName' }, value: 'test', operator: 'OR', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('OR componentName:*test* ');
  });

  it('skips items with missing search value', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: '', operator: 'OR', isExactMatch: false },
      { field: { value: 'applicationName' }, value: 'test', operator: 'OR', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('OR applicationName:*test* ');
  });

  it('builds query for single item without operator', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test-component', operator: 'OR', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test-component* ');
  });

  it('builds query for single item with exact match', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test-component', operator: 'OR', isExactMatch: true },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:"test-component" ');
  });

  it('builds query for multiple items with operators', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test-component', operator: 'OR', isExactMatch: false },
      { field: { value: 'applicationName' }, value: 'test-app', operator: 'AND', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test-component* AND applicationName:*test-app* ');
  });

  it('builds query for multiple items with mixed exact and partial matches', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test-component', operator: 'OR', isExactMatch: true },
      { field: { value: 'applicationName' }, value: 'test-app', operator: 'AND', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:"test-component" AND applicationName:*test-app* ');
  });

  it('handles items with prefixList', () => {
    const searchItems = [
      {
        field: {
          value: 'component',
          prefixList: [
            { value: 'componentName', label: 'Component Name' },
            { value: 'componentVersion', label: 'Component Version' },
          ],
        },
        value: 'test',
        operator: 'OR',
        isExactMatch: false,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test* componentVersion:*test* ');
  });

  it('handles items with prefixList and multiple search items', () => {
    const searchItems = [
      {
        field: {
          value: 'component',
          prefixList: [
            { value: 'componentName', label: 'Component Name' },
            { value: 'componentVersion', label: 'Component Version' },
          ],
        },
        value: 'test',
        operator: 'OR',
        isExactMatch: false,
      },
      {
        field: { value: 'applicationName' },
        value: 'app',
        operator: 'AND',
        isExactMatch: true,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe(
      'componentName:*test* componentVersion:*test* AND applicationName:"app" '
    );
  });

  it('handles items with prefixList and exact match', () => {
    const searchItems = [
      {
        field: {
          value: 'component',
          prefixList: [
            { value: 'componentName', label: 'Component Name' },
            { value: 'componentVersion', label: 'Component Version' },
          ],
        },
        value: 'test',
        operator: 'OR',
        isExactMatch: true,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:"test" componentVersion:"test" ');
  });

  it('handles complex real-world scenario', () => {
    const searchItems = [
      {
        field: { value: 'componentName' },
        value: 'log4j',
        operator: 'OR',
        isExactMatch: false,
      },
      {
        field: { value: 'vulnerabilityId' },
        value: 'CVE-2021-44228',
        operator: 'AND',
        isExactMatch: true,
      },
      {
        field: {
          value: 'application',
          prefixList: [
            { value: 'applicationName', label: 'Application Name' },
            { value: 'applicationVersion', label: 'Application Version' },
          ],
        },
        value: 'my-app',
        operator: 'OR',
        isExactMatch: false,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe(
      'componentName:*log4j* AND vulnerabilityId:"CVE-2021-44228" OR applicationName:*my-app* applicationVersion:*my-app* '
    );
  });

  it('handles special characters in values', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test@component', operator: 'OR', isExactMatch: false },
      { field: { value: 'applicationName' }, value: 'my-app-v1.0', operator: 'AND', isExactMatch: true },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test@component* AND applicationName:"my-app-v1.0" ');
  });

  it('handles empty prefixList', () => {
    const searchItems = [
      {
        field: {
          value: 'component',
          prefixList: [],
        },
        value: 'test',
        operator: 'OR',
        isExactMatch: false,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe('');
  });

  it('throws error for null field properties', () => {
    const searchItems = [
      { field: null, value: 'test', operator: 'OR', isExactMatch: false },
      { field: { value: 'componentName' }, value: 'test', operator: 'OR', isExactMatch: false },
    ];
    expect(() => buildSearchQuery(searchItems)).toThrow();
  });

  it('handles missing operator property', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'test1', isExactMatch: false },
      { field: { value: 'applicationName' }, value: 'test2', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test1* undefined applicationName:*test2* ');
  });

  it('handles missing isExactMatch property (defaults to partial match)', () => {
    const searchItems = [{ field: { value: 'componentName' }, value: 'test', operator: 'OR' }];
    expect(buildSearchQuery(searchItems)).toBe('componentName:*test* ');
  });

  it('handles items with only field value but no search value', () => {
    const searchItems = [{ field: { value: 'componentName' }, value: '', operator: 'OR', isExactMatch: false }];
    expect(buildSearchQuery(searchItems)).toBe('');
  });

  it('handles items with only search value but no field value', () => {
    const searchItems = [{ field: { value: '' }, value: 'test', operator: 'OR', isExactMatch: false }];
    expect(buildSearchQuery(searchItems)).toBe('');
  });

  it('handles items with both field and search value missing', () => {
    const searchItems = [
      { field: { value: '' }, value: '', operator: 'OR', isExactMatch: false },
      { field: { value: 'componentName' }, value: 'test', operator: 'OR', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe('OR componentName:*test* ');
  });

  it('handles single item with prefixList and exact match', () => {
    const searchItems = [
      {
        field: {
          value: 'component',
          prefixList: [{ value: 'componentName', label: 'Component Name' }],
        },
        value: 'exact-match',
        operator: 'OR',
        isExactMatch: true,
      },
    ];
    expect(buildSearchQuery(searchItems)).toBe('componentName:"exact-match" ');
  });

  it('handles multiple items with different operators', () => {
    const searchItems = [
      { field: { value: 'componentName' }, value: 'log4j', operator: 'OR', isExactMatch: false },
      { field: { value: 'vulnerabilityId' }, value: 'CVE-2021', operator: 'AND', isExactMatch: true },
      { field: { value: 'applicationName' }, value: 'my-app', operator: 'OR', isExactMatch: false },
    ];
    expect(buildSearchQuery(searchItems)).toBe(
      'componentName:*log4j* AND vulnerabilityId:"CVE-2021" OR applicationName:*my-app* '
    );
  });
});
