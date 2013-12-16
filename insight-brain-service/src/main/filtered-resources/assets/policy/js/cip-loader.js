/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global $, window, CLM, document */
(function() {
  'use strict';

  $.extend(true, window, {
    'CLM': {
      'path': '../brain/',
      'loadPlugin': (function() {
        var pluginsMap = null;

        function getPluginMap() {
          if (pluginsMap === null) {
            pluginsMap = {};
            $.each(Insight.InformationPanelPlugins, function(index, Plugin) {
              pluginsMap[new Plugin(null, {}).getTitle()] = index;
            });
          }
          return pluginsMap;
        }

        return function(createPluginFn, tabName) {
          function check() {
            if (Insight && Insight.InformationPanelPlugins) {
              var plugin = createPluginFn();
              if (tabName) {
                var index = getPluginMap()[tabName];
                if (index) {
                  Insight.InformationPanelPlugins[index] = plugin;
                  return;
                }
              }
              Insight.InformationPanelPlugins.push(plugin);
            }
            else {
              setTimeout(check, 50);
            }
          }

          check();
        };
      }())
    }
  });

  function loadScript(key, scriptSrc, onLoad) {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = CLM.path + scriptSrc + '?' + clmBuildTimestamp;
    $('head')[0].appendChild(script);
    if (onLoad) {
      script.onload = script.onreadystatechange = function() {
        if (!script.readyState || (script.readyState === 'complete' || script.readyState === 'loaded')) {
          script.onload = script.onreadystatechange = null;
          onLoad();
        }
      };
    }
  }

  function createApplicationIdProvider() {
    angular.module('ApplicationIdProvider', []).service('ApplicationId',function() {
      // TODO Are ui-router parameters encoded or decoded?
      return {
        encoded: function() {
          return applicationId;
        }
      };
    }).service('OrganizationId',function() {
          return {
            encoded: function() {
              return null;
            }
          };
        }).directive('disablenav', function() {
          return function(scope, element, attrs) {
            element.bind("keydown.nav", function(e) {
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
    $.ajax = function() {
      // used to resend failed requests
      var options = arguments[0];
      // our own deferred that will be returned, to force the callers to wait on our authentication logic
      var deferred = $.Deferred();
      // context to resolve/reject with
      var context = this;
      
      function getBaseUrl() {
        var idx = window.location.href.indexOf('/rest/report/');
        
        if (idx > -1) {
          return window.location.href.substring(0, idx + 1);
        } else {
          return '../../../../../';
        }
      }

      // use the original ajax call
      oldAjax.apply(context, Array.prototype.slice.apply(arguments)).then(
              function() {
                // success, nothing funky to do, just resolve
                deferred.resolveWith(context, Array.prototype.slice.apply(arguments));
              },
              function(jqXHR, textStatus, errorThrown) {
                // 401 error, time to force them to login
                if (jqXHR.status === 401) {
                  // put the request in the queue, as multiple requests may be sent simultaneously
                  requestQueue.push(function() {
                    oldAjax(options).then(function() {
                      deferred.resolveWith(context, Array.prototype.slice.apply(arguments));
                    }, function() {
                      deferred.rejectWith(context, Array.prototype.slice.apply(arguments));
                    });
                  });

                  if (!modalDiv) {
                    modalDiv = $(
                            '<div class="modal" id="loginModal">'
                                    + '<form name="loginForm" class="form-horizontal" style="margin-bottom:0px;">'
                                    + '<div class="modal-header">' + '<h3>User Login</h3>' + '</div>'
                                    + '<div class="modal-body">' + '<div class="control-group">'
                                    + '<label class="control-label" for="login-username">Username</label>'
                                    + '<div class="controls">'
                                    + '<input type="text" id="login-username" placeholder="Enter Username">' + '</div>'
                                    + '</div>' + '<div class="control-group">'
                                    + '<label class="control-label" for="login-password">Password</label>'
                                    + '<div class="controls">'
                                    + '<input type="password" id="login-password" placeholder="Enter Password">'
                                    + '</div>' + '</div>' + '</div>' + '<div class="modal-footer">'
                                    + '<span id="login-error" class="alert alert-error" '
                                    + 'style="margin-right: 10px; display: none;"/>'
                                    + '<button id="login-action" class="btn btn-primary">Sign in</button>' + '</div>'
                                    + '</form>' + '</div>').appendTo('body');
                    modalDiv.modal({
                      backdrop: 'static',
                      keyboard: false
                    });
                  } else {
                    modalDiv.modal('show');
                  }

                  $("#login-action").on('click', function(event) {
                    event.preventDefault();
                    $('#login-error').hide();
                    // do the login with the original ajax, so we don't hit our code here
                    oldAjax({
                      url: getBaseUrl() + 'rest/user/session',
                      type: 'POST',
                      headers: {
                        'Authorization': 'Basic ' + Base64.encode($('#login-username').val() + ':' + $('#login-password').val())
                      }
                    }).then(function() {
                      // login success, go ahead and resend each of the requests
                      $.each(requestQueue, function(index, requestFn) {
                        requestFn();
                      });

                      // clean up
                      requestQueue = [];
                      modalDiv.modal('hide');
                      $('#login-username').val('');
                      $('#login-password').val('');
                    }, function() {
                      $('#login-error').text('Invalid credentials. Please try again.');
                      $('#login-error').show();
                    });
                  });
                } else {
                  // non auth error, again nothing funky, just reject
                  deferred.rejectWith(context, Array.prototype.slice.apply(arguments));
                }
              });

      // make sure to setup these mappings, just as is done in the jquery sources
      deferred.success = deferred.done;
      deferred.error = deferred.fail;
      return deferred;
    };
  }

  var head = $('head'),
       scripts = ['assets/lib/datepicker/bootstrap-datepicker.js',
        'assets/lib/ui-bootstrap-tpls-0.6.0.min.js', 'assets/lib/Base64.js', 'assets/util/HttpInterceptors.js',
        'assets/util/CLMLocation.js', 'cip/cip-label-editor.js',
        'cip/cip-policy-violations.js', 'cip/cip-claim-component.js', 'cip/cip-license-editor.js',
        'assets/util/AngularCommon.js'],
      styles = ['assets/lib/datepicker/datepicker.css', 'cip/cip.css'],
      clmBuildTimestamp = '${build.timestamp}';
  
  applyHttpOverride();

  if (!window.angular) {
    loadScript(null, 'assets/lib/angular/angular-${angularjs.version}.min.js', function() {
      createApplicationIdProvider();
      $.each(scripts, loadScript);
    });
  }
  else {
    createApplicationIdProvider();
    $.each(scripts, loadScript);
  }

  $.each(styles, function(key, style) {
    var url = CLM.path + style + '?' + clmBuildTimestamp;
    if (document.createStyleSheet) {
      // Note at most 31 stylesheets can be loaded this way
      document.createStyleSheet(url);
    }
    else {
      $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', url).appendTo(head);
    }
  });
}());
