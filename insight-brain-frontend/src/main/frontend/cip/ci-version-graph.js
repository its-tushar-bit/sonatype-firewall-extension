/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legacyConfigurationModule from '../LegacyConfigurationModule';
import versionGraphModule from '../version-graph/version.graph/version.graph.module';
import proprietaryMatchersModule from './proprietary.matchers.modal/proprietary.matchers.module';

/*global $, angular, applicationId, Insight, CLM */
(function () {
  'use strict';

  function VersionGraphTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    VersionGraphTab.prototype = new Insight.InformationPanelPlugin({
      priority: 1,
    });
    VersionGraphTab.prototype.getTitle = function () {
      return 'Component Info';
    };
    VersionGraphTab.prototype.destroy = function () {
      this.node.empty();
    };
    VersionGraphTab.prototype.create = function () {
      var timestamp = new Date().getTime(),
        container = $('<div information-panel></div>'),
        me = this;
      me.node.empty();
      container.appendTo(this.node);
      angular
        .module('componentProvider' + timestamp, ['ComponentUtils'])
        .run([
          'ComponentUtil',
          'Properties',
          'Coordinates',
          function (ComponentUtil, Properties, Coordinates) {
            var component = me.component || me.gav;
            ComponentUtil.enhanceWithComponentIdentifier(component);

            Properties.setHash(component.hash);
            Properties.setFilename(component.matchState === 'unknown' ? component.coordinates : null);
            Properties.setProprietary(component.proprietary || false);
            Properties.setMatchState(component.matchState);
            Coordinates.setIdentificationSource(component.identificationSource);

            if (component.componentIdentifier) {
              Coordinates.set(component.componentIdentifier.format, component.componentIdentifier.coordinates); //coordinates may be null for unknown
            } else {
              Coordinates.set(null, {}); // unknown
            }
          },
        ])
        .service('OwnerContext', function () {
          return {
            ownerType: 'application',
            ownerId: applicationId,
          };
        })
        .service('SelectedComponent', function () {
          return {
            get: function () {
              return me.component || me.gav;
            },
          };
        });
      angular.bootstrap(container[0], [
        versionGraphModule.name,
        'componentProvider' + timestamp,
        'HttpInterceptors',
        'UnauthenticatedResponseHttpInterceptor',
        proprietaryMatchersModule.name,
        legacyConfigurationModule.name,
      ]);
    };
    return VersionGraphTab;
  }

  window.clmEndpoint = {
    type: 'ci',
    migrate: false,
    selectApplication: false,
    openView: angular.noop,
    linkTarget: '_blank',
    path: CLM.assetsPath + '/version-graph/',
    canAddProprietary: true,
  };

  CLM.loadPlugin(createPlugin, 'Component Info');
})();
