var PolicyResourceMockData = {
  getApplicablePolicies: function() {
    return {
      "policiesByOwner": [
        {
          "ownerId": "f3cea033acf84984ae08d9250db4aa7b",
          "ownerName": "Org1 Heh",
          "ownerType": "organization",
          "policies": [
            {
              "id": "4d6b4ac75ea148b2aa6ca36e6899cc78",
              "name": "Org Policy 3",
              "ownerId": "f3cea033acf84984ae08d9250db4aa7b",
              "enabled": true,
              "threatLevel": 0,
              "constraints": [
                {
                  "id": "d4fe6780471e4543bcb0e28d0e122b69",
                  "name": "Unpopular",
                  "enabled": true,
                  "operator": "OR",
                  "conditions": [{"conditionTypeId": "RelativePopularity", "operator": "<", "value": "10"}]
                }
              ],
              "actions": {
                "develop": [{"actionTypeId": "warn", "target": null}],
                "build": [{"actionTypeId": "fail", "target": null}],
                "stage-release": [{"actionTypeId": "fail", "target": null}],
                "release": [{"actionTypeId": "warn", "target": null}],
                "operate": [{"actionTypeId": "warn", "target": null}],
                "proxy": [{"actionTypeId": "warn", "target": null}]
              },
              "monitorNotifyActions": null
            }
          ],
          "policyTags": []
        }, {
          "ownerId": "ROOT_ORGANIZATION_ID",
          "ownerName": "Root Organization",
          "ownerType": "organization",
          "policies": [],
          "policyTags": []
        }
      ]
    };
  }
};
