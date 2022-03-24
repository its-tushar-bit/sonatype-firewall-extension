/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
  selectIsNotificationsSupported,
  selectIsWebhooksSupported,
  selectIsSourceControlSupported,
  selectIsInnerSourceRepositorySupported,
  selectIsEvaluateApplicationAvailable,
  selectIsSourceControlForSourceTileSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('productFeaturesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      productFeatures: {
        enforcement: true,
        firewall: true,
        'policy-monitoring': true,
        'policy-grandfathering': true,
        notifications: true,
        'webhooks-for-applications': true,
        'webhooks-for-repositories': true,
        automation: true,
        'inner-source-repository-integration': true,
        'cli-integration': true,
      },
    };
  });

  describe('selectIsEnforcementSupported', () => {
    it('returns true if enforcement enabled', () => {
      expect(selectIsEnforcementSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsFirewallSupported', () => {
    it('returns true if firewall enabled', () => {
      expect(selectIsFirewallSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsMonitoringSupported', () => {
    it('returns true if policy-monitoring enabled', () => {
      expect(selectIsMonitoringSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsGrandfatheringSupported', () => {
    it('returns true if policy-grandfathering enabled', () => {
      expect(selectIsGrandfatheringSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsNotificationsSupported', () => {
    it('returns true if notifications enabled', () => {
      expect(selectIsNotificationsSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsWebhooksSupported', () => {
    it('returns true if either webhooks-for-applications or webhooks-for-repositories are enabled', () => {
      expect(selectIsWebhooksSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsSourceControlSupported', () => {
    it('returns true if automation enabled', () => {
      expect(selectIsSourceControlSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsInnerSourceRepositorySupported', () => {
    it('returns true if inner-source-repository-integration enabled', () => {
      expect(selectIsInnerSourceRepositorySupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsEvaluateApplicationAvailable', () => {
    it('returns true if cli-integration enabled', () => {
      expect(selectIsEvaluateApplicationAvailable(mockState)).toBeTrue();
    });
  });

  describe('selectIsSourceControlForSourceTileSupported', () => {
    it('returns true if notifications enabled', () => {
      mockState.productFeatures.notifications = true;
      mockState.productFeatures.automation = false;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBeTrue();
    });

    it('returns true if automation enabled', () => {
      mockState.productFeatures.notifications = false;
      mockState.productFeatures.automation = true;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBeTrue();
    });
  });
});
