/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { quoteEnumValue } from 'MainRoot/nosc/search/searchFilterInsert';
import { FILTER_TREE, FilterLeaf, FilterNode, isCompleteSyntax } from 'MainRoot/nosc/search/searchFilterTree';

function allLeaves(nodes: readonly FilterNode[]): readonly FilterLeaf[] {
  return nodes.flatMap((node) => [...(node.leaves ?? []), ...(node.groups ?? []).flatMap((group) => group.leaves)]);
}

function allEnumValues(): readonly string[] {
  return allLeaves(FILTER_TREE).flatMap((leaf) => leaf.values ?? []);
}

describe('FILTER_TREE shape', () => {
  it('offers the eight prototype categories in order', () => {
    expect(FILTER_TREE.map((n) => n.label)).toEqual([
      'Type',
      'Application',
      'Component',
      'License',
      'Organization',
      'Policy',
      'Violation',
      'Vulnerability',
    ]);
  });

  it('uses the backend-accepted enum values (not the prototype casing)', () => {
    const app = FILTER_TREE.find((n) => n.label === 'Application')!;
    const sbom = app.leaves!.find((l) => l.label === 'SBOM Specification')!;
    // Prototype listed CycloneDx; the FieldMap indexes CycloneDX.
    expect(sbom.values).toEqual(['CycloneDX', 'SPDX']);

    const violation = FILTER_TREE.find((n) => n.label === 'Violation')!;
    const waiverStatus = violation.leaves!.find((l) => l.label === 'Waiver Status')!;
    // Prototype listed Active/Expired; the FieldMap vocabulary is Active/Waived/AutoWaived.
    expect(waiverStatus.values).toEqual(['Active', 'Waived', 'AutoWaived']);
  });
});

describe('isCompleteSyntax', () => {
  it('treats a syntax carrying a value as complete', () => {
    expect(isCompleteSyntax('itemType:APPLICATION')).toBe(true);
    expect(isCompleteSyntax('componentFormat:maven')).toBe(true);
    expect(isCompleteSyntax('vulnerabilityStatus:"Not Applicable"')).toBe(true);
  });

  it('treats a value-expecting syntax as incomplete', () => {
    expect(isCompleteSyntax('applicationName:')).toBe(false);
    expect(isCompleteSyntax('applicationName:""')).toBe(false);
  });
});

describe('FILTER_TREE range/score leaves', () => {
  // Named explicitly rather than matched by regex: these are the leaves whose
  // label advertises a range or score the user types, so their syntax must ship
  // no placeholder value and must never auto-commit a search.
  const RANGE_LEAF_SYNTAXES = [
    'componentLicenseThreatLevel:',
    'policyThreatLevel:',
    'policyViolationThreatLevel:',
    'vulnerabilitySeverity:',
  ];

  it.each(RANGE_LEAF_SYNTAXES)('%s is present in the tree and expects a typed value', (syntax) => {
    const leaf = allLeaves(FILTER_TREE).find((l) => l.syntax === syntax);
    expect(leaf).toBeDefined();
    expect(isCompleteSyntax(syntax)).toBe(false);
    // No hardcoded placeholder value ships in the inserted syntax.
    expect(syntax).toMatch(/:$/);
  });
});

describe('FILTER_TREE enum vocabulary invariants', () => {
  it('covers a non-trivial number of enum values', () => {
    expect(allEnumValues().length).toBeGreaterThan(20);
  });

  /**
   * quoteEnumValue wraps whitespace-bearing values in double quotes without
   * escaping embedded quotes, so a value containing `"` would emit malformed
   * query syntax. The backend FieldMap vocabularies are closed sets of
   * identifier-like tokens; this asserts the frontend mirror stays that way.
   */
  it('has no value containing a double quote', () => {
    expect(allEnumValues().filter((value) => value.includes('"'))).toEqual([]);
  });

  it('quotes every whitespace-bearing value and leaves the rest bare', () => {
    for (const value of allEnumValues()) {
      expect(quoteEnumValue(value)).toBe(/\s/.test(value) ? `"${value}"` : value);
    }
  });
});
