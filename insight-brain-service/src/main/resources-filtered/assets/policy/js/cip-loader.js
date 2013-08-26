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
        'CLM' : {
            'path' : '../brain/',
            'loadPlugin' : (function () {
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
                             var index = getPluginMap()[tabName];
                             if (index) {
                                 Insight.InformationPanelPlugins[index] = plugin;
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
            }())
        }
    });

    function loadScript(key, scriptSrc, onLoad) {
		var script = document.createElement('script');
		script.type = 'text/javascript';
		script.src = CLM.path + scriptSrc + '?' + clmBuildTimestamp;
		$('head')[0].appendChild(script);
		if (onLoad) {
			script.onload = script.onreadystatechange = function () {
				if (!script.readyState || (script.readyState === 'complete' || script.readyState === 'loaded')) {
					script.onload = script.onreadystatechange = null;
					onLoad();
				}
			};
		}
	}

	function createApplicationIdProvider() {
		angular.module('ApplicationIdProvider', []).service('ApplicationId', function () {
			// TODO Are ui-router parameters encoded or decoded?
			return {
				encoded: function () {
					return applicationId;
				}
			};
		}).service('OrganizationId', function () {
			return {
				encoded : function () {
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

    var head = $('head'),
        scripts = [ 'assets/lib/datepicker/bootstrap-datepicker.js', 'cip/cip-label-editor.js', 'cip/cip-policy-violations.js', 'cip/cip-claim-component.js', 'cip/cip-license-editor.js', 'assets/js/Hudson.js', 'assets/util/AngularCommon.js' ],
        styles = [ 'assets/lib/datepicker/datepicker.css', 'cip/cip.css' ],
        clmBuildTimestamp = '${build.timestamp}';

	if (!window.angular) {
		loadScript(null, 'assets/lib/angular/angular-${angularjs.version}.min.js', function () {
			createApplicationIdProvider();
			$.each(scripts, loadScript);
		});
	} else {
		createApplicationIdProvider();
		$.each(scripts, loadScript);
	}

    $.each(styles, function(key, style) {
        var url = CLM.path + style + '?' + clmBuildTimestamp;
        if (document.createStyleSheet) {
            // Note at most 31 stylesheets can be loaded this way
            document.createStyleSheet(url);
        } else {
            $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', url).appendTo(head);
        }
    });
}());
