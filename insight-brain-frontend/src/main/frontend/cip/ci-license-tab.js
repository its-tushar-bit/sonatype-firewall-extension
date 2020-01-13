/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legacyConfigurationModule from '../LegacyConfigurationModule';
import cipLicenseEditorModule from './cip.license.editor/cip.license.editor.module';
/*global angular, $, CLM, Insight, applicationId */
(function() {
  'use strict';

  function BrainLicenseEditorTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {

    BrainLicenseEditorTab.prototype = new Insight.InformationPanelPlugin({ priority: 80 });

    BrainLicenseEditorTab.prototype.destroy = function() {
      if (this.node) {
        this.node.empty();
      }
    };
    BrainLicenseEditorTab.prototype.getTitle = function() {
      return 'Licenses';
    };
    BrainLicenseEditorTab.prototype.isVisible = function() {
      return this.gav.matchState !== 'unknown';
    };

    BrainLicenseEditorTab.prototype.create = function() {
      var timestamp = new Date().getTime(),
          container = $('<div cip-license-editor></div>'),
          me = this;

      me.node.empty();
      container.appendTo(this.node);

      angular.module('componentProvider' + timestamp, ['ComponentUtils']).run(['$rootScope', function ($rootScope) {
        $rootScope.$on('clm.grid.licenses.changed', function (e, component) {
          // Update Grid
          me.grid.getData().updateItem(component.id, component);
          // Update Summary Page
          Insight.updateSummary();
        });
      }]).service('SelectedComponent', ['ComponentUtil', function(ComponentUtil) {
        return {
          get: function () {
            var component = me.component || me.gav;
            ComponentUtil.enhanceWithComponentIdentifier(component);
            return component;
          }
        };
      }]).service('OwnerContext', function () {
        return {
          ownerType: 'application',
          ownerId: applicationId,
          scanId: window.reportId
        };
      });

      angular.bootstrap(container[0], [cipLicenseEditorModule.name, 'componentProvider' + timestamp,
          legacyConfigurationModule.name]);
    };

    return BrainLicenseEditorTab;
  }

  CLM.loadPlugin(createPlugin, 'Edit License');
}());
