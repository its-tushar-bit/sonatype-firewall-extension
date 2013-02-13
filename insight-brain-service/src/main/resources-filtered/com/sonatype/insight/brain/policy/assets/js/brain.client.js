/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*jslint plusplus:true */
var Brain;
(function () {
	"use strict";

	var features = ["policy", "labels", "release-graph"]; // Lowercase

	Brain = {
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
		"getVersion" : function () {
			return "${project.version}";
		},
		'getArtifactInfoUrl' : function (arg) {
		    return '/rest/ide/artifact/info/' + arg.appId + '?' + $.param({ groupId : arg.groupId, artifactId : arg.artifactId, version : arg.version, instanceId : arg.instanceId });
	    }
	};
}());