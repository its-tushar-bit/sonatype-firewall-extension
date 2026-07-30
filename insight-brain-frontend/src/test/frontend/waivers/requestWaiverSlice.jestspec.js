/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions, initialState } from '../../../main/frontend/waivers/requestWaiverSlice';

describe('requestWaiverSlice.initializeStateFromDetails', function () {
  const baseDetails = Object.freeze({
    scopeOwnerId: 'app-id',
    scopeOwnerType: 'application',
    scopeOwnerName: 'docker-nexus-iq-server',
    matcherStrategy: 'EXACT_COMPONENT',
    policyWaiverReasonId: null,
    comment: 'reason',
    noteToReviewer: 'note',
  });

  const load = (overrides) =>
    reducer(initialState, actions.initializeStateFromDetails({ ...baseDetails, ...overrides }));

  describe("flag=true, expiryTime=null (submitted as 'When Remediation Available')", function () {
    it("restores the dropdown to 'remediationAvailable'", function () {
      const state = load({ expiryTime: null, expireWhenRemediationAvailable: true });
      expect(state.expiryTime).toBe('remediationAvailable');
    });

    it('clears the custom-date input', function () {
      const state = load({ expiryTime: null, expireWhenRemediationAvailable: true });
      expect(state.customExpiryTime.value).toBe('');
    });
  });

  describe('flag=false, expiryTime=<date> (submitted as a Custom date)', function () {
    it("restores the dropdown to 'custom'", function () {
      const state = load({
        expiryTime: '2026-09-25T23:59:59.999+0000',
        expireWhenRemediationAvailable: false,
      });
      expect(state.expiryTime).toBe('custom');
    });

    it('populates the custom-date input with the saved date', function () {
      const state = load({
        expiryTime: '2026-09-25T23:59:59.999+0000',
        expireWhenRemediationAvailable: false,
      });
      expect(state.customExpiryTime.value).toBe('2026-09-25');
    });
  });

  describe('flag=false, expiryTime=null (submitted as Never)', function () {
    it('leaves the dropdown at null', function () {
      const state = load({ expiryTime: null, expireWhenRemediationAvailable: false });
      expect(state.expiryTime).toBeNull();
    });
  });

  describe('flag=true, expiryTime=<date> (both set on the server; API allows it)', function () {
    it("prefers 'remediationAvailable' over 'custom' so the auto-clear intent is preserved", function () {
      const state = load({
        expiryTime: '2026-09-25T23:59:59.999+0000',
        expireWhenRemediationAvailable: true,
      });
      expect(state.expiryTime).toBe('remediationAvailable');
    });
  });

  describe('real wire shape — @JsonInclude(NON_EMPTY) omits fields from the payload', function () {
    // ApiPolicyWaiverRequestDTO.expiryTime is @JsonInclude(NON_EMPTY), so a null expiryTime is
    // omitted from the JSON entirely — the reducer receives `undefined`, not `null`. The flag
    // can also be absent from older API responses. Guard against a future refactor that
    // tightens the checks to `=== null` / `!== undefined` / `Object.hasOwn(...)`.
    it('both fields omitted → dropdown null (never)', function () {
      const state = load({});
      expect(state.expiryTime).toBeNull();
      expect(state.customExpiryTime.value).toBe('');
    });

    it('flag omitted, expiryTime present → dropdown custom', function () {
      const state = load({ expiryTime: '2026-09-25T23:59:59.999+0000' });
      expect(state.expiryTime).toBe('custom');
      expect(state.customExpiryTime.value).toBe('2026-09-25');
    });

    it('flag=true, expiryTime omitted → dropdown remediationAvailable', function () {
      const state = load({ expireWhenRemediationAvailable: true });
      expect(state.expiryTime).toBe('remediationAvailable');
      expect(state.customExpiryTime.value).toBe('');
    });
  });

  describe('non-dropdown fields (unchanged behavior)', function () {
    it('restores scope, matcherStrategy, comment, and noteToReviewer', function () {
      const state = load({ expiryTime: null, expireWhenRemediationAvailable: true });
      expect(state.selectedWaiverScope).toEqual({
        id: 'app-id',
        type: 'application',
        name: 'docker-nexus-iq-server',
      });
      expect(state.componentMatcherStrategy).toBe('EXACT_COMPONENT');
      expect(state.comments.value).toBe('reason');
      expect(state.noteToReviewer.value).toBe('note');
    });
  });
});
