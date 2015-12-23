var AccessMockData = {
  getRoleMappings: function() {
    return {
      "membersByRole": [
        {
          "roleId": "2cb71b3468d649789163ea2e212b5411",
          "roleName": "Test Role",
          "roleDescription": "Evaluates applications and views policy violation summary results.",
          "membersByOwner": [
            {
              "ownerId": "asdf",
              "ownerName": "Test App",
              "ownerType": "application",
              "members": [
                {
                  "type": "USER",
                  "internalName": "userTest1",
                  "displayName": "User Test1",
                  "email": "userTest1@sonatype.com",
                  "realm": "CLM"
                },                 {
                  "type": "USER",
                  "internalName": "userTest2",
                  "displayName": "User Test2",
                  "email": "userTest2@sonatype.com",
                  "realm": "CLM"
                }
              ]
            }, {
              "ownerId": "f3c2f4468f1e408b8cb2724ce8c676c2",
              "ownerName": "Org",
              "ownerType": "organization",
              "members": [
                {
                  "type": "USER",
                  "internalName": "userTest1",
                  "displayName": "User Test1",
                  "email": "userTest1@sonatype.com",
                  "realm": "CLM"
                }
              ]
            }, {
              "ownerId": "ROOT_ORGANIZATION_ID",
              "ownerName": "Root Organization",
              "ownerType": "organization",
              "members": []
            }
          ]
        }
      ],
        "ldapRealm": null,
        "groupSearchEnabled": true
    };
  },

  getMoreRoleMappings: function() {
    var base = AccessMockData.getRoleMappings();
    base.membersByRole[1] = {
      "roleId": "abcdef",
      "roleName": "Another Test Role",
      "roleDescription": "Yet another test role.",
      "membersByOwner": [
        {
          "ownerId": "asdf",
          "ownerName": "Test App",
          "ownerType": "application",
          "members": []
        }, {
          "ownerId": "f3c2f4468f1e408b8cb2724ce8c676c2",
          "ownerName": "Org",
          "ownerType": "organization",
          "members": []
        }, {
          "ownerId": "ROOT_ORGANIZATION_ID",
          "ownerName": "Root Organization",
          "ownerType": "organization",
          "members": []
        }
      ]
    };
    return base;
  }
};
