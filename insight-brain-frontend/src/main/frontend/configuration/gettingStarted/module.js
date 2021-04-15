/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import reduxConfigModule from '../../reduxConfig/module';
import gettingStarted from './gettingStarted';
import productLicenseSummary from './productLicenseSummary/productLicenseSummary';
import systemSetup from './systemSetup/systemSetup';
import learningTopics from './learningTopics/learningTopics';
import gettingStartedDocLink from './gettingStartedDocLink/gettingStartedDocLink';
import PermissionServiceModule from '../../util/PermissionService';
import CLMLocationModule from '../../util/CLMLocation';
import telemetryServiceModule from '../../services/telemetryService';
import componentsModule from '../../components/module';
import gettingStartedUsageTelemetryService from './gettingStartedUsageTelemetryService';

import { DEPARTED_ACTION } from './gettingStartedUsageTelemetryService';

export const GETTING_STARTED_STATE = 'gettingStarted';

export default angular
  .module('gettingStartedModule', [
    'ui.router',
    CLMLocationModule.name,
    'mainHeader',
    PermissionServiceModule.name,
    reduxConfigModule.name,
    telemetryServiceModule.name,
    componentsModule.name,
  ])
  .component('gettingStarted', gettingStarted)
  .component('productLicenseSummary', productLicenseSummary)
  .component('systemSetup', systemSetup)
  .component('learningTopics', learningTopics)
  .component('gettingStartedDocLink', gettingStartedDocLink)
  .service(
    'gettingStartedUsageTelemetryService',
    gettingStartedUsageTelemetryService
  )
  .value('routerListener', routerListener) // add to angular so we can test it
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state(GETTING_STARTED_STATE, {
        url: '/gettingStarted',
        component: 'gettingStarted',
        data: {
          title: 'Getting Started',
        },
      });
    },
  ])
  .run(routerListener);

// track transitions from gettingStarted page
function routerListener($transitions, gettingStartedUsageTelemetryService) {
  $transitions.onFinish({ from: GETTING_STARTED_STATE }, (transition) => {
    gettingStartedUsageTelemetryService.submitData(DEPARTED_ACTION, {
      departedTo: transition.to().name,
    });
  });
}

routerListener.$inject = [
  '$transitions',
  'gettingStartedUsageTelemetryService',
];
