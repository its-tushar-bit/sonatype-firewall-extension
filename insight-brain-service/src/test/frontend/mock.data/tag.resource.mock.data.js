var TagResourceMockData = {
  getApplicationTagUrl: function(orgId) {
    return [
      {
        color: "black",
        description: "Description 1",
        id: "c824e5d5c20d48e4a202dec55e2905cd",
        name: "Category 1",
        nameLowercaseNoWhitespace: "category1",
        organizationId: orgId || "f3cea033acf84984ae08d9250db4aa7b"
      }, {
        color: "blue",
        description: "Description 2",
        id: "cfe4d9c29b9a443d98c7e37669553eab",
        name: "Category 2",
        nameLowercaseNoWhitespace: "category2",
        organizationId: orgId || "f3cea033acf84984ae08d9250db4aa7b"
      }
    ];
  },
  getTagsUrl: function() {
    return {
      "tagsByOwner": [
        {
          "ownerId": "orgownerid",
          "ownerName": "orgname",
          "ownerType": "organization",
          "tags": [
            {
              color: "black",
              description: "Description 1",
              id: "appCategoryId_1",
              name: "Category 1",
              nameLowercaseNoWhitespace: "category1",
              organizationId: "orgownerid"
            },
            {
              color: "black",
              description: "Description 2",
              id: "appCategoryId_2",
              name: "Category 2",
              nameLowercaseNoWhitespace: "category2",
              organizationId: "orgownerid"
            }
          ]
        },
        {
          "ownerId": "rootorgownerid",
          "ownerName": "rootorgname",
          "ownerType": "organization",
          "tags": [
            {
              color: "red",
              description: "Description 3",
              id: "appCategoryId_3",
              name: "Category 3",
              nameLowercaseNoWhitespace: "category3",
              organizationId: "rootorgownerid"
            }
          ]
        }
      ]
    };
  },
  getApplicableOrganizationTags: function(orgId) {
    return [
      {
        color: "black",
        description: "Description 1",
        id: "c824e5d5c20d48e4a202dec55e2905cd",
        name: "Category 1",
        nameLowercaseNoWhitespace: "category1",
        organizationId: orgId || "f3cea033acf84984ae08d9250db4aa7b"
      }, {
        color: "blue",
        description: "Description 2",
        id: "cfe4d9c29b9a443d98c7e37669553eab",
        name: "Category 2",
        nameLowercaseNoWhitespace: "category2",
        organizationId: orgId || "f3cea033acf84984ae08d9250db4aa7b"
      }, {
        color: "red",
        description: "Description 2",
        id: "cfe4d9c29b9a443d98c7e37669553eef",
        name: "Category 2",
        nameLowercaseNoWhitespace: "category3",
        organizationId: orgId || "f3cea033acf84984ae08d9250db4aa7b"
      }
    ];
  }
};
