/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocation from '../../../main/frontend/util/CLMContextLocation';

describe('CLMContextLocation', function () {
  var appId, orgId, state;

  function setApplicationState(newAppId) {
    angular.extend(state, {
      current: {
        name: 'application'
      }
    });
    appId = newAppId;
    orgId = null;
  }

  function setOrganizationState(newOrgId) {
    angular.extend(state, {
      current: {
        name: 'organization'
      }
    });
    appId = null;
    orgId = newOrgId;
  }

  beforeEach(angular.mock.module(clmContextLocation.name, function($provide) {
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

  it('New Triggers Global', inject(function (CLMContextLocations) {
    setApplicationState('_new_');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/global/global/query');
  }));

  it('Application', inject(function (CLMContextLocations) {
    setApplicationState('bom1-12345678');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/application/bom1-12345678/query');
  }));

  it('Organization', inject(function (CLMContextLocations) {
    setOrganizationState('bom1-12345678');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/organization/bom1-12345678/query');
  }));
});
