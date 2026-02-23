/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import { Messages } from './util/CommonServices';
import store from './reduxConfig/store';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { setError } from 'MainRoot/session/appErrorSlice';
import {
  selectHasLifecycleLicense,
  selectIsSbomManagerOnlyLicense,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectIsFirewallSupportedForNavigationContainer,
  selectIsDashboardSupported,
  selectIsFirewallSupported,
  selectIsReportListSupported,
  selectIsSbomManagerEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { unwrapResult } from '@reduxjs/toolkit';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import { selectUnconfiguredRepoManager } from 'MainRoot/firewallOnboarding/firewallOnboardingSelectors';
import { actions as firewallOnboardingActions } from 'MainRoot/firewallOnboarding/firewallOnboardingSlice';
import { waitForLogin } from 'MainRoot/user/userSessionUtils';

// Root route with redirect logic based on product features
router.stateRegistry.register({
  name: 'root',
  url: '^',
  redirectTo: function (transition) {
    return Promise.all([
      store.dispatch(actions.fetchProductFeaturesIfNeeded()).then(unwrapResult),
      store.dispatch(loadProductLicense()).then(unwrapResult),
      // This call will return a 403 if the user doesn't have adequate permissions, but we should
      // ignore that error and just use the unconfiguredRepoManager value from state (which will
      // remain null/undefined on error). Don't unwrap this result to avoid blocking redirect.
      store.dispatch(firewallOnboardingActions.loadUnconfiguredRepoManagers()),
      waitForLogin(),
    ])
      .then(() => {
        const state = store.getState();
        const hasLifecycleLicense = selectHasLifecycleLicense(state);
        const isDashboardAvailable = selectIsDashboardSupported(state);
        const isFirewallAvailable = selectIsFirewallSupported(state);
        const isFirewallEnabled = selectIsFirewallSupportedForNavigationContainer(state);
        const isReportsListAvailable = selectIsReportListSupported(state);
        const isSbomManagerEnabled = selectIsSbomManagerEnabled(state);
        const isSbomManagerOnlyLicense = selectIsSbomManagerOnlyLicense(state);
        const unconfiguredRepoManager = selectUnconfiguredRepoManager(state);

        if (isSbomManagerEnabled && isSbomManagerOnlyLicense) {
          return 'sbomManager.dashboard';
        } else if (isFirewallAvailable && unconfiguredRepoManager && isFirewallEnabled) {
          return 'firewallOnboarding.firewallOnboardingPage';
        } else if (isDashboardAvailable) {
          return 'dashboard.overview.violations';
        } else if (!isDashboardAvailable && isReportsListAvailable && hasLifecycleLicense) {
          return 'violations'; //Landing page is reports page if dashboard is not available for LC
        } else if (isFirewallAvailable) {
          return 'firewall.firewallPage';
        } else if (isReportsListAvailable) {
          return 'violations';
        }

        return 'gettingStarted';
      })
      .catch((err) => {
        const currentTransition = transition.router.globals.transition;
        const isStillActive = currentTransition === transition;

        // Only show error if this transition is still active (hasn't been superseded).
        // Superceding can happen here if user clicks Vulnerability Lookup link on login modal
        if (isStillActive) {
          store.dispatch(setError(Messages.getHttpErrorMessage(err)));
        }
      });
  },
});

// Home route redirects to root
router.stateRegistry.register({
  name: 'home',
  url: '/',
  redirectTo: 'root',
});

// See CLM-34076. Some customers want a way to get to the local login page even if they have
// SSO exclusively enabled, as a recovery option in case of SSO misconfiguration. LoginModalService
// has special case handling for this state.
router.stateRegistry.register({
  name: 'backupLogin',
  url: '/backupLogin',
  redirectTo: 'root',
});
