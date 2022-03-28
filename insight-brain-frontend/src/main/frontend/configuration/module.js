/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ldapModule from './ldap/ldap.module';
import samlModule from './saml/module';
import webhookModule from './webhook/webhook.module';
import ProductLicenseModule from './license/ProductLicenseModule';
import MailConfigContainer from './mail/MailConfigContainer';
import AutomaticSourceControlConfigurationContainer from './automaticSourceControlConfiguration/AutomaticSourceControlConfigurationContainer';
import ProxyConfigContainer from './proxy/ProxyConfigContainer';
import ScmOnboardingContainer from './scmOnboarding/ScmOnboardingContainer';
import LabsDataInsightsContainer from './labsDataInsights/LabsDataInsightsContainer';
import scmOnboardingActions from './scmOnboarding/scmOnboardingActions';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import AdvancedSearchConfigContainer from './advancedSearch/AdvancedSearchConfigContainer';
import SuccessMetricsConfigurationContainer from './successMetricsConfiguration/SuccessMetricsConfigurationContainer';
import SystemNoticeConfigurationContainer from './systemNoticeConfiguration/SystemNoticeConfigurationContainer';
import AutomaticApplicationsConfiguration from './automaticApplicationsConfiguration/AutomaticApplicationsConfigurationContainer';
import AdministratorsConfig from './administrators/config/AdministratorsConfig';
import AdministratorsEdit from './administrators/edit/AdministratorsEdit';
import GettingStartedContainer from './gettingStarted/GettingStartedContainer';
import { submitData, DEPARTED_ACTION } from './gettingStarted/gettingStartedTelemetryServiceHelper';

export const GETTING_STARTED_STATE = 'gettingStarted';

export default angular
  .module('configurationModule', [
    ldapModule.name,
    samlModule.name,
    webhookModule.name,
    ProductLicenseModule.name,
    'ngRedux',
  ])
  .component(
    'automaticApplicationsConfiguration',
    react2angular(withStoreProvider(AutomaticApplicationsConfiguration), [], ['$ngRedux', '$state'])
  )
  .component(
    'automaticSourceControlConfiguration',
    react2angular(withStoreProvider(AutomaticSourceControlConfigurationContainer), [], ['$ngRedux', '$state'])
  )
  .component('mailConfig', react2angular(withStoreProvider(MailConfigContainer), ['isAuthorized'], ['$ngRedux']))
  .component(
    'proxyConfig',
    react2angular(withStoreProvider(withRouterStateProvider(ProxyConfigContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'advancedSearchConfig',
    react2angular(withStoreProvider(AdvancedSearchConfigContainer), ['isAuthorized'], ['$ngRedux'])
  )
  .component('gettingStarted', react2angular(withStoreProvider(GettingStartedContainer), [], ['$ngRedux']))
  .component(
    'scmOnboarding',
    react2angular(withStoreProvider(ScmOnboardingContainer), ['isAuthorized'], ['$ngRedux', '$state'])
  )
  .component(
    'labsDataInsights',
    react2angular(withStoreProvider(LabsDataInsightsContainer), ['isAuthorized'], ['$ngRedux'])
  )
  .component(
    'successMetricsConfiguration',
    react2angular(withStoreProvider(SuccessMetricsConfigurationContainer), [], ['$ngRedux'])
  )
  .component(
    'systemNoticeConfiguration',
    react2angular(withStoreProvider(SystemNoticeConfigurationContainer), [], ['$ngRedux'])
  )
  .component('administratorsConfig', react2angular(withStoreProvider(AdministratorsConfig), [], ['$ngRedux']))
  .component(
    'administratorsEdit',
    react2angular(withStoreProvider(withRouterStateProvider(AdministratorsEdit)), [], ['$ngRedux', '$state'])
  )
  .factory('scmOnboardingActions', scmOnboardingActions)
  .value('routerListener', routerListener) // add to angular so we can test it
  .config(routes)
  .run(routerListener);

function routes($stateProvider) {
  const scmOnboardingRouteCommonProps = {
    component: 'scmOnboarding',
    data: {
      title: 'Onboarding',
    },
    resolve: {
      isAuthorized: [
        'PermissionService',
        function (PermissionService) {
          const configPromise = PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          const automationPromise = PermissionService.isAutomationFeatureEnabled();
          return Promise.all([configPromise, automationPromise]).then((data) => {
            return data[0] && data[1];
          });
        },
      ],
    },
  };

  $stateProvider
    .state('dataInsights', {
      component: 'labsDataInsights',
      url: '/dataInsights',
      data: {
        title: 'Data Insights',
      },
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isLabsDataInsightsEnabled();
          },
        ],
      },
    })
    .state('mailConfig', {
      component: 'mailConfig',
      url: '/mailConfig',
      data: {
        title: 'Mail Config',
        isDirty: ['mailConfig', 'isDirty'],
      },
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          },
        ],
      },
    })
    .state('proxyConfig', {
      component: 'proxyConfig',
      url: '/proxyConfig',
      data: {
        title: 'Proxy',
        isDirty: ['proxyConfig', 'isDirty'],
      },
    })
    .state('advancedSearchConfig', {
      component: 'advancedSearchConfig',
      url: '/advancedSearchConfig',
      data: {
        title: 'Advanced Search Config',
        isDirty: ['advancedSearchConfig', 'viewState', 'isDirty'],
      },
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          },
        ],
      },
    })
    .state('successMetricsConfiguration', {
      component: 'successMetricsConfiguration',
      url: '/successMetricsConfiguration',
      data: {
        title: 'Success Metrics',
        isDirty: ['successMetricsConfiguration', 'viewState', 'isDirty'],
      },
    })
    .state('systemNoticeConfiguration', {
      component: 'systemNoticeConfiguration',
      url: '/systemNoticeConfiguration',
      data: {
        title: 'System Notice',
        isDirty: ['systemNoticeConfiguration', 'viewState', 'isDirty'],
      },
    })
    .state('scmOnboarding', {
      ...scmOnboardingRouteCommonProps,
      url: '/onboarding',
    })
    .state('scmOnboardingOrg', {
      ...scmOnboardingRouteCommonProps,
      url: '/onboarding/{organizationId}',
    })
    .state('automaticSourceControlConfiguration', {
      component: 'automaticSourceControlConfiguration',
      url: '/automaticSourceControlConfiguration',
      data: {
        title: 'Automatic Source Control',
        isDirty: ['automaticSourceControlConfiguration', 'viewState', 'isDirty'],
      },
    })
    .state('automaticApplicationsConfiguration', {
      component: 'automaticApplicationsConfiguration',
      url: '/automaticApplicationsConfiguration',
      data: {
        title: 'Automatic Applications',
        isDirty: ['automaticApplicationsConfiguration', 'isDirty'],
      },
    })
    .state('gettingStarted', {
      component: 'gettingStarted',
      url: '/gettingStarted',
      data: {
        title: 'Getting Started',
      },
    });
}

// track transitions from gettingStarted page
function routerListener($transitions) {
  $transitions.onFinish({ from: GETTING_STARTED_STATE }, (transition) => {
    return submitData(DEPARTED_ACTION, {
      departedTo: transition.to().name,
    });
  });
}

routes.$inject = ['$stateProvider'];
routerListener.$inject = ['$transitions'];
