/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
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
import AdvancedSearchConfigContainer from './advancedSearch/AdvancedSearchConfigContainer';
import SuccessMetricsConfiguration from 'MainRoot/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';
import SystemNoticeConfigurationContainer from './systemNoticeConfiguration/SystemNoticeConfigurationContainer';
import AutomaticApplicationsConfiguration from './automaticApplicationsConfiguration/AutomaticApplicationsConfigurationContainer';
import AdministratorsConfig from './administrators/config/AdministratorsConfig';
import AdministratorsEdit from './administrators/edit/AdministratorsEdit';
import GettingStartedContainer from './gettingStarted/GettingStartedContainer';
import WaivedComponentUpgradesConfiguration from './waivedComponentUpgradesConfiguration/WaivedComponentUpgradesConfiguration';
import RoiConfigurationPage from './roiConfiguration/RoiConfigurationPage';
import EditRoiConfigurationPage from './editRoiConfiguration/EditRoiConfigurationPage';
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
    iqReact2Angular(AutomaticApplicationsConfiguration, [], ['$ngRedux', '$state'])
  )
  .component(
    'automaticSourceControlConfiguration',
    iqReact2Angular(AutomaticSourceControlConfigurationContainer, [], ['$ngRedux', '$state'])
  )
  .component('mailConfig', iqReact2Angular(MailConfigContainer, ['isAuthorized'], ['$ngRedux']))
  .component('proxyConfig', iqReact2Angular(ProxyConfigContainer, [], ['$ngRedux', '$state']))
  .component('advancedSearchConfig', iqReact2Angular(AdvancedSearchConfigContainer, ['isAuthorized'], ['$ngRedux']))
  .component('gettingStarted', iqReact2Angular(GettingStartedContainer, [], ['$ngRedux']))
  .component('scmOnboarding', iqReact2Angular(ScmOnboardingContainer, [], ['$ngRedux', '$state']))
  .component('labsDataInsights', iqReact2Angular(LabsDataInsightsContainer, ['isAuthorized'], ['$ngRedux']))
  .component('successMetricsConfiguration', iqReact2Angular(SuccessMetricsConfiguration, [], ['$ngRedux']))
  .component(
    'waivedComponentUpgradesConfiguration',
    iqReact2Angular(WaivedComponentUpgradesConfiguration, [], ['$ngRedux', '$state'])
  )
  .component('systemNoticeConfiguration', iqReact2Angular(SystemNoticeConfigurationContainer, [], ['$ngRedux']))
  .component('administratorsConfig', iqReact2Angular(AdministratorsConfig, [], ['$ngRedux']))
  .component('administratorsEdit', iqReact2Angular(AdministratorsEdit, [], ['$ngRedux', '$state']))
  .component('roiConfiguration', iqReact2Angular(RoiConfigurationPage, [], ['$ngRedux', '$state']))
  .component('editRoiConfiguration', iqReact2Angular(EditRoiConfigurationPage, [], ['$ngRedux', '$state']))
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
  };

  $stateProvider
    .state('dataInsights', {
      component: 'labsDataInsights',
      url: '/dataInsights',
      data: {
        title: 'Data Insights',
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
    .state('waivedComponentUpgradesConfiguration', {
      component: 'waivedComponentUpgradesConfiguration',
      url: '/waivedComponentUpgradesConfiguration',
      data: {
        title: 'Success Metrics',
        isDirty: ['waivedComponentUpgradesConfiguration', 'isDirty'],
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
    })
    .state('roiConfiguration', {
      component: 'roiConfiguration',
      url: '/roiConfiguration',
      data: {
        title: 'ROI Configuration',
      },
    })
    .state('editRoiConfiguration', {
      component: 'editRoiConfiguration',
      url: '/roiConfiguration/edit',
      data: {
        title: 'Edit ROI Configuration',
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
