OrganizationMockData = {
  getGETResponse: function() {
    return [
      {
        "id": "1",
        "name": "org1"
      },
      {
        "id": "2",
        "name": "org2"
      },
      {
        "id": "3",
        "name": "org3"
      }
    ];
  },
  getPOSTResponse: function(name) {
    return {
      "id": "newid",
      "name": name
    };
  },
  getApplicablePolicies: function() {
    return {
      policiesByOwner: [
        {
          ownerId: "bom1-12345678",
          name: "orgName",
          type: "organization",
          policies: [
            {
              "id": "053e89a476b34d7dac5d97665d2d241e",
              "name": "asdffffrfff",
              "enabled": true,
              "threatLevel": 10,
              "constraints": [
                {
                  "id": "076688f8f45a43b3a6061ef7aad6de4e",
                  "name": "asf",
                  "enabled": true,
                  "operator": "OR",
                  "conditions": [
                    {
                      "conditionTypeId": "License",
                      "operator": "is",
                      "value": "AAL"
                    },
                    {
                      "conditionTypeId": "AgeInDays",
                      "operator": "older than",
                      "value": "360"
                    },
                    {
                      "conditionTypeId": "SecurityVulnerability",
                      "operator": "present",
                      "value": null
                    },
                    {
                      "conditionTypeId": "SecurityVulnerabilitySeverity",
                      "operator": "=",
                      "value": "44"
                    },
                    {
                      "conditionTypeId": "DependencyDepth",
                      "operator": "is direct dependency",
                      "value": null
                    }
                  ]
                },
                {
                  "id": "6c2755ee5ef6400e935e913fdeda4e6b",
                  "name": "jjj",
                  "enabled": true,
                  "operator": "OR",
                  "conditions": [
                    {
                      "conditionTypeId": "License",
                      "operator": "is",
                      "value": "AAL"
                    }
                  ]
                },
                {
                  "id": "ed721f80645042e0b4505c072f7b657d",
                  "name": "ffff",
                  "enabled": true,
                  "operator": "OR",
                  "conditions": [
                    {
                      "conditionTypeId": "License",
                      "operator": "is",
                      "value": "AAL"
                    }
                  ]
                },
                {
                  "id": "7f7c035288004b60a580df3f3e14326a",
                  "name": "test",
                  "enabled": true,
                  "operator": "OR",
                  "conditions": [
                    {
                      "conditionTypeId": "LicenseStatus",
                      "operator": "is",
                      "value": "OPEN"
                    }
                  ]
                }
              ],
              "actions": {
                "procure": [],
                "develop": [],
                "build": [
                  {
                    "actionTypeId": "fail",
                    "target": null
                  }
                ],
                "release": [],
                "operate": []
              }
            }
          ],
          policyTags: [
            {
              id: "f5014de14af9495e8e6a16be8b3ed9bc",
              policyId: "053e89a476b34d7dac5d97665d2d241e",
              tagId: '1'
            }
          ]
        }
      ]
    };
  }
};