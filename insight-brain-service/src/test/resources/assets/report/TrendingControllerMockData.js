TrendingReportMockData = {
  get: function() {
    return {
      "meta": {
        "generatedBy": "Author",
        "generatedFor": "Company Name",
        "generatedOn": 1362902400000,
        "periodStart": 1362384000000,
        "periodEnd": 1362902400000
      },
      "components": {
        "inRepository": 50,
        "inApplications": 40,
        "exact": 20,
        "partial": 15,
        "unknown": 5
      },
      "applications": {
        "total": 15,
        "risks": [
          {
            "name": "Application One",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "name": "Application Two",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "name": "Application Three",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "name": "Application Four",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "name": "Application Five",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ]
      },
      "policies": [
        {
          "name": "Architecture-Banned",
          "threat": 10,
          "category": "quality",
          "violations": [
            {
              "period": 0,
              "count": 4
            },
            {
              "period": 1,
              "count": 6
            },
            {
              "period": 2,
              "count": 2
            },
            {
              "period": 3,
              "count": 4
            }
          ]
        },
        {
          "name": "License-Banned",
          "threat": 10,
          "category": "license",
          "violations": [
            {
              "period": 0,
              "count": 12
            },
            {
              "period": 1,
              "count": 10
            },
            {
              "period": 2,
              "count": 6
            },
            {
              "period": 3,
              "count": 2
            }
          ]
        },
        {
          "name": "Security-Critical",
          "threat": 10,
          "category": "security",
          "violations": [
            {
              "period": 0,
              "count": 1
            },
            {
              "period": 1,
              "count": 3
            },
            {
              "period": 2,
              "count": 2
            },
            {
              "period": 3,
              "count": 1
            }
          ]
        },
        {
          "name": "License-Copyleft",
          "threat": 9,
          "category": "license",
          "violations": [
            {
              "period": 0,
              "count": 10
            },
            {
              "period": 1,
              "count": 11
            },
            {
              "period": 2,
              "count": 5
            },
            {
              "period": 3,
              "count": 5
            }
          ]
        },
        {
          "name": "Security-High",
          "threat": 9,
          "category": "security",
          "violations": [
            {
              "period": 0,
              "count": 2
            },
            {
              "period": 1,
              "count": 4
            },
            {
              "period": 2,
              "count": 1
            },
            {
              "period": 3,
              "count": 2
            }
          ]
        },
        {
          "name": "Security-Medium",
          "threat": 7,
          "category": "security",
          "violations": [
            {
              "period": 0,
              "count": 12
            },
            {
              "period": 1,
              "count": 12
            },
            {
              "period": 2,
              "count": 2
            },
            {
              "period": 3,
              "count": 3
            }
          ]
        },
        {
          "name": "License-Non-Standard",
          "threat": 6,
          "category": "license",
          "violations": [
            {
              "period": 0,
              "count": 1
            },
            {
              "period": 1,
              "count": 4
            },
            {
              "period": 2,
              "count": 4
            },
            {
              "period": 3,
              "count": 8
            }
          ]
        },
        {
          "name": "License-Unknown",
          "threat": 5,
          "category": "license",
          "violations": [
            {
              "period": 0,
              "count": 6
            },
            {
              "period": 1,
              "count": 7
            },
            {
              "period": 2,
              "count": 4
            },
            {
              "period": 3,
              "count": 8
            }
          ]
        },
        {
          "name": "Security-Low",
          "threat": 3,
          "category": "security",
          "violations": [
            {
              "period": 0,
              "count": 3
            },
            {
              "period": 1,
              "count": 5
            },
            {
              "period": 2,
              "count": 2
            },
            {
              "period": 3,
              "count": 1
            }
          ]
        },
        {
          "name": "Architecture-Deprecated",
          "threat": 1,
          "category": "quality",
          "violations": [
            {
              "period": 0,
              "count": 2
            },
            {
              "period": 1,
              "count": 5
            },
            {
              "period": 2,
              "count": 4
            },
            {
              "period": 3,
              "count": 3
            }
          ]
        },
        {
          "name": "Architecture-Quality",
          "threat": 1,
          "category": "quality",
          "violations": [
            {
              "period": 0,
              "count": 9
            },
            {
              "period": 1,
              "count": 9
            },
            {
              "period": 2,
              "count": 9
            },
            {
              "period": 3,
              "count": 9
            }
          ]
        },
        {
          "name": "Component-Indeterminate",
          "threat": 1,
          "category": "other",
          "violations": [
            {
              "period": 0,
              "count": 2
            },
            {
              "period": 1,
              "count": 5
            },
            {
              "period": 2,
              "count": 2
            },
            {
              "period": 3,
              "count": 10
            }
          ]
        },
        {
          "name": "Component-Unknown",
          "threat": 0,
          "category": "other",
          "violations": [
            {
              "period": 0,
              "count": 10
            },
            {
              "period": 1,
              "count": 8
            },
            {
              "period": 2,
              "count": 6
            },
            {
              "period": 3,
              "count": 7
            }
          ]
        }
      ],
      "partialMatches": [
        {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "version": "3.0.5",
          "count": 5
        },
        {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "version": "3.0.5",
          "count": 5
        },
        {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "version": "3.0.5",
          "count": 4
        },
        {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "version": "3.0.5",
          "count": 2
        },
        {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "version": "3.0.5",
          "count": 2
        }
      ],
      "topPolicyViolations": {
        "security": [
          {
            "groupId": "org.eclipse.birt.runtime.3_7_1",
            "artifactId": "org.eclipse.equinox.app",
            "version": "1.3.100",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ],
        "quality": [
          {
            "groupId": "org.powermock",
            "artifactId": "powermock-mockito-release-full",
            "version": "1.4.11",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ],
        "license": [
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ],
        "all": [
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ],
        "other": [
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          }
        ]
      }
    };
  }
};
ChartMockData = {
  getDiffData: function() {
    return {
      diffData: {"security": [
        {"threat": "critical", "violations": 10, "previousViolations": 20}
      ], "license": [
        {"threat": "critical", "violations": 22, "previousViolations": 7}
      ], "quality": [
        {"threat": "critical", "violations": 4, "previousViolations": 4}
      ], "other": [
        {"threat": "critical", "violations": 0, "previousViolations": 0}
      ]}
    };
  },
  getPercentageData: function() {
    return [
      {
        name: 'name',
        value: 10,
        color: 'black'
      }
    ]
  }
};