/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { assocPath } from 'ramda';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsMonitoringSupported,
  selectIsLegacyViolationSupported,
  selectIsNotificationsSupported,
  selectIsWebhooksSupported,
  selectIsAutomationSupported,
  selectIsInnerSourceRepositorySupported,
  selectIsArtifactoryRepositorySupported,
  selectIsEvaluateApplicationAvailable,
  selectIsSourceControlForSourceTileSupported,
  selectIsAdvancedLegalPackSupported,
  selectIsReleaseIntegritySupported,
  selectIsFirewallAutoUnquarantineSupported,
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsReportListSupported,
  selectIsDataInsightsSupported,
  selectIsInnerSourceTransitiveWaiverSupported,
  selectIsAllowExternalHyperlinksSupported,
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectProductFeaturesSlice,
  selectProductFeatures,
  selectIsBaseUrlConfigurationEnabled,
  selectIsIntegratedEnterpriseReportingEnabled,
  selectTenantScmProviderTypes,
  selectIsScmEnabled,
  selectIsAutomaticScmConfigurationEnabled,
  selectTenantScmOptionsTypes,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('productFeaturesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      productFeatures: {
        loading: false,
        loadError: 'error',
        productFeatures: {
          enforcement: true,
          firewall: true,
          'policy-monitoring': true,
          'policy-grandfathering': true,
          notifications: true,
          'webhooks-for-applications': true,
          automation: true,
          'inner-source-repository-integration': true,
          'built-from-source': true,
          'cli-integration': true,
          'reports-list': true,
          dashboard: true,
          'advanced-legal-pack': true,
          'release-integrity': true,
          'firewall-auto-unquarantine': true,
          'data-insights': true,
          'inner-source-transitive-waiver': true,
          'allow-external-hyperlinks': true,
          'integrated-enterprise-reporting': true,
        },
      },
    };
  });

  describe('selectProductFeaturesSlice', () => {
    it('returns selectProductFeaturesSlice', () => {
      expect(selectProductFeaturesSlice(mockState)).toEqual(mockState.productFeatures);
    });
  });

  describe('selectProductFeatures', () => {
    it('returns productFeatures object', () => {
      expect(selectProductFeatures(mockState)).toEqual(mockState.productFeatures.productFeatures);
    });
  });

  describe('selectLoadingFeatures', () => {
    it('returns loading', () => {
      expect(selectLoadingFeatures(mockState)).toBeFalse();
    });
  });

  describe('selectLoadErrorFeatures', () => {
    it('returns loadError', () => {
      expect(selectLoadErrorFeatures(mockState)).toBe('error');
    });
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

  describe('selectIsLegacyViolationSupported', () => {
    it('returns true if policy-grandfathering is enabled', () => {
      expect(selectIsLegacyViolationSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsNotificationsSupported', () => {
    it('returns true if notifications enabled', () => {
      expect(selectIsNotificationsSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsWebhooksSupported', () => {
    it('returns true if webhooks-for-applications is enabled', () => {
      expect(selectIsWebhooksSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsAutomationSupported', () => {
    it('returns true if automation enabled', () => {
      expect(selectIsAutomationSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsInnerSourceRepositorySupported', () => {
    it('returns true if inner-source-repository-integration enabled', () => {
      expect(selectIsInnerSourceRepositorySupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsArtifactoryRepositorySupported', () => {
    it('returns true if built-from-source is enabled', () => {
      expect(selectIsArtifactoryRepositorySupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsEvaluateApplicationAvailable', () => {
    it('returns true if cli-integration enabled', () => {
      expect(selectIsEvaluateApplicationAvailable(mockState)).toBeTrue();
    });
  });

  describe('selectIsSourceControlForSourceTileSupported', () => {
    it('returns true if notifications enabled', () => {
      mockState.productFeatures.productFeatures.notifications = true;
      mockState.productFeatures.productFeatures.automation = false;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBeTrue();
    });

    it('returns true if automation enabled', () => {
      mockState.productFeatures.productFeatures.notifications = false;
      mockState.productFeatures.productFeatures.automation = true;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsReportListSupported', () => {
    it('returns true if report-list enabled', () => {
      expect(selectIsReportListSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsDashboardSupported', () => {
    it('returns true if dashboard enabled', () => {
      expect(selectIsDashboardSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsAdvancedLegalPackSupported', () => {
    it('returns true if advanced-legal-pack enabled', () => {
      expect(selectIsAdvancedLegalPackSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsReleaseIntegritySupported', () => {
    it('returns true if release-integrity enabled', () => {
      expect(selectIsReleaseIntegritySupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsFirewallAutoUnquarantineSupported', () => {
    it('returns true if firewall-auto-unquarantine enabled', () => {
      expect(selectIsFirewallAutoUnquarantineSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsFirewallSupportedForNavigationContainer', () => {
    it('returns true if both firewall-auto-unquarantine and release-integrity are enabled', () => {
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBeTrue();
    });

    it('returns false if firewall-auto-unquarantine is disabled', () => {
      mockState.productFeatures.productFeatures['firewall-auto-unquarantine'] = false;
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBeFalse();
    });

    it('returns false if release-integrity is disabled', () => {
      mockState.productFeatures.productFeatures['release-integrity'] = false;
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBeFalse();
    });
  });

  describe('selectIsDataInsightsSupported', () => {
    it('returns true if data-insights enabled', () => {
      expect(selectIsDataInsightsSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsIntegratedEnterpriseReportingEnabled', () => {
    it('returns true if integrated-enterprise-reporting is enabled', () => {
      expect(selectIsIntegratedEnterpriseReportingEnabled(mockState)).toBeTrue();
    });

    it('returns false if integrated-enterprise-reporting is disabled', () => {
      mockState.productFeatures.productFeatures['integrated-enterprise-reporting'] = false;
      expect(selectIsIntegratedEnterpriseReportingEnabled(mockState)).toBeFalse();
    });
  });

  describe('selectIsInnerSourceTransitiveWaiverSupported', () => {
    it('returns true if inner-source-transitive-waiver enabled', () => {
      expect(selectIsInnerSourceTransitiveWaiverSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsAllowExternalHyperlinksSupported', () => {
    it('returns true if allow-external-hyperlinks enabled', () => {
      expect(selectIsAllowExternalHyperlinksSupported(mockState)).toBeTrue();
    });
  });

  describe('selectIsBaseUrlConfigurationEnabled', () => {
    it('returns false in multi-tenant mode', () => {
      mockState.productFeatures.productFeatures['multi-tenant'] = true;
      expect(selectIsBaseUrlConfigurationEnabled(mockState)).toBeFalse();
    });

    it('returns true in single-tenant mode', () => {
      mockState.productFeatures.productFeatures['single-tenant'] = true;
      expect(selectIsBaseUrlConfigurationEnabled(mockState)).toBeTrue();
    });
  });

  describe('selectScmProviders', () => {
    it('returns only github in multi-tenant mode', () => {
      mockState.productFeatures.productFeatures['multi-tenant'] = true;
      expect(selectTenantScmProviderTypes(mockState)).toHaveSize(1);
    });

    it('returns all providers in single-tenant mode', () => {
      mockState.productFeatures.productFeatures['single-tenant'] = true;
      expect(selectTenantScmProviderTypes(mockState)).toHaveSize(4);
    });
  });

  describe('selectIsAutomaticScmConfigurationEnabled', () => {
    it('returns true iff saas-lifecycle-scm-enabled and automatic-scm-configuration are both true, and false otherwise', () => {
      // undefined, undefined -> false
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();

      // true, undefined -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'automatic-scm-configuration'], true, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();

      // true, false -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();

      // true, true -> true
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], true, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeTrue();

      // false, true -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'automatic-scm-configuration'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();

      // undefined, true -> false
      mockState = assocPath(
        ['productFeatures', 'productFeatures', 'automatic-scm-configuration'],
        undefined,
        mockState
      );
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();

      // undefined, false -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBeFalse();
    });
  });

  describe('selectIsScmEnabled', () => {
    it('returns true iff saas-lifecycle-scm-enabled is true, and false otherwise', () => {
      expect(selectIsScmEnabled(mockState)).toBeFalse();

      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsScmEnabled(mockState)).toBeFalse();

      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], true, mockState);
      expect(selectIsScmEnabled(mockState)).toBeTrue();
    });
  });

  describe('selectSourceControlOptions', () => {
    it('returns all with the exception of auto remediated PR in multi-tenant mode', () => {
      mockState.productFeatures.productFeatures['multi-tenant'] = true;
      expect(selectTenantScmOptionsTypes(mockState)).toHaveSize(4);
    });

    it('returns all option in single-tenant mode', () => {
      mockState.productFeatures.productFeatures['single-tenant'] = true;
      expect(selectTenantScmOptionsTypes(mockState)).toHaveSize(5);
    });
  });
});
