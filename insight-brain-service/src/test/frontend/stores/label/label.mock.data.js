var LabelMockData = {
  getApplicableLabels: function() {
    return {
      "labelsByOwner": [
        {
          "ownerId": "appownerid",
          "ownerName": "appname",
          "ownerType": "application",
          "labels": [
            {
              "id": "applabelid",
              "ownerId": "appownerid",
              "label": "AppLabel",
              "labelLowercase": "applabel",
              "color": "red"
            },
            {
              "id": "applabelid_01",
              "ownerId": "appownerid",
              "label": "AnotherAppLabel",
              "labelLowercase": "anotherapplabel",
              "color": "red"
            }
          ]
        },
        {
          "ownerId": "orgownerid",
          "ownerName": "orgname",
          "ownerType": "organization",
          "labels": [
            {
              "id": "orglabelid",
              "ownerId": "orgownerid",
              "label": "OrgLabel",
              "labelLowercase": "orglabel",
              "color": "red"
            }
          ]
        }
      ]
    };
  }
};
