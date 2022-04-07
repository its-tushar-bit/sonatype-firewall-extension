/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legacyConfigurationModule from '../LegacyConfigurationModule';
import cipPolicyViolationsModule from './cip.policy.violations/cip.policy.violations.module';
/*global angular, $, CLM, Insight, applicationId */
(function () {
  'use strict';

  function PolicyViolationTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    PolicyViolationTab.prototype = new Insight.InformationPanelPlugin({
      priority: 32,
    });

    PolicyViolationTab.prototype.create = function () {
      var timestamp = new Date().getTime(),
        container = $('<div cip-policy-violations></div>'),
        me = this;

      me.node.empty();

      container.appendTo(me.node);

      angular
        .module('policyViolations' + timestamp, [])
        .service('SelectedComponent', function () {
          return {
            get: function () {
              return me.component || me.gav;
            },
          };
        })
        .service('OwnerContext', function () {
          return {
            ownerType: 'application',
            ownerId: applicationId,
          };
        });
      angular.bootstrap(container[0], [
        cipPolicyViolationsModule.name,
        'policyViolations' + timestamp,
        'AngularCommon',
        legacyConfigurationModule.name,
      ]);
    };

    PolicyViolationTab.prototype.destroy = function () {
      this.node.empty();
    };

    PolicyViolationTab.prototype.getTitle = function () {
      return 'Policy';
    };

    return PolicyViolationTab;
  }

  CLM.loadPlugin(createPlugin, 'Policy');
})();
