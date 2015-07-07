/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, angular, applicationId, Insight, CLM */
(function () {
  'use strict';

  function VersionGraphTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    VersionGraphTab.prototype = new Insight.InformationPanelPlugin({ priority: 1 });
    VersionGraphTab.prototype.getTitle = function() {
      return 'Component Info';
    };
    VersionGraphTab.prototype.destroy = function() {
      this.node.empty();
    };
    VersionGraphTab.prototype.create = function() {
      var timestamp = new Date().getTime(),
          container = $('<div clm-include="\'' + CLM.path + 'assets/version-graph/version-graph.html\'"></div>'),
          me = this;
      me.node.empty();
      container.appendTo(this.node);
      angular.module('componentProvider' + timestamp, ['ComponentUtils']).run([
        'ComponentUtil', function(ComponentUtil) {
          var component = me.component || me.gav;
          var properties = {
            //legacy coordinates here is the name to display, not componentIdentifier.coordinates
            filename: component.matchState === 'unknown' ? component.coordinates : null,
            hash: component.hash,
            matchState: component.matchState,
            proprietary: component.proprietary,
            appId: applicationId
          };
          ComponentUtil.enhanceWithComponentIdentifier(component);
          if (component.componentIdentifier) {
            Insight.setCoordinates(component.componentIdentifier.format, component.componentIdentifier.coordinates,
              properties);
          }
          else {
            Insight.setCoordinates(null, null, properties);
          }
        }
      ]);
      angular.bootstrap(container[0], ['CIP', 'componentProvider' + timestamp, 'HttpInterceptors',
          'UnauthenticatedResponseHttpInterceptor']);
    };
    return VersionGraphTab;
  }

  window.clmEndpoint = {
    type : 'ci',
    migrate : false,
    selectApplication : false,
    openView : angular.noop,
    linkTarget : '_blank',
    path : CLM.path + 'assets/version-graph/'
  };

  CLM.loadPlugin(createPlugin, 'Component Info');
}());
