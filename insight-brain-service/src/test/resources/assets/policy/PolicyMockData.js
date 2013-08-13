PolicyMockData = {
  getConditionTypeData : function() {
    return [{
          "name" : "Label",
          "id" : "Label",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "LabelValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "License",
          "id" : "License",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "LicenseValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "License Status",
          "id" : "LicenseStatus",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "LicenseStatusValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "Security Vulnerability",
          "id" : "SecurityVulnerability",
          "supportedOperators" : ["present", "absent"],
          "valueTypeId" : null,
          "valueHint" : null
        }, {
          "name" : "Security Vulnerability Severity",
          "id" : "SecurityVulnerabilitySeverity",
          "supportedOperators" : ["=", "<", "<=", ">", ">="],
          "valueTypeId" : "FloatValueType",
          "valueHint" : "Enter value 1 to 10",
          "valueType" : {
            "dataType": "Float"
          }
        }, {
          "name" : "Security Vulnerability Status",
          "id" : "SecurityVulnerabilityStatus",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "SecurityVulnerabilityStatusValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "Relative Popularity (Percentage)",
          "id" : "RelativePopularity",
          "supportedOperators" : ["=", "<", "<=", ">", ">="],
          "valueTypeId" : "FloatValueType",
          "valueHint" : "Enter percent value, 1 to 100",
          "valueType" : {
            "dataType": "Integer"
          }
        }, {
          "name" : "Age",
          "id" : "AgeInDays",
          "supportedOperators" : ["older than", "younger than"],
          "valueTypeId" : "AgeInDaysValueType",
          "valueHint" : "Enter term",
          "valueType" : {
            "dataType": "Integer"
          }
        }, {
          "name" : "Identification Source",
          "id" : "IdentificationSource",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "IdentificationSourceValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "Match State",
          "id" : "MatchState",
          "supportedOperators" : ["is", "is not"],
          "valueTypeId" : "MatchStateValueType",
          "valueHint" : null,
          "valueType" : {
            "dataType": "String"
          }
        }, {
          "name" : "Coordinates (GAV)",
          "id" : "Coordinates",
          "supportedOperators" : ["match", "do not match"],
          "valueTypeId" : "CoordinatesValueType",
          "valueHint" : null
        }];
  },
  getActionTypeData : function() {
    return [{
          "name" : "Fail",
          "id" : "fail",
          "availableTargets" : null,
          "requiresTarget" : false
        }, {
          "name" : "Warn",
          "id" : "warn",
          "availableTargets" : null,
          "requiresTarget" : false
        }, {
          "name" : "Notify",
          "id" : "notify",
          "availableTargets" : null,
          "requiresTarget" : true
        }];
  },
  getConditionValueTypeData : function() {
    return [{
          "id" : "AgeInDaysValueType",
          "dataType" : "Integer",
          "allowMultiple" : false,
          "availableValues" : null
        }, {
          "id" : "CoordinatesValueType",
          "dataType" : "String",
          "allowMultiple" : false,
          "availableValues" : null
        }, {
          "id" : "FloatValueType",
          "dataType" : "Float",
          "allowMultiple" : false,
          "availableValues" : null
        }, {
          "id" : "LabelValueType",
          "dataType" : "Label",
          "allowMultiple" : false,
          "availableValues" : []
        }, {
          "id" : "LicenseStatusValueType",
          "dataType" : "LicenseStatus",
          "allowMultiple" : false,
          "availableValues" : [{
                "id" : "OPEN",
                "name" : "Open"
              }, {
                "id" : "ACKNOWLEDGED",
                "name" : "Acknowledged"
              }, {
                "id" : "OVERRIDDEN",
                "name" : "Overridden"
              }, {
                "id" : "SELECTED",
                "name" : "Selected"
              }, {
                "id" : "CONFIRMED",
                "name" : "Confirmed"
              }]
        }, {
          "id" : "LicenseValueType",
          "dataType" : "License",
          "allowMultiple" : false,
          "availableValues" : [{
                "id" : "AAL",
                "shortDisplayName" : "AAL",
                "longDisplayName" : "Attribution Assurance License",
                "description" : null,
                "licenseUrl" : "http://www.spdx.org/licenses/AAL",
                "licenseCategoryId" : null
              }, {
                "id" : "AFL-UNSPECIFIED",
                "shortDisplayName" : "AFL",
                "longDisplayName" : "AFL-Style License Not Identifiable by Sonatype",
                "description" : null,
                "licenseUrl" : null,
                "licenseCategoryId" : null
              }]
        }, {
          "id" : "MatchStateValueType",
          "dataType" : "MatchState",
          "allowMultiple" : false,
          "availableValues" : [{
                "id" : "exact",
                "name" : "Exact"
              }, {
                "id" : "similar",
                "name" : "Similar"
              }, {
                "id" : "unknown",
                "name" : "Unknown"
              }]
        }, {
          "id" : "PercentageValueType",
          "dataType" : "Integer",
          "allowMultiple" : false,
          "availableValues" : null
        }, {
          "id" : "SecurityVulnerabilityStatusValueType",
          "dataType" : "SecurityerabilityStatus",
          "allowMultiple" : false,
          "availableValues" : [{
                "id" : "OPEN",
                "name" : "Open"
              }, {
                "id" : "ACKNOWLEDGED",
                "name" : "Acknowledged"
              }, {
                "id" : "NOT_APPLICABLE",
                "name" : "Not Applicable"
              }, {
                "id" : "CONFIRMED",
                "name" : "Confirmed"
              }]
        }];
  },
  getPolicyData : function() {
    return [{
          "id" : "053e89a476b34d7dac5d97665d2d241e",
          "name" : "asdffffrfff",
          "enabled" : true,
          "threatLevel" : 10,
          "constraints" : [{
                "id" : "076688f8f45a43b3a6061ef7aad6de4e",
                "name" : "asf",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "License",
                      "operator" : "is",
                      "value" : "AAL"
                    }, {
                      "conditionTypeId" : "AgeInDays",
                      "operator" : "older than",
                      "value" : "360"
                    }, {
                      "conditionTypeId" : "SecurityVulnerability",
                      "operator" : "present",
                      "value" : null
                    }, {
                      "conditionTypeId" : "SecurityVulnerabilitySeverity",
                      "operator" : "=",
                      "value" : "44"
                    }, {
                        "conditionTypeId" : "DependencyDepth",
                        "operator" : "is direct dependency",
                        "value" : null
                    }]
              }, {
                "id" : "6c2755ee5ef6400e935e913fdeda4e6b",
                "name" : "jjj",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "License",
                      "operator" : "is",
                      "value" : "AAL"
                    }]
              }, {
                "id" : "ed721f80645042e0b4505c072f7b657d",
                "name" : "ffff",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "License",
                      "operator" : "is",
                      "value" : "AAL"
                    }]
              }, {
                "id" : "7f7c035288004b60a580df3f3e14326a",
                "name" : "test",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "LicenseStatus",
                      "operator" : "is",
                      "value" : "OPEN"
                    }]
              }],
          "actions" : {
            "procure" : [],
            "develop" : [],
            "build" : [{
                  "actionTypeId" : "fail",
                  "target" : null
                }],
            "release" : [],
            "operate" : []
          }
        }, {
          "id" : "ec21b3ee9f31447c9e40913d91776593",
          "name" : "ppp",
          "enabled" : true,
          "threatLevel" : 5,
          "constraints" : [{
                "id" : "f7a416ad0b1f4b10b5d4b99268964bad",
                "name" : "jjj",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "AgeInDays",
                      "operator" : "older than",
                      "value" : "20075"
                    }]
              }],
          "actions" : {
            "procure" : [],
            "develop" : [],
            "build" : [{
                  "actionTypeId" : "fail",
                  "target" : null
                }],
            "release" : [],
            "operate" : []
          }
        }, {
          "id" : "bf80c1107cce40c7adecbe92e419732d",
          "name" : "asdf",
          "enabled" : true,
          "threatLevel" : 5,
          "constraints" : [{
                "id" : "74ab600e197a48b0b381e0a6eabaebd4",
                "name" : "asdf",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "SecurityVulnerability",
                      "operator" : "present",
                      "value" : null
                    }]
              }],
          "actions" : {}
        }, {
          "id" : "9a1475cd4c264a81a1294a4b7c00f12e",
          "name" : "asdfff",
          "enabled" : true,
          "threatLevel" : 5,
          "constraints" : [{
                "id" : "d376656bb49c49c1b167fda64ad58678",
                "name" : "asdf",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "SecurityVulnerability",
                      "operator" : "present",
                      "value" : null
                    }]
              }],
          "actions" : {}
        }, {
          "id" : "03bf6717cbbf49b8a177c3004668875a",
          "name" : "4444",
          "enabled" : true,
          "threatLevel" : 5,
          "constraints" : [{
                "id" : "d68c0fda6269459ab81524079a4bc6a8",
                "name" : "sd",
                "enabled" : true,
                "operator" : "OR",
                "conditions" : [{
                      "conditionTypeId" : "SecurityVulnerability",
                      "operator" : "present",
                      "value" : null
                    }]
              }],
          "actions" : {}
        }];
  },
  getCreateTestPolicy : function() {
    return {
      "constraints" : [{
            "name" : "createPolicyTest_constraint",
            "conditions" : [{
                  "conditionTypeId" : "SecurityVulnerability",
                  "operator" : "present"
                }],
            "operator" : "AND",
            "enabled" : true,
            "id" : "createPolicyTest_constraint"
          }],
      "actions" : {},
      "threatLevel" : 5,
      "name" : "createPolicyTest"
    };
  },
  getEditTestPolicy : function() {
    return {
      "id" : "03bf6717cbbf49b8a177c3004668875a",
      "name" : "5555",
      "enabled" : true,
      "threatLevel" : 5,
      "constraints" : [{
            "id" : "d68c0fda6269459ab81524079a4bc6a8",
            "name" : "sd",
            "enabled" : true,
            "operator" : "OR",
            "conditions" : [{
                  "conditionTypeId" : "SecurityVulnerability",
                  "operator" : "present",
                  "value" : null
                }]
          }],
      "actions" : {}
    };
  },
  getNewPolicy : function() {
    return {
      "name" : "policy3",
      "enabled" : true,
      "threatLevel" : 5,
      "constraints" : [{
            "id" : "constraint4",
            "name" : "constraint4",
            "enabled" : true,
            "operator" : "AND",
            "conditions" : [{
                  "conditionTypeId" : "LicenseStatus",
                  "operator" : "is not",
                  "value" : "CONFIRMED"
                }]
          }],
      "actions" : {
        "procure" : [{
              "actionTypeId" : "fail",
              "target" : null
            }, {
              "actionTypeId" : "notify",
              "target" : "mail11"
            }],
        "develop" : [{
              "actionTypeId" : "warn",
              "target" : null
            }, {
              "actionTypeId" : "notify",
              "target" : "mail12"
            }],
        "build" : [{
              "actionTypeId" : "fail",
              "target" : null
            }, {
              "actionTypeId" : "notify",
              "target" : "mail13"
            }],
        "release" : [{
              "actionTypeId" : "warn",
              "target" : null
            }, {
              "actionTypeId" : "notify",
              "target" : "mail14"
            }],
        "operate" : [{
              "actionTypeId" : "fail",
              "target" : null
            }, {
              "actionTypeId" : "notify",
              "target" : "mail15"
            }]
      }
    };
  },
  getPolicyEvaluationData : function() {
  	return {
	    	"alerts": [ ],
	    	"affectedComponentCount": 5,
	    	"criticalComponentCount": 5,
	    	"severeComponentCount": 0,
	    	"moderateComponentCount": 0
  	}
  }
};