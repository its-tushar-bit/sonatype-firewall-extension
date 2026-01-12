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
  selectIsInnerSourceTransitiveWaiverSupported,
  selectIsAllowExternalHyperlinksSupported,
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectProductFeaturesSlice,
  selectProductFeatures,
  selectIsBaseUrlConfigurationEnabled,
  selectTenantScmProviderTypes,
  selectIsScmEnabled,
  selectIsAutomaticScmConfigurationEnabled,
  selectTenantScmOptionsTypes,
  selectIsSAMLEnabled,
  selectIsUserManagementPagesEnabled,
  selectIsGithubAppAuthenticationEnabled,
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
      expect(selectLoadingFeatures(mockState)).toBe(false);
    });
  });

  describe('selectLoadErrorFeatures', () => {
    it('returns loadError', () => {
      expect(selectLoadErrorFeatures(mockState)).toBe('error');
    });
  });

  describe('selectIsEnforcementSupported', () => {
    it('returns true if enforcement enabled', () => {
      expect(selectIsEnforcementSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsFirewallSupported', () => {
    it('returns true if firewall enabled', () => {
      expect(selectIsFirewallSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsMonitoringSupported', () => {
    it('returns true if policy-monitoring enabled', () => {
      expect(selectIsMonitoringSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsLegacyViolationSupported', () => {
    it('returns true if policy-grandfathering is enabled', () => {
      expect(selectIsLegacyViolationSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsNotificationsSupported', () => {
    it('returns true if notifications enabled', () => {
      expect(selectIsNotificationsSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsWebhooksSupported', () => {
    it('returns true if webhooks-for-applications is enabled', () => {
      expect(selectIsWebhooksSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsAutomationSupported', () => {
    it('returns true if automation enabled', () => {
      expect(selectIsAutomationSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsInnerSourceRepositorySupported', () => {
    it('returns true if inner-source-repository-integration enabled', () => {
      expect(selectIsInnerSourceRepositorySupported(mockState)).toBe(true);
    });
  });

  describe('selectIsArtifactoryRepositorySupported', () => {
    it('returns true if built-from-source is enabled', () => {
      expect(selectIsArtifactoryRepositorySupported(mockState)).toBe(true);
    });
  });

  describe('selectIsEvaluateApplicationAvailable', () => {
    it('returns true if cli-integration enabled', () => {
      expect(selectIsEvaluateApplicationAvailable(mockState)).toBe(true);
    });
  });

  describe('selectIsSourceControlForSourceTileSupported', () => {
    it('returns true if notifications enabled', () => {
      mockState.productFeatures.productFeatures.notifications = true;
      mockState.productFeatures.productFeatures.automation = false;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBe(true);
    });

    it('returns true if automation enabled', () => {
      mockState.productFeatures.productFeatures.notifications = false;
      mockState.productFeatures.productFeatures.automation = true;
      expect(selectIsSourceControlForSourceTileSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsReportListSupported', () => {
    it('returns true if report-list enabled', () => {
      expect(selectIsReportListSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsDashboardSupported', () => {
    it('returns true if dashboard enabled', () => {
      expect(selectIsDashboardSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsAdvancedLegalPackSupported', () => {
    it('returns true if advanced-legal-pack enabled', () => {
      expect(selectIsAdvancedLegalPackSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsReleaseIntegritySupported', () => {
    it('returns true if release-integrity enabled', () => {
      expect(selectIsReleaseIntegritySupported(mockState)).toBe(true);
    });
  });

  describe('selectIsFirewallAutoUnquarantineSupported', () => {
    it('returns true if firewall-auto-unquarantine enabled', () => {
      expect(selectIsFirewallAutoUnquarantineSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsFirewallSupportedForNavigationContainer', () => {
    it('returns true if both firewall-auto-unquarantine and release-integrity are enabled', () => {
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBe(true);
    });

    it('returns false if firewall-auto-unquarantine is disabled', () => {
      mockState.productFeatures.productFeatures['firewall-auto-unquarantine'] = false;
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBe(false);
    });

    it('returns false if release-integrity is disabled', () => {
      mockState.productFeatures.productFeatures['release-integrity'] = false;
      expect(selectIsFirewallSupportedForNavigationContainer(mockState)).toBe(false);
    });
  });

  describe('selectIsInnerSourceTransitiveWaiverSupported', () => {
    it('returns true if inner-source-transitive-waiver enabled', () => {
      expect(selectIsInnerSourceTransitiveWaiverSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsAllowExternalHyperlinksSupported', () => {
    it('returns true if allow-external-hyperlinks enabled', () => {
      expect(selectIsAllowExternalHyperlinksSupported(mockState)).toBe(true);
    });
  });

  describe('selectIsBaseUrlConfigurationEnabled', () => {
    it('returns false in multi-tenant mode', () => {
      mockState.productFeatures.productFeatures['multi-tenant'] = true;
      expect(selectIsBaseUrlConfigurationEnabled(mockState)).toBe(false);
    });

    it('returns true in single-tenant mode', () => {
      mockState.productFeatures.productFeatures['single-tenant'] = true;
      expect(selectIsBaseUrlConfigurationEnabled(mockState)).toBe(true);
    });
  });

  describe('selectScmProviders', () => {
    it('returns github and azure and bitbucket and gitlab in multi-tenant mode', () => {
      mockState.productFeatures.productFeatures['multi-tenant'] = true;
      expect(selectTenantScmProviderTypes(mockState)).toEqual([
        { name: 'Azure DevOps', value: 'azure' },
        { name: 'Bitbucket', value: 'bitbucket' },
        { name: 'GitHub', value: 'github' },
        { name: 'GitLab', value: 'gitlab' },
      ]);
    });

    it('returns all providers in single-tenant mode', () => {
      mockState.productFeatures.productFeatures['single-tenant'] = true;
      expect(selectTenantScmProviderTypes(mockState)).toHaveLength(4);
    });
  });

  describe('selectIsAutomaticScmConfigurationEnabled', () => {
    it('returns true iff saas-lifecycle-scm-enabled and automatic-scm-configuration are both true, and false otherwise', () => {
      // undefined, undefined -> false
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);

      // true, undefined -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'automatic-scm-configuration'], true, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);

      // true, false -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);

      // true, true -> true
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], true, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(true);

      // false, true -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'automatic-scm-configuration'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);

      // undefined, true -> false
      mockState = assocPath(
        ['productFeatures', 'productFeatures', 'automatic-scm-configuration'],
        undefined,
        mockState
      );
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);

      // undefined, false -> false
      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsAutomaticScmConfigurationEnabled(mockState)).toBe(false);
    });
  });

  describe('selectIsScmEnabled', () => {
    it('returns true iff saas-lifecycle-scm-enabled is true, and false otherwise', () => {
      expect(selectIsScmEnabled(mockState)).toBe(false);

      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], false, mockState);
      expect(selectIsScmEnabled(mockState)).toBe(false);

      mockState = assocPath(['productFeatures', 'productFeatures', 'saas-lifecycle-scm-enabled'], true, mockState);
      expect(selectIsScmEnabled(mockState)).toBe(true);
    });
  });

  describe('selectSourceControlOptions', () => {
    it(
      'returns all with the exception of ' +
        'auto remediated PR, manual PR, InnerSource PR, and ssh for git operations when saas-lifecycle-scm-prs-enabled is false',
      () => {
        mockState.productFeatures.productFeatures['saas-lifecycle-scm-prs-enabled'] = false;
        const options = selectTenantScmOptionsTypes(mockState);

        expect(options).toHaveLength(3);

        const optionsIds = options.map((option) => option.id);
        expect(optionsIds).toContain('source-control-pull-request-commenting');
        expect(optionsIds).toContain('source-control-evaluations');
        expect(optionsIds).toContain('automated-commit-feedback');
      }
    );

    it(
      'returns all with the exception of ' +
        'auto remediated PR, manual PR, InnerSource PR, and ssh for git operations when saas-lifecycle-scm-prs-enabled is undefined',
      () => {
        // Don't set the flag, so it defaults to false via propOr
        const options = selectTenantScmOptionsTypes(mockState);

        expect(options).toHaveLength(3);

        const optionsIds = options.map((option) => option.id);
        expect(optionsIds).toContain('source-control-pull-request-commenting');
        expect(optionsIds).toContain('source-control-evaluations');
        expect(optionsIds).toContain('automated-commit-feedback');
      }
    );

    it('returns all options when saas-lifecycle-scm-prs-enabled is true', () => {
      mockState.productFeatures.productFeatures['saas-lifecycle-scm-prs-enabled'] = true;
      const options = selectTenantScmOptionsTypes(mockState);
      expect(options).toHaveLength(7);
      const optionsIds = options.map((option) => option.id);
      expect(optionsIds).toContain('source-control-remediation-pull-requests');
      expect(optionsIds).toContain('source-control-ssh');
      expect(optionsIds).toContain('source-control-pull-request-commenting');
      expect(optionsIds).toContain('source-control-evaluations');
      expect(optionsIds).toContain('automated-commit-feedback');
      expect(optionsIds).toContain('manual-pull-requests');
      expect(optionsIds).toContain('inner-source-automated-updates');
    });
  });

  describe('selectIsSAMLEnabled', () => {
    it('returns true if selectIsSAMLEnabled is true, and false otherwise', () => {
      expect(selectIsSAMLEnabled(mockState)).toBe(false);

      mockState = assocPath(['productFeatures', 'productFeatures', 'saml-enabled'], false, mockState);
      expect(selectIsSAMLEnabled(mockState)).toBe(false);

      mockState = assocPath(['productFeatures', 'productFeatures', 'saml-enabled'], true, mockState);
      expect(selectIsSAMLEnabled(mockState)).toBe(true);
    });
  });

  describe('selectIsUserManagementPagesEnabled', () => {
    it('returns true if user-management-pages enabled', () => {
      mockState.productFeatures.productFeatures['user-management-pages'] = true;
      expect(selectIsUserManagementPagesEnabled(mockState)).toBe(true);
    });

    it('returns false if user-management-pages disabled', () => {
      mockState.productFeatures.productFeatures['user-management-pages'] = false;
      expect(selectIsUserManagementPagesEnabled(mockState)).toBe(false);
    });

    it('returns false if user-management-pages not present', () => {
      expect(selectIsUserManagementPagesEnabled(mockState)).toBe(false);
    });
  });

  describe('selectIsGithubAppAuthenticationEnabled', () => {
    it('returns true if github-app-authentication enabled', () => {
      mockState.productFeatures.productFeatures['github-app-authentication'] = true;
      expect(selectIsGithubAppAuthenticationEnabled(mockState)).toBe(true);
    });

    it('returns false if github-app-authentication disabled', () => {
      mockState.productFeatures.productFeatures['github-app-authentication'] = false;
      expect(selectIsGithubAppAuthenticationEnabled(mockState)).toBe(false);
    });

    it('returns false if github-app-authentication not present', () => {
      expect(selectIsGithubAppAuthenticationEnabled(mockState)).toBe(false);
    });
  });
});
