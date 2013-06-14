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

    function loadScript(key, scriptSrc) {
		var script = document.createElement('script');
		script.type = 'text/javascript';
		script.src = CLM.path + scriptSrc;
		$('head')[0].appendChild(script);
	}

    var head = $('head'),
        scripts = [ 'policy-assets/js/cip-label-editor.js', 'policy-assets/js/cip-policy-violations.js', 'policy-assets/js/cip-claim-component.js' ],
        styles = [ 'policy-assets/css/cip-label-editor.css', 'policy-assets/css/cip-policy-violations.css' ];

	if (!window.angular) {
		loadScript(null, 'assets/angular/angular-1.0.6.min.js');
	}
	$.each(scripts, loadScript);

    $.each(styles, function(key, style) {
        $('<link></link>').attr('type', 'text/css').attr('rel', 'stylesheet').attr('href', CLM.path + style).appendTo(head);
    });
}());