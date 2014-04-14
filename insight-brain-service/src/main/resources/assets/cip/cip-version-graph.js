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
          container = $('<div ng-view></div>'),
          me = this;
      me.node.empty();
      container.appendTo(this.node);
      angular.module('componentProvider' + timestamp, []).run(function() {
        Insight.setGav({
          appId : applicationId,
          groupId : me.gav.groupId,
          artifactId : me.gav.artifactId,
          version : me.gav.version,
          filename : me.gav.matchState === 'unknown' ? me.gav.coordinates : null,
          hash : me.gav.hash,
          matchState : me.gav.matchState,
          proprietary : me.gav.proprietary
        });
      });
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