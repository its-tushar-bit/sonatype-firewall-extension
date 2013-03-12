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
				string += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(obj[field]);
			}
			return string.substring(1);
		};

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
				return '/rest/ci/component/details/' + arg.appId + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId, ts : new Date().getTime() });
			},
			/**
			 * Get the URL for all versions from a specific GAV. (Used to generate the versions graph in the CIP)
			 * @since version 1.2
			 */
			'getArtifactVersionInfoUrl' : function (arg) {
				return '/rest/ci/component/details/versions/' + arg.appId + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });
			}
		},
		'ide' : {
			/**
			 * Get the URL for a specific GAV. (Used to generate the table in the CIP)
			 * @since version 1.2
			 */
			'getArtifactInfoUrl' : function (arg) {
				return '/rest/ide/component/details/' + arg.appId + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId, ts : new Date().getTime() });
			},
			/**
			 * Get the URL for all versions from a specific GAV. (Used to generate the versions graph in the CIP)
			 * @since version 1.2
			 */
			'getArtifactVersionInfoUrl' : function (arg) {
				return '/rest/ide/component/details/versions/' + arg.appId + '?' + param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });
			}
		}
	};
}());