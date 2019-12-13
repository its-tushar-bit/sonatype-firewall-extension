/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, angularDebug*/

import legacyConfigurationModule from '../../LegacyConfigurationModule';
import auditReportPendoModule from '../pendo/module';
import componentInformationPanelModule from '../cip/component.information.panel.module';
import componentsModule from '../../components/module';

import auditSummaryController from './audit.summary.controller';
import auditThreatDirective from './audit.threat.directive';
import ownerContextService from './owner.context.service';
import repositoryViolationTableFilterDirective from './repository.violation.table.filter.directive';
import componentUpdateController from './component.update.controller';
import componentUpdateOptionalController from './component.update.optional.controller';
import componentUpdateService from './component.update.service';

window.CLM = {
  path: '../../',
  assetsPath : '../'
};

(function () {
  'use strict';
  
  function init($rootScope, ComponentUpdateService, pendoService) {
    $rootScope.$on('reevaluate.component', function (event, componentKey) {
      ComponentUpdateService.reevaluate(componentKey, true);
    });
    $rootScope.$on('reload.component', function (event, componentKey) {
      ComponentUpdateService.reevaluate(componentKey, false);
    });
    pendoService.start();
  }
  init.$inject = ['$rootScope', 'component.update.service', 'pendoService'];

  function config($compileProvider) {
    $compileProvider.debugInfoEnabled(angularDebug);
  }
  config.$inject = ['$compileProvider'];

  angular.module('audit',
      ['AngularCommon', 'UnauthenticatedResponseHttpInterceptor', 'ui.bootstrap', 'CLMLocation',
        auditReportPendoModule.name, componentInformationPanelModule.name, legacyConfigurationModule.name,
        componentsModule.name])
      .controller('audit.summary.controller', auditSummaryController)
      .directive('auditThreat', auditThreatDirective)
      .service('OwnerContext', ownerContextService)
      .directive('repositoryViolationTableFilter', repositoryViolationTableFilterDirective)
      .controller('component.update.controller', componentUpdateController)
      .controller('component.update.optional.controller', componentUpdateOptionalController)
      .service('component.update.service', componentUpdateService)
      .run(init)
      .config(config);
}());
