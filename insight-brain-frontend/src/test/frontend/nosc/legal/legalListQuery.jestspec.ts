/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildLegalListRouteParams,
  parseLegalListParams,
} from 'MainRoot/nosc/legal/legalListQuery';

describe('legalListQuery (CLM-43207)', () => {
  it('treats category URL tokens as LTG names', () => {
    const parsed = parseLegalListParams({
      category: 'Copyleft,Banned',
      page: '2',
    });
    expect(Array.from(parsed.filters.threatCategories).sort()).toEqual(['Banned', 'Copyleft']);
    // URL page is 1-based; parsed state is 0-based.
    expect(parsed.page).toBe(1);
  });

  it('serializes LTG names back onto category', () => {
    const params = buildLegalListRouteParams(
      parseLegalListParams({ category: 'Copyleft', stage: 'build' }),
    );
    expect(params.category).toBe('Copyleft');
    expect(params.stage).toBe('build');
  });

  it('ignores state and waiver URL tokens (not applicable to LEGAL_VIOLATION)', () => {
    const parsed = parseLegalListParams({
      category: 'Copyleft',
      state: 'OPEN',
      waiver: 'auto',
    });
    expect(Array.from(parsed.filters.threatCategories)).toEqual(['Copyleft']);
    expect(parsed.filters.states.size).toBe(0);
    expect(parsed.filters.waiverType).toBe('ANY');
    const params = buildLegalListRouteParams(parsed);
    expect(params).not.toHaveProperty('state');
    expect(params).not.toHaveProperty('waiver');
    expect(params.category).toBe('Copyleft');
  });
});
