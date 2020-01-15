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
import withStoreProvider from '../reactAdapter/StoreProvider';

export default angular.module('configurationModule',
    [
      gettingStartedModule.name, successMetricsConfigurationModule.name, systemNoticeConfigurationModule.name,
      automaticApplicationsConfigurationModule.name, ldapModule.name, samlModule.name, webhookModule.name,
      ProductLicenseModule.name, automaticSourceControlConfigurationModule.name, 'ngRedux'])
    .component('mailConfig', react2angular(withStoreProvider(MailConfigContainer), [], ['$ngRedux']))
    .config(routes);

function routes($stateProvider) {
  $stateProvider
      .state('mailConfig', {
        component: 'mailConfig',
        url: '/mailConfig',
        data: {
          title: 'Mail Config'
        }
      });
}

routes.$inject = ['$stateProvider'];
