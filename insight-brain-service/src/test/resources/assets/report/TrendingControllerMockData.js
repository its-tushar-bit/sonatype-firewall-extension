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
TrendingViolationsMockData = {
  getPolicyViolationMockData: function() {
    return {
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
      },
      "applications": {
        "total": 7,
        "risks": [
          {
            "none": 4,
            "moderate": 4,
            "severe": 3,
            "critical": 0,
            "name": "ci-plugin"
          },
          {
            "none": 8,
            "moderate": 5,
            "severe": 3,
            "critical": 1,
            "name": "clm-maven-plugin"
          },
          {
            "none": 36,
            "moderate": 41,
            "severe": 36,
            "critical": 5,
            "name": "CLM-server"
          },
          {
            "none": 47,
            "moderate": 64,
            "severe": 49,
            "critical": 18,
            "name": "insight-portal"
          },
          {
            "none": 39,
            "moderate": 37,
            "severe": 45,
            "critical": 2,
            "name": "Nexus-OSS"
          },
          {
            "none": 116,
            "moderate": 47,
            "severe": 64,
            "critical": 3,
            "name": "Nexus-Pro"
          },
          {
            "none": 6,
            "moderate": 6,
            "severe": 5,
            "critical": 0,
            "name": "sonatype-clm-scanner"
          }
        ]
      }
    }
  }
}