/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from './gettingStarted/module';
import successMetricsConfigurationModule from './successMetricsConfiguration/successMetricsConfigurationModule';
import systemNoticeConfigurationModule from './systemNoticeConfiguration/systemNoticeConfigurationModule';
import automaticApplicationsConfigurationModule
  from './automaticApplicationsConfiguration/automaticApplicationsConfigurationModule';
import ldapModule from './ldap/ldap.module';
import samlModule from './saml/module';
import webhookModule from './webhook/webhook.module';
import ProductLicenseModule from './license/ProductLicenseModule';

export default angular.module('configurationModule',
    [
      gettingStartedModule.name, successMetricsConfigurationModule.name, systemNoticeConfigurationModule.name,
      automaticApplicationsConfigurationModule.name, ldapModule.name, samlModule.name, webhookModule.name,
      ProductLicenseModule.name
    ]);
