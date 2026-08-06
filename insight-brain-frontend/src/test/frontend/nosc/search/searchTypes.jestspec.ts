/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  ITEM_TYPE_LABEL,
  RENDERED_ITEM_TYPES,
  ResultRow,
  SearchEntityType,
  SearchRow,
  SuggestRow,
  displayNameFor,
  isApplication,
  isComponent,
  isViolation,
  isVulnerability,
  isWaiver,
  reactKeyFor,
  resultRowToSearchRow,
  suggestRowToSearchRow,
  tabIdForType,
} from 'MainRoot/nosc/search/searchTypes';

function row(type: SearchEntityType, extra: Partial<SearchRow> = {}): SearchRow {
  return {
    id: 'id-1',
    type,
    source: 'local',
    title: 'Title',
    subtitle: '',
    href: null,
    fields: {},
    ...extra,
  };
}

describe('searchTypes type guards', () => {
  it.each([
    ['APPLICATION', isApplication],
    ['COMPONENT', isComponent],
    ['VULNERABILITY', isVulnerability],
    ['VIOLATION', isViolation],
    ['WAIVER', isWaiver],
  ] as const)('%s guard is true only for its own type', (matchingType, guard) => {
    for (const t of RENDERED_ITEM_TYPES) {
      expect(guard(row(t))).toBe(t === matchingType);
    }
  });
});

describe('RENDERED_ITEM_TYPES / ITEM_TYPE_LABEL', () => {
  it('covers the five public entity types in fixed presentation order', () => {
    expect(RENDERED_ITEM_TYPES).toEqual([
      'APPLICATION',
      'COMPONENT',
      'VULNERABILITY',
      'VIOLATION',
      'WAIVER',
    ]);
  });

  it('has a non-empty label for every rendered type', () => {
    for (const t of RENDERED_ITEM_TYPES) {
      expect(ITEM_TYPE_LABEL[t]).toBeTruthy();
    }
  });
});

describe('tabIdForType', () => {
  it('maps each entity type to its uppercase results tab id', () => {
    expect(tabIdForType('VULNERABILITY')).toBe('VULNERABILITY');
    expect(tabIdForType('APPLICATION')).toBe('APPLICATION');
    expect(tabIdForType('VIOLATION')).toBe('VIOLATION');
  });
});

describe('suggestRowToSearchRow', () => {
  it('maps wire fields and coerces a null href / subtitle', () => {
    const wire: SuggestRow = {
      id: 'CVE-2021-44228',
      type: 'VULNERABILITY',
      source: 'local',
      title: 'CVE-2021-44228',
      subtitle: 'Log4Shell',
      href: null,
    };
    expect(suggestRowToSearchRow(wire)).toEqual({
      id: 'CVE-2021-44228',
      type: 'VULNERABILITY',
      source: 'local',
      title: 'CVE-2021-44228',
      subtitle: 'Log4Shell',
      href: null,
      fields: {},
    });
  });

  it('coerces an absent subtitle to an empty string', () => {
    const wire: SuggestRow = {
      id: 'app-1',
      type: 'APPLICATION',
      source: 'local',
      title: 'Webgoat',
    };
    const mapped = suggestRowToSearchRow(wire);
    expect(mapped.subtitle).toBe('');
    expect(mapped.href).toBeNull();
    expect(mapped.fields).toEqual({});
  });
});

describe('resultRowToSearchRow', () => {
  it('maps wire fields and preserves the open fields bag + href', () => {
    const wire: ResultRow = {
      id: 'app-1',
      type: 'APPLICATION',
      source: 'local',
      title: 'Webgoat',
      subtitle: 'Engineering',
      fields: { organizationName: 'Engineering' },
      href: '#/applications/webgoat-app',
    };
    expect(resultRowToSearchRow(wire)).toEqual({
      id: 'app-1',
      type: 'APPLICATION',
      source: 'local',
      title: 'Webgoat',
      subtitle: 'Engineering',
      href: '#/applications/webgoat-app',
      fields: { organizationName: 'Engineering' },
    });
  });

  it('defaults an absent subtitle / fields to empty', () => {
    const wire: ResultRow = {
      id: 'c-1',
      type: 'COMPONENT',
      source: 'catalog',
      title: 'log4j-core',
    };
    const mapped = resultRowToSearchRow(wire);
    expect(mapped.subtitle).toBe('');
    expect(mapped.fields).toEqual({});
    expect(mapped.href).toBeNull();
  });
});

describe('displayNameFor', () => {
  it('returns the title, falling back to the id', () => {
    expect(displayNameFor(row('APPLICATION', { title: 'Webgoat' }))).toBe('Webgoat');
    expect(displayNameFor(row('APPLICATION', { title: '', id: 'app-1' }))).toBe('app-1');
  });
});

describe('reactKeyFor', () => {
  it('produces a stable type + source + id key', () => {
    expect(reactKeyFor(row('VULNERABILITY', { source: 'catalog', id: 'CVE-1' }))).toBe(
      'VULNERABILITY:catalog:CVE-1',
    );
  });
});
