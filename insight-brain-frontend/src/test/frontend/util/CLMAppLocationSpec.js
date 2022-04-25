/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocation, {
  getApplicableLicenseGroupsUrl,
  getDeleteLicenseGroupUrl,
  getLicenseGroupLicensesUrl,
  getLicenseGroupsUrl,
} from '../../../main/frontend/util/CLMContextLocation';

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
  };

  const setRepositoryState = () => {
    angular.extend(state, {
      current: {
        name: 'repositories',
      },
      params: {},
    });
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

  describe('If context information', () => {
    describe('is from an application', () => {
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

    describe('is from an organization', () => {
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

    describe('is from a ROOT organization', () => {
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

    describe('is from a repository', () => {
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

  describe('getLicenseGroupsUrl', () => {
    it('returns the license threat group url with application', () => {
      const $state = {
        params: {
          applicationPublicId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.application',
        },
      };
      expect(getLicenseGroupsUrl($state)).toBe('/rest/licenseThreatGroup/application/applicationId');
    });

    it('returns the license threat group url with organization', () => {
      const $state = {
        params: {
          applicationId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.organization',
        },
      };
      expect(getLicenseGroupsUrl($state)).toBe('/rest/licenseThreatGroup/organization/organizationId');
    });
  });

  describe('getApplicableLicenseGroupsUrl', () => {
    it('returns the applicable license threat group url with application', () => {
      const $state = {
        params: {
          applicationPublicId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.application',
        },
      };
      expect(getApplicableLicenseGroupsUrl($state)).toBe(
        '/rest/licenseThreatGroup/application/applicationId/applicable'
      );
    });

    it('returns the applicable license threat group url with organization', () => {
      const $state = {
        params: {
          applicationId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.organization',
        },
      };
      expect(getApplicableLicenseGroupsUrl($state)).toBe(
        '/rest/licenseThreatGroup/organization/organizationId/applicable'
      );
    });
  });

  describe('getDeleteLicenseGroupUrl', () => {
    it('returns the delete license threat group url with application', () => {
      const $state = {
        params: {
          applicationPublicId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.application',
        },
      };
      expect(getDeleteLicenseGroupUrl($state, 'ltgId')).toBe(
        '/rest/licenseThreatGroup/application/applicationId/ltgId'
      );
    });

    it('returns the delete license threat group url with organization', () => {
      const $state = {
        params: {
          applicationId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.organization',
        },
      };
      expect(getDeleteLicenseGroupUrl($state, 'ltgId')).toBe(
        '/rest/licenseThreatGroup/organization/organizationId/ltgId'
      );
    });
  });

  describe('getLicenseGroupLicensesUrl', () => {
    it('returns the license threat group licenses url with application', () => {
      const $state = {
        params: {
          applicationPublicId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.application',
        },
      };
      expect(getLicenseGroupLicensesUrl($state, 'ltgId')).toBe(
        '/rest/licenseThreatGroupLicense/application/applicationId/ltgId'
      );
    });

    it('returns the license threat group licenses url with organization', () => {
      const $state = {
        params: {
          applicationId: 'applicationId',
          organizationId: 'organizationId',
        },
        current: {
          name: 'something.organization',
        },
      };
      expect(getLicenseGroupLicensesUrl($state, 'ltgId')).toBe(
        '/rest/licenseThreatGroupLicense/organization/organizationId/ltgId'
      );
    });
  });
});
