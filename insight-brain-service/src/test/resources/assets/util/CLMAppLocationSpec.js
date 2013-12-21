describe('CLMAppLocation', function () {
  'use strict';
  var appId, orgId, state;

  function setApplicationState(newAppId) {
    angular.extend(state, {
      current : {
        name : 'application'
      }
    });
    appId = newAppId;
    orgId = null;
  }

  function setOrganizationState(newOrgId) {
    angular.extend(state, {
      current : {
        name : 'organization'
      }
    });
    appId = null;
    orgId = newOrgId;
  }

  function setGlobalState() {
    angular.extend(state, {
      current : {
        name : ''
      }
    });
  }
  
  beforeEach(module('CLMAppLocation', function($provide) {
    state = {};

    $provide.value('ApplicationId', {
      encoded: function() {
        return appId;
      }
    });

    $provide.value('OrganizationId', {
      encoded: function() {
        return orgId;
      }
    });

    $provide.value('$state', state);
    $provide.value('baseUrl', '');
  }));

  it('New Triggers Global', inject(function (CLMAppLocations) {
    setApplicationState('_new_');
    expect(CLMAppLocations.getFindUsersUrl()).toEqual('/rest/user/global/global/query');
  }));

  it('Application', inject(function (CLMAppLocations) {
    setApplicationState("bom1-12345678");
    expect(CLMAppLocations.getFindUsersUrl()).toEqual('/rest/user/application/bom1-12345678/query');
  }));

  it('Organization', inject(function (CLMAppLocations) {
    setOrganizationState("bom1-12345678");
    expect(CLMAppLocations.getFindUsersUrl()).toEqual('/rest/user/organization/bom1-12345678/query');
  }));
});