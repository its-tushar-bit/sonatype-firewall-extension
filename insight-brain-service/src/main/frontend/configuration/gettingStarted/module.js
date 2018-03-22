/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import gettingStarted from './gettingStarted';
import productLicenseSummary from './productLicenseSummary/productLicenseSummary';
import systemSetup from './systemSetup/systemSetup';
import learningTopics from './learningTopics/learningTopics';
import gettingStartedDocLink from './gettingStartedDocLink/gettingStartedDocLink';
import PermissionServiceModule from '../../util/PermissionService';

export default angular.module('gettingStartedModule',
    ['ui.router', 'CLMLocation', 'mainHeader', PermissionServiceModule.name])
    .component('gettingStarted', gettingStarted)
    .component('productLicenseSummary', productLicenseSummary)
    .component('systemSetup', systemSetup)
    .component('learningTopics', learningTopics)
    .component('gettingStartedDocLink', gettingStartedDocLink)
    .config([
      '$stateProvider', function($stateProvider) {
        $stateProvider.state('gettingStarted', {
          url: '/gettingStarted',
          component: 'gettingStarted',
          data: {
            title: 'Getting Started'
          }
        });
      }
    ]);
