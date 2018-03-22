/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from './gettingStarted/module';
import successMetricsConfigurationModule from './successMetricsConfiguration/successMetricsConfigurationModule';
import automaticApplicationsConfigurationModule from './automaticApplicationsConfiguration/automaticApplicationsConfigurationModule';

export default angular.module('configurationModule', [
  gettingStartedModule.name, successMetricsConfigurationModule.name, automaticApplicationsConfigurationModule.name
]);
