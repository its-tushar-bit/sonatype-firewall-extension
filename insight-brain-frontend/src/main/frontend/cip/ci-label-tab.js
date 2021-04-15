/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legacyConfigurationModule from '../LegacyConfigurationModule';
import cipLabelEditorModule from './cip.label.editor/cip.label.editor.module';
/*global angular, $, CLM, Insight, applicationId */
(function () {
  'use strict';

  function LabelTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    LabelTab.prototype = new Insight.InformationPanelPlugin({ priority: 112 });

    LabelTab.prototype.isVisible = function () {
      return (this.component || this.gav).matchState !== 'unknown';
    };

    LabelTab.prototype.create = function () {
      var timestamp = new Date().getTime(),
        container = $('<div cip-label-editor></div>'),
        me = this;

      me.node.empty();
      container.appendTo(this.node);
      angular
        .module('componentProvider' + timestamp, [])
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
        cipLabelEditorModule.name,
        'componentProvider' + timestamp,
        'AngularCommon',
        'ui.bootstrap',
        legacyConfigurationModule.name,
      ]);
    };

    LabelTab.prototype.destroy = function () {
      this.node.empty();
    };

    LabelTab.prototype.getTitle = function () {
      return 'Labels';
    };

    return LabelTab;
  }

  CLM.loadPlugin(createPlugin, 'Labels');
})();
