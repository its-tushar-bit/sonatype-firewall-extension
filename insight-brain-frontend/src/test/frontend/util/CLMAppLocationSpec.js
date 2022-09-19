/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocation from '../../../main/frontend/utilAngular/CLMContextLocation';

describe('CLMContextLocation', () => {
  let appId, orgId, state;

  const setApplicationState = (newAppId) => {
    angular.extend(state, {
      current: {
        name: 'application',
      },
      params: {
        applicationId: newAppId,
        applicationPublicId: newAppId,
      },
    });
    appId = newAppId;
    orgId = null;
  };

  const setOrganizationState = (newOrgId) => {
    angular.extend(state, {
      current: {
        name: 'organization',
      },
      params: {
        organizationId: newOrgId,
      },
    });
    appId = null;
    orgId = newOrgId;
  };

  const setRepositoryState = () => {
    angular.extend(state, {
      current: {
        name: 'repositories',
      },
      params: {},
    });
    appId = null;
    orgId = null;
  };

  beforeEach(() => {
    angular.mock.module(clmContextLocation.name, ($provide) => {
      state = {};
      $provide.value('ApplicationId', {
        encoded: () => {
          return appId;
        },
      });

      $provide.value('OrganizationId', {
        encoded: () => {
          return orgId;
        },
      });

      $provide.value('$state', state);
      $provide.value('baseUrl', '');
    });
  });

  it('New Triggers Global', inject((CLMContextLocations) => {
    setApplicationState('_new_');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/global/global/query');
  }));

  it('Application', inject((CLMContextLocations) => {
    setApplicationState('bom1-12345678');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/application/bom1-12345678/query');
  }));

  it('Organization', inject((CLMContextLocations) => {
    setOrganizationState('bom1-12345678');
    expect(CLMContextLocations.getFindUsersUrl()).toEqual('/rest/user/organization/bom1-12345678/query');
  }));

  it('getRetentionPoliciesUrl', inject(function (CLMContextLocations) {
    setOrganizationState('bom1-12345678');
    expect(CLMContextLocations.getRetentionPoliciesUrl('orgId')).toEqual(
      '/api/v2/dataRetentionPolicies/organizations/orgId'
    );
  }));

  describe('Application Context', () => {
    it('isApplication returns true', inject((CLMContextLocations) => {
      setApplicationState('appId');
      expect(CLMContextLocations.isApplication()).toBeTrue();
    }));

    it('isOrganization returns false', inject((CLMContextLocations) => {
      setApplicationState('appId');
      expect(CLMContextLocations.isOrganization()).toBeFalse();
    }));

    it('isRootOrg returns false', inject((CLMContextLocations) => {
      setApplicationState('appId');
      expect(CLMContextLocations.isRootOrg()).toBeFalse();
    }));

    it('isRepositories returns false', inject((CLMContextLocations) => {
      setApplicationState('appId');
      expect(CLMContextLocations.isRepositories()).toBeFalse();
    }));

    it('isGlobal returns false', inject((CLMContextLocations) => {
      setApplicationState('appId');
      expect(CLMContextLocations.isGlobal()).toBeFalse();
    }));
  });

  describe('Organization Context', () => {
    it('isApplication returns false', inject((CLMContextLocations) => {
      setOrganizationState('orgId');
      expect(CLMContextLocations.isApplication()).toBeFalse();
    }));

    it('isOrganization returns true', inject((CLMContextLocations) => {
      setOrganizationState('orgId');
      expect(CLMContextLocations.isOrganization()).toBeTrue();
    }));

    it('isRootOrg returns false', inject((CLMContextLocations) => {
      setOrganizationState('orgId');
      expect(CLMContextLocations.isRootOrg()).toBeFalse();
    }));

    it('isRepositories returns false', inject((CLMContextLocations) => {
      setOrganizationState('orgId');
      expect(CLMContextLocations.isRepositories()).toBeFalse();
    }));

    it('isGlobal returns false', inject((CLMContextLocations) => {
      setOrganizationState('orgId');
      expect(CLMContextLocations.isGlobal()).toBeFalse();
    }));
  });

  describe('ROOT Organization Context', () => {
    it('isApplication returns false', inject((CLMContextLocations) => {
      setOrganizationState('ROOT_ORGANIZATION_ID');
      expect(CLMContextLocations.isApplication()).toBeFalse();
    }));

    it('isOrganization returns true', inject((CLMContextLocations) => {
      setOrganizationState('ROOT_ORGANIZATION_ID');
      expect(CLMContextLocations.isOrganization()).toBeTrue();
    }));

    it('isRootOrg returns true', inject((CLMContextLocations) => {
      setOrganizationState('ROOT_ORGANIZATION_ID');
      expect(CLMContextLocations.isRootOrg()).toBeTrue();
    }));

    it('isRepositories returns false', inject((CLMContextLocations) => {
      setOrganizationState('ROOT_ORGANIZATION_ID');
      expect(CLMContextLocations.isRepositories()).toBeFalse();
    }));

    it('isGlobal returns false', inject((CLMContextLocations) => {
      setOrganizationState('ROOT_ORGANIZATION_ID');
      expect(CLMContextLocations.isGlobal()).toBeFalse();
    }));
  });

  describe('Repository Context', () => {
    it('isApplication returns false', inject((CLMContextLocations) => {
      setRepositoryState();
      expect(CLMContextLocations.isApplication()).toBeFalse();
    }));

    it('isOrganization returns false', inject((CLMContextLocations) => {
      setRepositoryState();
      expect(CLMContextLocations.isOrganization()).toBeFalse();
    }));

    it('isRootOrg returns false', inject((CLMContextLocations) => {
      setRepositoryState();
      expect(CLMContextLocations.isRootOrg()).toBeFalse();
    }));

    it('isRepositories returns true', inject((CLMContextLocations) => {
      setRepositoryState();
      expect(CLMContextLocations.isRepositories()).toBeTrue();
    }));

    it('isGlobal returns true', inject((CLMContextLocations) => {
      setRepositoryState();
      expect(CLMContextLocations.isGlobal()).toBeTrue();
    }));
  });
});
