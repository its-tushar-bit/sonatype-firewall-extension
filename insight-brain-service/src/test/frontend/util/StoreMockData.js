StoreMockData = {
  getOrganizations: function() {
    return [
      {
        "id": "org_1",
        "name": "org_ONE",
        "nameLowercaseNoWhitespace": "org_one"
      },
      {
        "id": "org_2",
        "name": "org_TWO",
        "nameLowercaseNoWhitespace": "org_two"
      }
    ];
  },
  newOrganization: function() {
    return {
      "id": "org_3",
      "name": "org_THREE",
      "nameLowercaseNoWhitespace": "org_three"
    };
  },
  getApplications: function() {
    return [
      {
        "id": "app_10",
        "publicId":"app_public_ten",
        "name":"app_TEN",
        "organizationId":"org_1",
        "organizationName":"org_TEN",
        "contact": {
          "internalName":"admin",
          "displayName":"Admin BuiltIn",
          "email":"admin@localhost",
          "realm":"CLM",
          "error":null
        }
      },
      {
        "id": "app_20",
        "publicId":"app_public_twenty",
        "name":"app_TWENTY",
        "organizationId":"org_1",
        "organizationName":"org_ONE",
        "contact": null
      }
    ];
  },
  newApplication: function() {
    return {
      "id": "app_30",
      "publicId":"app_public_thirty",
      "name":"app_THIRTY",
      "organizationId":"org_2",
      "organizationName":"org_TWO",
      "contact": null
    };
  }
};
