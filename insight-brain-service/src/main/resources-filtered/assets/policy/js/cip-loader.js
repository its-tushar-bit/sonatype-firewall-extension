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
            'path' : '../brain/'
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
        $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', CLM.path + style + '?' + clmBuildTimestamp).appendTo(head);
    });
}());
