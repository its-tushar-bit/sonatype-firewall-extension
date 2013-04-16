/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global window, $ */
/*jslint plusplus:true */
(function () {
	"use strict";

	var features = ["policy", "labels", "release-graph", "policy-violations", "notification"],// Lowercase
		param = window.$ ? $.param : function (obj) {
			var string = '',
				field;
			for (field in obj) {
				string += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(obj[field] == null ? "" : obj[field]);
			}
			return string.substring(1);
		},
		basePath = (function () {
			var scripts = window.document.getElementsByTagName('script'),
				i,
				index;
			if (scripts.length) {
				for (var i = 0; i < scripts.length; i++) {
					if (scripts[i].src) {
						index = scripts[i].src.indexOf('policy-assets/js/brain.client.js');
						if (index == -1) {
							index = scripts[i].src.indexOf('assets/js/brain.client.js');
						}

						if (index != -1) {
							return scripts[i].src.substring(0, index);
						}
					}
				}
			}
			return '/';
		}());

	window.Brain = {
		/**
		 * Check if the Brain instance supports a feature
		 * @since version 1.1
		 */
		"hasFeature" : function (feature) {
			var i;
			feature = feature.toLowerCase();
			for (i = 0; i < features.length; i++) {
				if (feature === features[i]) {
					return true;
				}
			}
			return false;
		},
		/**
		 * Get the Brain's version.
		 * @since version 1.1
		 */
		"getVersion" : function () {
			return "${project.version}";
		},
		'ci' : {
			/**
			 * Get the URL for a specific GAV. (Used to generate the table in the CIP)
			 * @since version 1.2
			 */
			'getArtifactInfoUrl' : function (arg) {
				return '/rest/ci/component/details/' + encodeURIComponent(arg.appId) + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId, ts : new Date().getTime() });
			},
			/**
			 * Get the URL for all versions from a specific GAV. (Used to generate the versions graph in the CIP)
			 * @since version 1.2
			 */
			'getArtifactVersionInfoUrl' : function (arg) {
				return '/rest/ci/component/details/versions/' + encodeURIComponent(arg.appId) + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });
			},
			/**
			 * Get the selectable licenses for a particular gav
			 * @since version 1.4
			 */
			'getArtifactLicensesUrl' : function (arg) {
				return '/rest/ci/component/details/selectableLicenses/' + encodeURIComponent(arg.appId) + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });				 
			 }
		},
		'ide' : {
			/**
			 * Get the URL for a specific GAV. (Used to generate the table in the CIP)
			 * @since version 1.2
			 */
			'getArtifactInfoUrl' : function (arg) {
				return basePath + 'rest/ide/component/details/' + encodeURIComponent(arg.appId) + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId, ts : new Date().getTime() });
			},
			/**
			 * Get the URL for all versions from a specific GAV. (Used to generate the versions graph in the CIP)
			 * @since version 1.2
			 */
			'getArtifactVersionInfoUrl' : function (arg) {
				return basePath + 'rest/ide/component/details/versions/' + encodeURIComponent(arg.appId) + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });
			}
		}
	};
}());