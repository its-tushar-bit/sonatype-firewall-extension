MockData = {
  getActionStageData: function() {
    return [{
      "name": "Procure",
      "id": "procure"
    }, {
      "name": "Develop",
      "id": "develop"
    }, {
      "name": "Build",
      "id": "build"
    }, {
      "name": "Stage Release",
      "id": "stage-release"
    }, {
      "name": "Release",
      "id": "release"
    }, {
      "name": "Operate",
      "id": "operate"
    }];
  },
  getActionTypeData: function() {
    return [{
      "name": "Fail",
      "id": "fail",
      "summary": "Build Failed",
      "requiresTarget": false
    }, {
      "name": "Warn",
      "id": "warn",
      "summary": "Build Warning",
      "requiresTarget": false
    }, {
      "name": "Notify",
      "id": "notify",
      "summary": "Notification Sent",
      "requiresTarget": true
    }];
  },
  getRoleOneData: function() {
    return {
      "roleId": "1da70fae1fd54d6cb7999871ebdb9a36",
      "roleName": "Developer",
      "roleDescription": "Allows to evaluate policies.",
      "membersByOwner": [{
        "ownerId": "bom1-12345678",
        "ownerName": "app",
        "ownerType": "application",
        "members": [{
          "type": "USER",
          "internalName": "admin",
          "displayName": "Admin BuiltIn"
        }, {
          "type": "USER",
          "internalName": "plynch",
          "displayName": "Peter Lynch"
        }]
      }, {
        "ownerId": "58634626a6b747e3b3e585512b682832",
        "ownerName": "test",
        "ownerType": "organization",
        "members": [{
          "type": "USER",
          "internalName": "bfox",
          "displayName": "Brian Fox"
        }, {
          "type": "USER",
          "internalName": "dbradicich",
          "displayName": "Damian Bradicich"
        }, {
          "type": "USER",
          "internalName": "jduggan",
          "displayName": "Jordan Duggan"
        }, {
          "type": "USER",
          "internalName": "jwayman",
          "displayName": "Jeffrey Wayman"
        }, {
          "type": "USER",
          "internalName": "krobinson",
          "displayName": "Kelly Robinson"
        }, {
          "type": "USER",
          "internalName": "mhansen",
          "displayName": "Mike Hansen"
        }, {
          "type": "USER",
          "internalName": "mpiggott",
          "displayName": "Matthew Piggott"
        }, {
          "type": "USER",
          "internalName": "sgleason",
          "displayName": "Sunny Gleason"
        }]
      }]
    };
  },
  getRoleTwoData: function() {
    return {
      "roleId": "1cddabf7fdaa47d6833454af10e0a3ef",
      "roleName": "Owner",
      "roleDescription": "Allows to manage policies.",
      "membersByOwner": [{
        "ownerId": "bom1-12345678",
        "ownerName": "app",
        "ownerType": "application",
        "members": [{
          "type": "USER",
          "internalName": "bfox",
          "displayName": "Brian Fox"
        }, {
          "type": "USER",
          "internalName": "dbradicich",
          "displayName": "Damian Bradicich"
        }, {
          "type": "USER",
          "internalName": "jduggan",
          "displayName": "Jordan Duggan"
        }, {
          "type": "USER",
          "internalName": "jorlina",
          "displayName": "Joel Orlina"
        }]
      }, {
        "ownerId": "58634626a6b747e3b3e585512b682832",
        "ownerName": "test",
        "ownerType": "organization",
        "members": [{
          "type": "USER",
          "internalName": "admin",
          "displayName": "Admin BuiltIn"
        }, {
          "type": "USER",
          "internalName": "jswank",
          "displayName": "Jason Swank"
        }, {
          "type": "USER",
          "internalName": "jwayman",
          "displayName": "Jeffrey Wayman"
        }, {
          "type": "USER",
          "internalName": "mpiggott",
          "displayName": "Matthew Piggott"
        }]
      }]
    };
  },
  getRoleSaveCompleteEventMemberList: function() {
    return [{
      "type": "USER",
      "internalName": "test1",
      "displayName": "Test1"
    }, {
      "type": "USER",
      "internalName": "test2",
      "displayName": "Test2"
    }, {
      "type": "USER",
      "internalName": "test3",
      "displayName": "Test3"
    }, {
      "type": "USER",
      "internalName": "test4",
      "displayName": "Test4"
    }];
  }
};