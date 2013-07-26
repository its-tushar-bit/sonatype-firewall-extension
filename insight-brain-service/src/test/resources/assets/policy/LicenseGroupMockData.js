LicenseGroupMockData = {
		getLicensesData : function() {
			return [{
				"id":"AFL-UNSPECIFIED",
				"shortDisplayName":"AFL",
				"longDisplayName":"AFL-Style License Not Identifiable by Sonatype",
				"description":null,
				"licenseUrl":null,
				"licenseCategoryId":"WEAKCOPYLEFT",
				"unknown":false,
				"unspecified":false
			},{
				"id":"AFL-1.2",
				"shortDisplayName":"AFL-1.2",
				"longDisplayName":"Academic Free License v1.2",
				"description":null,
				"licenseUrl":"http://www.spdx.org/licenses/AFL-1.2",
				"licenseCategoryId":"WEAKCOPYLEFT",
				"unknown":false,
				"unspecified":false
			},{
				"id":"AAL",
				"shortDisplayName":"AAL",
				"longDisplayName":"Attribution Assurance License",
				"description":null,
				"licenseUrl":"http://www.spdx.org/licenses/AAL",
				"licenseCategoryId":null,
				"unknown":false,
				"unspecified":false
			}];
		},		
		getLicenseGroupData : function() {
			return [{
				"id":"8bffe93293fb49a0b6a072909ecfc1e4",
				"applicationId":"ddf27b0554cf4d18ac9fbc110286cc40",
				"name":"Copyleft",
				"threatLevel":9
				}];
		},
		getLicenseGroupLicensesData : function() {
			return [{"id":"6104366387ba4b3993304189aa49fa5f",
			    "ownerId":"beb7d7d7d0d446149af052f412a7693a",
			    "licenseThreatGroupId":"6a45d92848a9462497554939263458ba",
			    "licenseId":"AAL"
			},{"id":"088c47f045f64d29962f05f8b255e96e",
			    "ownerId":"beb7d7d7d0d446149af052f412a7693a",
			    "licenseThreatGroupId":"6a45d92848a9462497554939263458ba",
			    "licenseId":"AFL-UNSPECIFIED"
			}];
		},
		getApplicableLicenseGroupData : function() {
			return {
				licenseThreatGroupsByOwner: [{
					ownerId : "78c1d44c07584e57945f04890c672e82",
					ownerName: "applicationName",
					ownerType : "application"
				}]
			};
		}
};
