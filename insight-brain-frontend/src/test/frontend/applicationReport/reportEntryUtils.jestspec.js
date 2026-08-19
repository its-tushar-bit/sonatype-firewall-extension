/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { unwrapReportEntry, ensureAaData, getReportDisplayName } from 'MainRoot/applicationReport/reportEntryUtils';

const toBase64 = (str) => Buffer.from(str, 'utf-8').toString('base64');

describe('reportEntryUtils', () => {
  describe('unwrapReportEntry', () => {
    it('unwraps the HRC ReportEntry {name, time, buf(base64)} envelope into parsed JSON', () => {
      const payload = { aaData: [{ policyViolationId: 'v1' }] };
      const wrapped = {
        name: 'policythreats.json',
        time: 1702041439230,
        buf: toBase64(JSON.stringify(payload)),
      };

      expect(unwrapReportEntry(wrapped)).toEqual(payload);
    });

    it('preserves multi-byte UTF-8 through base64 decode (regression guard against atob())', () => {
      // atob() alone yields Latin-1 code points and mangles non-ASCII; the module uses
      // TextDecoder('utf-8') to keep component names / violation messages intact.
      const payload = { name: 'コンポーネント日本語', emoji: '🔒', notes: 'café' };
      const wrapped = {
        name: 'bom.json',
        time: 0,
        buf: toBase64(JSON.stringify(payload)),
      };

      expect(unwrapReportEntry(wrapped)).toEqual(payload);
    });

    it('returns undefined and does not throw when the buf is malformed base64', () => {
      const wrapped = {
        name: 'broken.json',
        time: 42,
        buf: 'not-valid-base64!!!',
      };

      expect(unwrapReportEntry(wrapped)).toBeUndefined();
    });

    it('returns undefined and does not throw when the decoded string is not valid JSON', () => {
      const wrapped = {
        name: 'broken.json',
        time: 42,
        buf: toBase64('this is not json'),
      };

      expect(unwrapReportEntry(wrapped)).toBeUndefined();
    });

    it('falls through unchanged for already-parsed application responses (no envelope shape)', () => {
      const alreadyParsed = { aaData: [{ policyViolationId: 'v1' }] };
      expect(unwrapReportEntry(alreadyParsed)).toBe(alreadyParsed);
    });

    it('falls through unchanged for null / undefined / primitive inputs', () => {
      expect(unwrapReportEntry(null)).toBeNull();
      expect(unwrapReportEntry(undefined)).toBeUndefined();
      expect(unwrapReportEntry(42)).toBe(42);
      expect(unwrapReportEntry('str')).toBe('str');
    });

    it('does not treat partial envelopes (missing buf/name/time) as wrappers', () => {
      // Only complete {name: string, time: number, buf: string} triggers unwrap. A raw report
      // object that happens to have a `name` field must fall through untouched.
      const partial = { name: 'not-a-wrapper', aaData: [] };
      expect(unwrapReportEntry(partial)).toBe(partial);
    });
  });

  describe('ensureAaData', () => {
    it('returns the object with the existing aaData preserved when present', () => {
      expect(ensureAaData({ aaData: [1, 2, 3], extra: 'ok' })).toEqual({ aaData: [1, 2, 3], extra: 'ok' });
    });

    it('adds an empty aaData array when the input is an object without one', () => {
      expect(ensureAaData({ other: 'stuff' })).toEqual({ other: 'stuff', aaData: [] });
    });

    it('unwraps the ReportEntry envelope first, then normalizes', () => {
      const wrapped = {
        name: 'policythreats.json',
        time: 1,
        buf: toBase64(JSON.stringify({ policyThreats: [] })),
      };
      expect(ensureAaData(wrapped)).toEqual({ policyThreats: [], aaData: [] });
    });

    it('returns undefined for null / non-object inputs', () => {
      expect(ensureAaData(null)).toBeUndefined();
      expect(ensureAaData(undefined)).toBeUndefined();
      expect(ensureAaData(42)).toBeUndefined();
    });
  });

  describe('getReportDisplayName', () => {
    it('prefers metadata.application.name when present', () => {
      expect(
        getReportDisplayName({ application: { name: 'my-app' } }, { componentDisplayName: 'ignored', hrcId: 'ignored' })
      ).toBe('my-app');
    });

    it('falls back to routerParams.componentDisplayName when application.name is missing (HRC path)', () => {
      expect(getReportDisplayName({ application: null }, { componentDisplayName: 'ansible 2.8.0 (.tar.gz)' })).toBe(
        'ansible 2.8.0 (.tar.gz)'
      );
    });

    it('falls back to routerParams.hrcId when application.name and componentDisplayName are both missing', () => {
      expect(getReportDisplayName({ application: null }, { hrcId: 'hrc-uuid-1' })).toBe('hrc-uuid-1');
    });

    it('returns an empty string when nothing is available', () => {
      expect(getReportDisplayName(null, null)).toBe('');
      expect(getReportDisplayName({}, {})).toBe('');
      expect(getReportDisplayName(undefined, undefined)).toBe('');
    });
  });
});
