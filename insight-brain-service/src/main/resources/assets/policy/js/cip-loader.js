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
		script.src = CLM.path + scriptSrc;
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

    var head = $('head'),
        scripts = [ 'assets/bootstrap/bootstrap-datepicker.js', 'policy-assets/js/cip-label-editor.js', 'policy-assets/js/cip-policy-violations.js', 'policy-assets/js/cip-claim-component.js', 'assets/js/Hudson.js', 'assets/CLMLocation.js' ],
        styles = [ 'assets/bootstrap/datepicker.css', 'policy-assets/css/cip-label-editor.css', 'policy-assets/css/cip-policy-violations.css', 'policy-assets/css/cip-claim-component.css' ];

	if (!window.angular) {
		loadScript(null, 'assets/angular/angular-1.0.6.min.js', function () {
			$.each(scripts, loadScript);
		});
	} else {
		$.each(scripts, loadScript);
	}

    $.each(styles, function(key, style) {
        $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', CLM.path + style).appendTo(head);
    });
}());