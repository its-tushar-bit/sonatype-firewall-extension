/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectApplicationsSlice,
  selectLoadApplicationsError,
  selectLoadEmptyError,
  selectLoadingApplications,
} from 'MainRoot/OrgsAndPolicies/applicationsSelectors';

describe('applicationSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        applications: {
          ownerName: 'alpine test',
          loadingApplications: false,
          loadApplicationsError: 'some applications error',
          applications: [
            {
              id: '430b39e52a2e4ca48d708913f0f4b10d',
              publicId: 'alpine-test',
              name: 'alpine test',
              organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
              organizationName: 'wencel org',
              contact: null,
            },
            {
              id: 'edc3a3666e8c4d02a9065e9b8fa5a0d6',
              publicId: 'consumer',
              name: 'Consumer',
              organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
              organizationName: 'wencel org',
              contact: null,
            },
          ],
        },
      },
      router: {
        currentState: {
          name: 'management.view.application',
        },
        currentParams: { organizationId: 'orgId', applicationPublicId: 'alpine-test' },
      },
    };
  });

  describe('selectApplicationSlice', () => {
    it('returns slice', () => {
      expect(selectApplicationsSlice(mockState)).toEqual(mockState.orgsAndPolicies.applications);
    });
  });

  describe('Applications', () => {
    describe('selectLoadingApplications', () => {
      it('returns false when loading applications request is not loading', () => {
        expect(selectLoadingApplications(mockState)).toBeFalse();
      });

      it('returns true when loading applications request is loading', () => {
        mockState.orgsAndPolicies.applications.loadingApplications = true;
        expect(selectLoadingApplications(mockState)).toBeTrue();
      });
    });

    describe('selectLoadApplicationsError', () => {
      it('returns error when present', () => {
        expect(selectLoadApplicationsError(mockState)).toBe('some applications error');
      });
    });
  });

  describe('selectLoadEmptyError', () => {
    it('returns no error', () => {
      expect(selectLoadEmptyError(mockState)).toBeNull();
    });

    it('returns an error', () => {
      mockState.orgsAndPolicies.applications.ownerName = null;
      expect(selectLoadEmptyError(mockState)).toBe('some applications error');
    });

    it('returns default error', () => {
      mockState.orgsAndPolicies.applications.ownerName = null;
      mockState.orgsAndPolicies.applications.loadApplicationsError = null;
      expect(selectLoadEmptyError(mockState)).toBe('Could not find an application with ID alpine-test.');
    });
  });
});
