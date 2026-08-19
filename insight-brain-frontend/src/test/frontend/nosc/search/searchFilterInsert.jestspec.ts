/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { computeFilterInsert, quoteEnumValue } from 'MainRoot/nosc/search/searchFilterInsert';

describe('quoteEnumValue', () => {
  it('leaves single-token values unquoted', () => {
    expect(quoteEnumValue('Open')).toBe('Open');
    expect(quoteEnumValue('CycloneDX')).toBe('CycloneDX');
    expect(quoteEnumValue('stage-release')).toBe('stage-release');
  });

  it('double-quotes a value containing whitespace so the parser reads one phrase', () => {
    expect(quoteEnumValue('Not Applicable')).toBe('"Not Applicable"');
    expect(quoteEnumValue('Security Critical')).toBe('"Security Critical"');
  });
});

describe('computeFilterInsert', () => {
  it('appends to an empty query without a leading space, caret at end', () => {
    expect(computeFilterInsert('', 'itemType:APPLICATION')).toEqual({
      value: 'itemType:APPLICATION',
      caretAt: 20,
      complete: true,
    });
  });

  it('adds a single leading space when chaining onto a non-space-terminated query', () => {
    expect(computeFilterInsert('log4j', 'itemType:COMPONENT')).toEqual({
      value: 'log4j itemType:COMPONENT',
      caretAt: 24,
      complete: true,
    });
  });

  it('does not double the space when the query already ends in whitespace', () => {
    expect(computeFilterInsert('log4j ', 'itemType:COMPONENT').value).toBe('log4j itemType:COMPONENT');
  });

  it('places the caret between the quotes for an incomplete quoted leaf', () => {
    expect(computeFilterInsert('', 'applicationName:""')).toEqual({
      value: 'applicationName:""',
      // Between the two quotes, so the next keystroke lands inside the phrase.
      caretAt: 17,
      complete: false,
    });
  });

  it('reports a value-expecting leaf (trailing colon) as incomplete, caret at end', () => {
    expect(computeFilterInsert('log4j', 'cvssSeverityScore:')).toEqual({
      value: 'log4j cvssSeverityScore:',
      caretAt: 24,
      complete: false,
    });
  });

  it('keeps a quoted multi-word enum value intact as one fielded phrase', () => {
    expect(computeFilterInsert('', 'vulnerabilityStatus:"Not Applicable"')).toEqual({
      value: 'vulnerabilityStatus:"Not Applicable"',
      caretAt: 36,
      complete: true,
    });
  });
});
