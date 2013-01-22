/*global $ */
(function () {
	'use strict';

	$.extend(true, window, {
		'CLM' : {
			'path' : '../brain/'
		}
	});

	var head = $('head'),
	    scripts = ['policy-assets/angular/angular-1.0.3.min.js', 'policy-assets/js/cip-label-editor.js'];

	$.each(scripts, function (key, script) {
		$('<script></script>').attr('src', CLM.path + script).appendTo(head);
	});
}());