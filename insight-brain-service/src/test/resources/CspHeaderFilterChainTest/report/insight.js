/**
 * @license Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, window, Insight */
/*jslint plusplus:true */
(function () {
	"use strict";

	$.extend(true, window, {
		'Insight' : {
			'isReadOnly' : function () {
				return Insight.util.getQueryParameter('readonly') !== 'false';
			},
			'toBrain' : function (url) {
				if (url.charAt(0) === '/') {
					return '../brain' + url;
				}
				return '../brain/' + url;
			}
		}
	});
}());

(function () {
	"use strict";

	function isNullOrUndefined(obj) {
		return obj === null || typeof obj === 'undefined';
	}

	function isNotNullOrUndefined(obj) {
		return !isNullOrUndefined(obj);
	}

	function getQueryParameter(name) {
		var search = window.location.search.length > 0 ? window.location.search.substring(1).split('&') : [],
			data = null,
			i = null;
		name = name.toLowerCase();

		$.each(search, function (index, item) {
		});
		for (i = 0; i < search.length; i++) {
			data = search[i].split('=');
			if (data[0].toLowerCase() === name) {
				return data[1];
			}
		}
	}

	$.extend(true, window, {
		'Insight' : {
			'util' : {
				'getQueryParameter' : getQueryParameter,
				'isNullOrUndefined' : isNullOrUndefined,
				'isNotNullOrUndefined' : isNotNullOrUndefined
			}
		}
	});
}());

// Brain client for backwards compatibility with Brain server 1.0
(function () {
	"use strict";
	var features = {};

	if (Insight.util.isNullOrUndefined(window.Brain)) {
		$.getJSON('../brain/rest/features').success(function (data) {
			$.each(data, function (key, item) {
				features[item.toLowerCase()] = true;
			});
		});

		$.extend(true, window, {
			'Brain' : {
				"hasFeature" : function (feature) {
				    return features[feature.toLowerCase()] === true;
				},
				'getVersion' : function () {
					return false;
				}
			}
		});
	}
}());