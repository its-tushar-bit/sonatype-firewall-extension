/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { SearchEntityType } from 'MainRoot/nosc/search/searchTypes';
import {
  DEFAULT_SEARCH_SOURCE,
  UNSERVABLE_TYPES_FOR_DATA_SOURCE,
  SEARCH_SOURCE_LABEL,
  isTypeHiddenForSource,
  isTypeVisibleForSource,
  parseSearchSource,
} from 'MainRoot/nosc/search/searchDataSource';

const ALL_TYPES: readonly SearchEntityType[] = ['VULNERABILITY', 'COMPONENT', 'APPLICATION', 'VIOLATION', 'WAIVER'];

describe('searchDataSource', () => {
  it('defaults to the local data source', () => {
    expect(DEFAULT_SEARCH_SOURCE).toBe('local');
  });

  it('labels local as "My Scan Data" and catalog as "Sonatype Catalog"', () => {
    expect(SEARCH_SOURCE_LABEL.local).toBe('My Scan Data');
    expect(SEARCH_SOURCE_LABEL.catalog).toBe('Sonatype Catalog');
  });

  it('hides nothing for the local source — it serves all five entity types', () => {
    expect(UNSERVABLE_TYPES_FOR_DATA_SOURCE.local).toEqual([]);
    for (const type of ALL_TYPES) {
      expect(isTypeVisibleForSource(type, 'local')).toBe(true);
      expect(isTypeHiddenForSource(type, 'local')).toBe(false);
    }
  });

  it('hides Application / Violation / Waiver for the catalog source', () => {
    expect(UNSERVABLE_TYPES_FOR_DATA_SOURCE.catalog).toEqual(['APPLICATION', 'VIOLATION', 'WAIVER']);
    expect(isTypeHiddenForSource('APPLICATION', 'catalog')).toBe(true);
    expect(isTypeHiddenForSource('VIOLATION', 'catalog')).toBe(true);
    expect(isTypeHiddenForSource('WAIVER', 'catalog')).toBe(true);
  });

  it('keeps Component + Vulnerability visible for the catalog source', () => {
    expect(isTypeVisibleForSource('COMPONENT', 'catalog')).toBe(true);
    expect(isTypeVisibleForSource('VULNERABILITY', 'catalog')).toBe(true);
    expect(isTypeHiddenForSource('COMPONENT', 'catalog')).toBe(false);
    expect(isTypeHiddenForSource('VULNERABILITY', 'catalog')).toBe(false);
  });

  it('keeps isTypeHiddenForSource and isTypeVisibleForSource as exact inverses', () => {
    for (const type of ALL_TYPES) {
      expect(isTypeVisibleForSource(type, 'local')).toBe(!isTypeHiddenForSource(type, 'local'));
      expect(isTypeVisibleForSource(type, 'catalog')).toBe(!isTypeHiddenForSource(type, 'catalog'));
    }
  });

  it('parseSearchSource accepts catalog and treats everything else as local', () => {
    expect(parseSearchSource('catalog')).toBe('catalog');
    expect(parseSearchSource('local')).toBe('local');
    expect(parseSearchSource(undefined)).toBe('local');
    expect(parseSearchSource('')).toBe('local');
    expect(parseSearchSource('other')).toBe('local');
  });
});

