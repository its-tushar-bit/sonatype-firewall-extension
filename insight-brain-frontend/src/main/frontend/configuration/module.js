/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import gettingStartedModule from './gettingStarted/module';
import successMetricsConfigurationModule from './successMetricsConfiguration/successMetricsConfigurationModule';
import systemNoticeConfigurationModule from './systemNoticeConfiguration/systemNoticeConfigurationModule';
import automaticApplicationsConfigurationModule
  from './automaticApplicationsConfiguration/automaticApplicationsConfigurationModule';
import ldapModule from './ldap/ldap.module';
import samlModule from './saml/module';
import webhookModule from './webhook/webhook.module';
import ProductLicenseModule from './license/ProductLicenseModule';
import automaticSourceControlConfigurationModule
  from './automaticSourceControlConfiguration/automaticSourceControlConfigurationModule';
import MailConfigContainer from './mail/MailConfigContainer';
import ProxyConfigContainer from './proxy/ProxyConfigContainer';
import ScmOnboardingContainer from './scmOnboarding/ScmOnboardingContainer';
import scmOnboardingActions from './scmOnboarding/scmOnboardingActions';
import withStoreProvider from '../reactAdapter/StoreProvider';
import { always } from 'ramda';
import AdvancedSearchConfigContainer from './advancedSearch/AdvancedSearchConfigContainer';

export default angular.module('configurationModule',
    [
      gettingStartedModule.name, successMetricsConfigurationModule.name, systemNoticeConfigurationModule.name,
      automaticApplicationsConfigurationModule.name, ldapModule.name, samlModule.name, webhookModule.name,
      ProductLicenseModule.name, automaticSourceControlConfigurationModule.name, 'ngRedux'])
    .component('mailConfig', react2angular(withStoreProvider(MailConfigContainer), ['isAuthorized'], ['$ngRedux']))
    .component('proxyConfig', react2angular(withStoreProvider(ProxyConfigContainer), ['isAuthorized', 'licensed'],
        ['$ngRedux', '$state']))
    .component('advancedSearchConfig', react2angular(withStoreProvider(AdvancedSearchConfigContainer), ['isAuthorized'],
        ['$ngRedux']))
    .component('scmOnboarding', react2angular(withStoreProvider(ScmOnboardingContainer), ['isAuthorized'],
        ['$ngRedux']))
    .factory('scmOnboardingActions', scmOnboardingActions)
    .config(routes);

function routes($stateProvider) {
  const scmOnboardingRouteCommonProps = {
    component: 'scmOnboarding',
    data: {
      title: 'Onboarding'
    },
    resolve: {
      isAuthorized: [
        'PermissionService', function(PermissionService) {
          return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
        }
      ]
    }
  };

  $stateProvider
      .state('mailConfig', {
        component: 'mailConfig',
        url: '/mailConfig',
        data: {
          title: 'Mail Config'
        },
        resolve: {
          isAuthorized: [
            'PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            }
          ]
        }
      })
      .state('proxyConfig', {
        component: 'proxyConfig',
        url: '/proxyConfig',
        data: {
          title: 'Proxy Config'
        },
        resolve: {
          isAuthorized: [
            'PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            }
          ],
          licensed: [
            'ProductLicense', function(ProductLicense) {
              return ProductLicense.load()
                  .then(always(true))
                  .catch(always(false));
            }
          ]
        }
      })
      .state('advancedSearchConfig', {
        component: 'advancedSearchConfig',
        url: '/advancedSearchConfig',
        data: {
          title: 'Advanced Search Config'
        },
        resolve: {
          isAuthorized: [
            'PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            }
          ]
        }
      })
      .state('scmOnboarding', {
        ...scmOnboardingRouteCommonProps,
        url: '/onboarding'
      })
      .state('scmOnboardingOrg', {
        ...scmOnboardingRouteCommonProps,
        url: '/onboarding/{organizationId}'
      });
}

routes.$inject = ['$stateProvider'];
