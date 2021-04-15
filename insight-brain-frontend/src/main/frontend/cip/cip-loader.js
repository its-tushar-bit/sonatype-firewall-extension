/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import pendoModule from './pendo/module';
import isIqIframe from '../util/isIqFrame';

/* global $, window, CLM, document, Insight, angular, Base64, Brain */
(function () {
  'use strict';

  function getBaseUrl() {
    var idx = window.location.href.indexOf('/rest/report/');
    if (idx === -1) {
      idx = window.location.href.indexOf('/audit-report/');
    }
    return window.location.href.substring(0, idx + 1);
  }

  $.extend(true, window, {
    CLM: {
      path: getBaseUrl(),
      assetsPath: getBaseUrl() + 'assets/',
      loadPlugin: (function () {
        var pluginsMap = null;

        function getPluginMap() {
          if (pluginsMap === null) {
            pluginsMap = {};
            $.each(Insight.InformationPanelPlugins, function (index, Plugin) {
              pluginsMap[new Plugin(null, {}).getTitle()] = index;
            });
          }
          return pluginsMap;
        }

        return function (createPluginFn, tabName) {
          function check() {
            if (Insight && Insight.InformationPanelPlugins) {
              var plugin = createPluginFn();
              if (tabName) {
                if (getPluginMap().hasOwnProperty(tabName)) {
                  Insight.InformationPanelPlugins[getPluginMap()[tabName]] = plugin;
                  return;
                }
              }
              Insight.InformationPanelPlugins.push(plugin);
            } else {
              setTimeout(check, 50);
            }
          }

          check();
        };
      })(),
    },
  });

  // CLM-5267 - fixes an issue w/ existing reports and CSRF protection
  $(document).ajaxSend(function (e, jqXHR, options) {
    if (options.type !== 'GET') {
      var headers = Brain.getCsrfHeaders();
      $.each(headers, function (headerName, headerValue) {
        jqXHR.setRequestHeader(headerName, headerValue);
      });
    }
  });

  function createApplicationIdProvider() {
    angular
      .module('ApplicationIdProvider', ['ui.bootstrap'])
      .service('ApplicationId', function () {
        // TODO Are ui-router parameters encoded or decoded?
        return {
          encoded: function () {
            return applicationId;
          },
        };
      })
      .service('OrganizationId', function () {
        return {
          encoded: function () {
            return null;
          },
        };
      })
      .directive('disablenav', function () {
        return function (scope, element, attrs) {
          element.bind('keydown.nav', function (e) {
            // 9 is tab, others are arrow keys
            if (e.keyCode == 9 || (e.keyCode >= 37 && e.keyCode <= 40)) {
              e.stopPropagation();
            }
          });
        };
      });
  }

  function applyHttpOverride() {
    var modalDiv = null;
    var requestQueue = [];

    // override the ajax method and inject our own handling
    var oldAjax = $.ajax;
    $.ajax = function () {
      // used to resend failed requests
      var options = arguments[0];
      // our own deferred that will be returned, to force the callers to wait on our authentication logic
      var deferred = $.Deferred();
      // context to resolve/reject with
      var context = this;

      // use the original ajax call
      oldAjax.apply(context, Array.prototype.slice.apply(arguments)).then(
        function () {
          // success, nothing funky to do, just resolve
          deferred.resolveWith(context, Array.prototype.slice.apply(arguments));
        },
        function (jqXHR) {
          // 401 error, time to force them to login
          if (jqXHR.status === 401) {
            if (isIqIframe(window)) {
              // signal to the SessionSecurityService in the IQ UI that the session seems to have expired
              window.top.sessionExpired();
            } else {
              window.location.reload();
            }
          } else {
            // non auth error, again nothing funky, just reject
            deferred.rejectWith(context, Array.prototype.slice.apply(arguments));
          }
        }
      );

      // make sure to setup these mappings, just as is done in the jquery sources
      deferred.success = deferred.done;
      deferred.error = deferred.fail;
      return deferred;
    };
  }

  function startPendo(pendoService) {
    pendoService.start();
  }
  startPendo.$inject = ['pendoService'];

  const injector = angular.bootstrap(null, [pendoModule.name]);
  injector.invoke(startPendo);

  var head = $('head'),
    styles = ['cip-loader.css'];

  applyHttpOverride();

  createApplicationIdProvider();

  $.each(styles, function (key, style) {
    var url = CLM.assetsPath + style + '?' + clmBuildTimestamp;
    if (document.createStyleSheet) {
      // Note at most 31 stylesheets can be loaded this way
      document.createStyleSheet(url);
    } else {
      $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', url).appendTo(head);
    }
  });
})();
