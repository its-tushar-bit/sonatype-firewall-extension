/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  messageForWaiverActionDisableReason,
  pendingWaiverRequestSessionKey,
  readPendingWaiverRequestSessionFlag,
  resolveCreateWaiverDisableReason,
  resolveRequestWaiverDisableReason,
  writePendingWaiverRequestSessionFlag,
} from 'MainRoot/nosc/waivers/waiverActionEligibility';

describe('waiverActionEligibility', () => {
  describe('resolveCreateWaiverDisableReason', () => {
    it('blocks when already waived before permission', () => {
      expect(
        resolveCreateWaiverDisableReason({ hasWaivePermission: true, isWaived: true }),
      ).toBe('already_waived');
    });

    it('blocks while permission is loading', () => {
      expect(
        resolveCreateWaiverDisableReason({ hasWaivePermission: null, isWaived: false }),
      ).toBe('checking_permission');
    });

    it('blocks without WAIVE permission', () => {
      expect(
        resolveCreateWaiverDisableReason({ hasWaivePermission: false, isWaived: false }),
      ).toBe('no_permission');
    });

    it('allows when permitted and open', () => {
      expect(
        resolveCreateWaiverDisableReason({ hasWaivePermission: true, isWaived: false }),
      ).toBeNull();
    });
  });

  describe('resolveRequestWaiverDisableReason', () => {
    it('prioritizes enterprise gate', () => {
      expect(
        resolveRequestWaiverDisableReason({
          isWaived: true,
          hasPendingRequest: true,
          isEnterpriseGated: true,
        }),
      ).toBe('enterprise_feature');
    });

    it('blocks already waived', () => {
      expect(
        resolveRequestWaiverDisableReason({
          isWaived: true,
          hasPendingRequest: false,
          isEnterpriseGated: false,
        }),
      ).toBe('already_waived');
    });

    it('blocks pending request', () => {
      expect(
        resolveRequestWaiverDisableReason({
          isWaived: false,
          hasPendingRequest: true,
          isEnterpriseGated: false,
        }),
      ).toBe('pending_request');
    });

    it('allows when open with no pending request', () => {
      expect(
        resolveRequestWaiverDisableReason({
          isWaived: false,
          hasPendingRequest: false,
          isEnterpriseGated: false,
        }),
      ).toBeNull();
    });
  });

  it('maps reasons to user-facing copy', () => {
    expect(messageForWaiverActionDisableReason('no_permission')).toMatch(/permission/i);
    expect(messageForWaiverActionDisableReason(null)).toBeUndefined();
  });

  it('persists pending-request hints in sessionStorage', () => {
    const violationId = 'viol-session-1';
    sessionStorage.removeItem(pendingWaiverRequestSessionKey(violationId));
    expect(readPendingWaiverRequestSessionFlag(violationId)).toBe(false);
    writePendingWaiverRequestSessionFlag(violationId);
    expect(readPendingWaiverRequestSessionFlag(violationId)).toBe(true);
  });
});
