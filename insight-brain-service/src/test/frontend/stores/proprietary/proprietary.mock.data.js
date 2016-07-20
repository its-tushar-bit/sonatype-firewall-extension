var ProprietaryMockData = {
  getProprietaryConfigurationStoreMockData: function() {
    return {
      "proprietaryConfigByOwners": [
        {
          "ownerId": "ownerID",
          "ownerName": "App Name",
          "ownerType": "application",
          "proprietaryConfig":
          [
            {
              "id": "configId",
              "ownerId": "ownerId",
              "packages": [
                "com.sonatype",
                "com.local"
              ],
              "regexes": [
                ".*\/test\\.zip"
              ]
            }
          ]
        },
        {
          "ownerId": "ROOT_ORGANIZATION_ID",
          "ownerName": "Root Organization",
          "ownerType": "organization",
          "proprietaryConfig":
          [
            {
              "id": null,
              "ownerId": "ROOT_ORGANIZATION_ID",
              "packages": [
              ],
              "regexes": [
                ".*\/foo\\.zip"
              ]
            }
          ]
        }
      ]
    };
  },
  getProprietaryConfiguration: function() {
    return {
      "proprietaryConfigByOwners": [
        {
          "ownerId": "ownerID",
          "ownerName": "App Name",
          "ownerType": "application",
          "proprietaryConfig": {
            "id": "configId",
            "ownerId": "ownerId",
            "packages": [
              "com.sonatype",
              "com.local"
            ],
            "regexes": [
              ".*\/test\\.zip"
            ]
          }
        },
        {
          "ownerId": "ROOT_ORGANIZATION_ID",
          "ownerName": "Root Organization",
          "ownerType": "organization",
          "proprietaryConfig": {
            "id": null,
            "ownerId": "ROOT_ORGANIZATION_ID",
            "packages": [],
            "regexes": [
              ".*\/foo\\.zip"
            ]
          }
        }
      ]
    };
  }
};
