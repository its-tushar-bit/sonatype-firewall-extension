/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import {
  addPredicate,
  FACET_DESCRIPTORS,
  facetPredicate,
  hasPredicate,
  hasThreatLevelField,
  isPredicateToken,
  isQuotableValue,
  orderedFacetKeys,
  quoteValue,
  readThreatLevelRange,
  removePredicate,
  setThreatLevelRange,
  stripPredicates,
  threatLevelFieldForTab,
  threatLevelPredicate,
  togglePredicate,
  tokenize,
} from 'MainRoot/nosc/searchResults/facetQuery';

describe('facetQuery — facet-rail → query round-trip (CLM-42453)', () => {
  describe('facetPredicate', () => {
    it('value facets round-trip as <indexField>:<value>', () => {
      expect(facetPredicate('policyTypes', 'SECURITY')).toBe('policyViolationThreatCategory:SECURITY');
      expect(facetPredicate('stages', 'build')).toBe('policyEvaluationStage:build');
    });

    it('quotes values containing spaces', () => {
      expect(facetPredicate('organizations', 'Sandbox Organization')).toBe('organizationName:"Sandbox Organization"');
      expect(facetPredicate('applications', 'my app')).toBe('applicationName:"my app"');
    });

    it('maps fixed-vocabulary state keys to their grammar predicate', () => {
      expect(facetPredicate('states', 'WAIVED')).toBe('policyViolationWaiverStatus:Waived');
      expect(facetPredicate('states', 'OPEN')).toBe('policyViolationWaiverStatus:Active');
    });

    it('gives waiverType MANUAL no token, so it cannot drive the states WAIVED checkbox', () => {
      // The waiver-status vocabulary distinguishes only AutoWaived from Waived, so a
      // MANUAL predicate would be the very same token states.WAIVED emits: ticking one
      // control would show the other as checked too. MANUAL therefore renders read-only.
      expect(facetPredicate('waiverType', 'MANUAL')).toBeNull();
      expect(facetPredicate('waiverType', 'AUTO')).toBe('policyViolationWaiverStatus:AutoWaived');
      expect(facetPredicate('states', 'WAIVED')).toBe('policyViolationWaiverStatus:Waived');
    });

    it('returns null for an unknown facet key or unmapped fixed value', () => {
      expect(facetPredicate('bogus', 'x')).toBeNull();
      expect(facetPredicate('states', 'NOT_A_STATE')).toBeNull();
    });

    it('renders the WAIVER status buckets read-only rather than as broken filters', () => {
      // Selecting one of these would leave the user's own narrowing in the base each
      // bucket is counted against, collapsing the sibling counts. Until the count base
      // strips those fields, no bucket carries a predicate, so every row renders as a
      // disabled read-only count.
      for (const bucket of ['active', 'expiring', 'expired', 'auto-waived']) {
        expect(facetPredicate('status', bucket)).toBeNull();
      }
    });

    it('never writes a vulnerability-scoped field for a waiver facet', () => {
      // The status buckets previously pointed at vulnerabilityStatus, which is scoped
      // away from waiver docs and compiles to a match-nothing clause without warning.
      for (const bucket of ['active', 'expired', 'auto-waived', 'expiring']) {
        expect(facetPredicate('status', bucket) ?? '').not.toContain('vulnerabilityStatus');
      }
    });
  });

  describe('predicate add/remove/toggle', () => {
    it('adds a predicate with single-space separation and is idempotent', () => {
      expect(addPredicate('jackson', 'policyEvaluationStage:build')).toBe('jackson policyEvaluationStage:build');
      expect(addPredicate('policyEvaluationStage:build', 'policyEvaluationStage:build')).toBe(
        'policyEvaluationStage:build'
      );
    });

    it('detects a present predicate token-exactly (not substring)', () => {
      expect(hasPredicate('a policyEvaluationStage:build b', 'policyEvaluationStage:build')).toBe(true);
      // A substring of a larger token must NOT match.
      expect(hasPredicate('policyEvaluationStage:building', 'policyEvaluationStage:build')).toBe(false);
    });

    it('removes an exact predicate, preserving surrounding text', () => {
      expect(removePredicate('jackson organizationName:"Acme" foo', 'organizationName:"Acme"')).toBe('jackson foo');
    });

    it('toggle adds then removes', () => {
      const p = 'policyViolationThreatCategory:SECURITY';
      const added = togglePredicate('jackson', p);
      expect(added).toBe('jackson policyViolationThreatCategory:SECURITY');
      expect(togglePredicate(added, p)).toBe('jackson');
    });

    it('treats quoted spans as single tokens', () => {
      const q = 'jackson organizationName:"Sandbox Organization"';
      expect(hasPredicate(q, 'organizationName:"Sandbox Organization"')).toBe(true);
    });
  });

  describe('quoteValue', () => {
    it('leaves a bare value unquoted', () => {
      expect(quoteValue('SECURITY')).toBe('SECURITY');
    });
    it('quotes values with whitespace, colon, or quote', () => {
      expect(quoteValue('a b')).toBe('"a b"');
      expect(quoteValue('a:b')).toBe('"a:b"');
      expect(quoteValue('a"b')).toBe('"a\\"b"');
    });
    it('quotes values containing brackets (range syntax in the grammar)', () => {
      // Unquoted, `1.0[RC1]` would be read as range syntax rather than a literal.
      expect(quoteValue('1.0[RC1]')).toBe('"1.0[RC1]"');
      expect(quoteValue('a]b')).toBe('"a]b"');
    });
    it('passes a backslash through instead of doubling it', () => {
      // The grammar's quote reader treats `\` as ordinary data, so doubling it here would
      // round-trip as two literal backslashes and stop matching the value it came from.
      expect(quoteValue('a\\b c')).toBe('"a\\b c"');
      expect(quoteValue('a\\"b')).toBe('"a\\\\"b"');
    });
  });

  describe('isQuotableValue', () => {
    it('accepts values with neither a backslash nor a quote', () => {
      expect(isQuotableValue('Sandbox Organization')).toBe(true);
      expect(isQuotableValue('1.0[RC1]')).toBe(true);
    });

    it('rejects any value containing a backslash', () => {
      expect(isQuotableValue('a\\b')).toBe(false);
      expect(isQuotableValue('trailing\\')).toBe(false);
    });

    it('rejects any value containing a double quote', () => {
      expect(isQuotableValue('a"b')).toBe(false);
      expect(isQuotableValue('trailing"')).toBe(false);
    });

    it('keeps a backslash-bearing bucket read-only rather than emitting a broken predicate', () => {
      // The parser's quote reader has no escape handling, so a `\"` sequence would end the
      // quoted span early: the predicate written into the query would not match the token
      // the round-trip lookup searches for, so the checkbox would read unchecked right
      // after being ticked and a second click would append a duplicate.
      expect(facetPredicate('organizations', 'Org\\Name')).toBeNull();
      expect(facetPredicate('applications', 'app trailing\\')).toBeNull();
      // A value without one still round-trips normally.
      expect(facetPredicate('organizations', 'Sandbox Organization')).toBe(
        'organizationName:"Sandbox Organization"'
      );
    });

    it('keeps a quote-bearing bucket read-only rather than emitting a broken predicate', () => {
      // Same reader limitation seen from the other side: emitting `"Org\"Name"` has the reader
      // stop at the inner quote, so the token written into the query is not the token the
      // round-trip lookup searches for and the remainder is parsed as unrelated free text.
      expect(facetPredicate('organizations', 'Org"Name')).toBeNull();
      expect(facetPredicate('applications', 'app trailing"')).toBeNull();
    });
  });

  describe('tokenize', () => {
    it('keeps a bracketed range span as one token', () => {
      // Split on raw whitespace this is three tokens, and no round-trip lookup
      // could ever match the predicate that was written into the query.
      expect(tokenize('foo policyViolationThreatLevel:[3 TO 7] bar')).toEqual([
        'foo',
        'policyViolationThreatLevel:[3 TO 7]',
        'bar',
      ]);
    });

    it('round-trips a parenthesised OR group as one token', () => {
      // The grammar accepts OR groups, so a multi-value predicate must survive the
      // tokenizer intact or add/remove/detect would disagree on the token.
      expect(tokenize('a (f:1 OR f:2) b')).toEqual(['a', '(f:1 OR f:2)', 'b']);
      const p = '(organizationName:a OR organizationName:b)';
      const q = addPredicate('jackson', p);
      expect(tokenize(q)).toEqual(['jackson', p]);
      expect(hasPredicate(q, p)).toBe(true);
      expect(togglePredicate(q, p)).toBe('jackson');
      expect(isPredicateToken(p)).toBe(true);
      expect(stripPredicates(q)).toBe('jackson');
    });

    it('keeps a quoted span as one token', () => {
      expect(tokenize('foo organizationName:"Sandbox Organization"')).toEqual([
        'foo',
        'organizationName:"Sandbox Organization"',
      ]);
    });

    it('round-trips a bracketed range predicate through has/toggle', () => {
      const p = 'policyViolationThreatLevel:[3 TO 7]';
      const q = addPredicate('jackson', p);
      expect(q).toBe('jackson policyViolationThreatLevel:[3 TO 7]');
      expect(hasPredicate(q, p)).toBe(true);
      // Toggling off must remove it rather than append a second dead copy.
      expect(togglePredicate(q, p)).toBe('jackson');
      // And toggling twice is a no-op, not an accumulation.
      expect(togglePredicate(togglePredicate(q, p), p)).toBe(q);
    });
  });

  describe('stripPredicates', () => {
    it('keeps free text and drops field predicates', () => {
      expect(stripPredicates('jackson policyEvaluationStage:build log4j')).toBe('jackson log4j');
    });

    it('drops a quoted predicate whole rather than shattering it', () => {
      // Splitting on raw whitespace would drop `organizationName:"Sandbox` and
      // leave `Organization"` behind as stray free text.
      expect(stripPredicates('jackson organizationName:"Sandbox Organization"')).toBe('jackson');
    });

    it('drops a bracketed range predicate whole rather than leaving fragments', () => {
      // Raw whitespace splitting would leave `TO 7]` behind.
      expect(stripPredicates('jackson policyViolationThreatLevel:[3 TO 7]')).toBe('jackson');
      expect(stripPredicates('policyViolationThreatLevel:[3 TO 7]')).toBe('');
    });

    it('identifies predicate tokens by known grammar field', () => {
      expect(isPredicateToken('organizationName:"a b"')).toBe(true);
      expect(isPredicateToken('policyViolationThreatLevel:[3 TO 7]')).toBe(true);
      expect(isPredicateToken('jackson')).toBe(false);
    });

    it('treats a colon-bearing search term as free text, not a predicate', () => {
      // A Maven coordinate is a search term. Classifying it as a predicate would let
      // Reset delete the user's actual query.
      expect(isPredicateToken('org.apache.logging.log4j:log4j-core')).toBe(false);
      expect(isPredicateToken('com.foo:bar:1.0')).toBe(false);
      expect(isPredicateToken('notAField:whatever')).toBe(false);
    });

    it('keeps a colon-bearing search term through a reset', () => {
      const coordinate = 'org.apache.logging.log4j:log4j-core';
      expect(stripPredicates(coordinate)).toBe(coordinate);
      expect(stripPredicates(`${coordinate} policyEvaluationStage:build`)).toBe(coordinate);
    });
  });

  describe('threat-level range', () => {
    it('writes each tab its own threat-level field', () => {
      // Every tab indexes the threat level under a different field, and the query
      // compiler scopes a field to its allowed entity types: the wrong field
      // compiles to a match-nothing clause with no warning, silently zeroing every
      // result on that tab.
      expect(threatLevelPredicate('VIOLATION', 2, 8)).toBe('policyViolationThreatLevel:[2 TO 8]');
      expect(threatLevelPredicate('APPLICATION', 2, 8)).toBe('applicationMaxPolicyThreatLevel:[2 TO 8]');
      expect(threatLevelPredicate('COMPONENT', 2, 8)).toBe('componentMaxPolicyThreatLevel:[2 TO 8]');
      expect(threatLevelPredicate('WAIVER', 2, 8)).toBe('policyWaiverThreatLevel:[2 TO 8]');
    });

    it('offers the slider only on tabs that have a threat-level field', () => {
      for (const tab of ['APPLICATION', 'VIOLATION', 'COMPONENT', 'WAIVER']) {
        expect(hasThreatLevelField(tab)).toBe(true);
        expect(threatLevelFieldForTab(tab)).not.toBeNull();
      }
      expect(hasThreatLevelField('VULNERABILITY')).toBe(false);
      expect(threatLevelFieldForTab('VULNERABILITY')).toBeNull();
      expect(threatLevelPredicate('VULNERABILITY', 2, 8)).toBeNull();
    });

    it('setThreatLevelRange replaces an existing range', () => {
      const q = 'jackson policyViolationThreatLevel:[2 TO 8]';
      expect(setThreatLevelRange(q, 'VIOLATION', 4, 9)).toBe('jackson policyViolationThreatLevel:[4 TO 9]');
    });

    it('setThreatLevelRange removes the predicate at the full 0–10 span', () => {
      const q = 'jackson policyViolationThreatLevel:[2 TO 8]';
      expect(setThreatLevelRange(q, 'VIOLATION', 0, 10)).toBe('jackson');
    });

    it('setThreatLevelRange round-trips per tab without stacking ranges', () => {
      // Re-dragging must replace, not append, on every tab.
      let q = setThreatLevelRange('jackson', 'WAIVER', 3, 7);
      expect(q).toBe('jackson policyWaiverThreatLevel:[3 TO 7]');
      expect(readThreatLevelRange(q, 'WAIVER')).toEqual([3, 7]);
      q = setThreatLevelRange(q, 'WAIVER', 5, 9);
      expect(q).toBe('jackson policyWaiverThreatLevel:[5 TO 9]');
      expect(setThreatLevelRange(q, 'WAIVER', 0, 10)).toBe('jackson');
    });

    it('setThreatLevelRange leaves the query alone on a tab with no field', () => {
      expect(setThreatLevelRange('jackson', 'VULNERABILITY', 3, 7)).toBe('jackson');
    });

    it('does not rewrite the range syntax from inside a quoted value', () => {
      // Clearing must be token-wise: a raw regex replace over the whole string would
      // reach inside the quotes and leave `applicationName:""` behind.
      const q = 'applicationName:"policyViolationThreatLevel:[3 TO 7]"';
      expect(setThreatLevelRange(q, 'VIOLATION', 0, 10)).toBe(q);
      expect(setThreatLevelRange(q, 'VIOLATION', 4, 9)).toBe(`${q} policyViolationThreatLevel:[4 TO 9]`);
    });

    it('does not read a quoted value as the current threat-level selection', () => {
      expect(readThreatLevelRange('applicationName:"policyViolationThreatLevel:[3 TO 7]"', 'VIOLATION')).toEqual([
        0, 10,
      ]);
    });

    it('readThreatLevelRange reads the range for the tab or defaults to full span', () => {
      expect(readThreatLevelRange('jackson policyViolationThreatLevel:[3 TO 7]', 'VIOLATION')).toEqual([3, 7]);
      expect(readThreatLevelRange('jackson', 'VIOLATION')).toEqual([0, 10]);
      // Another tab's range must not be read as this tab's selection.
      expect(readThreatLevelRange('jackson policyViolationThreatLevel:[3 TO 7]', 'WAIVER')).toEqual([0, 10]);
    });
  });

  describe('orderedFacetKeys', () => {
    it('orders keys to the prototype rail order regardless of input order', () => {
      const ordered = orderedFacetKeys(['policyTypes', 'applications', 'states', 'organizations']);
      expect(ordered).toEqual(['states', 'policyTypes', 'organizations', 'applications']);
    });

    it('sends unknown keys to the end, preserving known-key order', () => {
      const ordered = orderedFacetKeys(['zzz', 'states']);
      expect(ordered[0]).toBe('states');
      expect(ordered[ordered.length - 1]).toBe('zzz');
    });
  });

  describe('descriptor coverage for the backend facet keys', () => {
    // A key the backend emits with no descriptor is dropped by isRenderableFacet, so the section
    // silently disappears from the rail. threatLevel is the one deliberate omission: it renders as
    // the client-side slider instead of checkbox buckets.
    const WAIVER_KEYS = [
      'status',
      'auto',
      'threatLevel',
      'scope',
      'policyType',
      'organizationName',
      'applicationName',
      'policyName',
    ];

    it('has a descriptor for every WAIVER facet key except the slider-backed one', () => {
      const missing = WAIVER_KEYS.filter((k) => k !== 'threatLevel' && !FACET_DESCRIPTORS[k]);
      expect(missing).toEqual([]);
    });

    it('maps the WAIVER application and policy-name facets to their grammar fields', () => {
      expect(FACET_DESCRIPTORS.applicationName.field).toBe('applicationName');
      expect(FACET_DESCRIPTORS.policyName.field).toBe('policyWaiverPolicyName');
    });

    it('treats a policy-name predicate as structured so Reset strips it', () => {
      const token = facetPredicate('policyName', 'Security Policy');
      expect(isPredicateToken(token)).toBe(true);
      expect(stripPredicates(`log4j ${token}`)).toBe('log4j');
    });
  });
});
