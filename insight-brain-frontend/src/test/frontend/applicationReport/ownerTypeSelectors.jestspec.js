/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectOwnerType,
  selectIsHrcReport,
  selectIsApplicationReport,
  selectOwnerPublicId,
} from 'MainRoot/applicationReport/applicationReportSelectors';

describe('Owner Type Selectors', () => {
  describe('selectOwnerType', () => {
    it('should return APPLICATION for application reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'APPLICATION',
        },
      };

      expect(selectOwnerType(state)).toBe('APPLICATION');
    });

    it('should return HOSTED_REPOSITORY_COMPONENT for HRC reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'HOSTED_REPOSITORY_COMPONENT',
        },
      };

      expect(selectOwnerType(state)).toBe('HOSTED_REPOSITORY_COMPONENT');
    });

    it('should return HOSTED_REPOSITORY_COMPONENT when hrcId is only in router params (URL fallback)', () => {
      // On first render (before setReportParameters dispatches) Redux carries the
      // initState default of APPLICATION — the selector must still detect HRC from the URL.
      const state = {
        applicationReport: { ownerType: 'APPLICATION' },
        router: { currentParams: { hrcId: 'hrc-uuid-abc' } },
      };

      expect(selectOwnerType(state)).toBe('HOSTED_REPOSITORY_COMPONENT');
    });

    it('should normalize the backend wire form (lowercase) to uppercase', () => {
      const state = {
        applicationReport: { ownerType: 'hosted_repository_component' },
      };

      expect(selectOwnerType(state)).toBe('HOSTED_REPOSITORY_COMPONENT');
    });
  });

  describe('selectIsHrcReport', () => {
    it('should return true for HRC reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'HOSTED_REPOSITORY_COMPONENT',
        },
      };

      expect(selectIsHrcReport(state)).toBe(true);
    });

    it('should return false for application reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'APPLICATION',
        },
      };

      expect(selectIsHrcReport(state)).toBe(false);
    });
  });

  describe('selectIsApplicationReport', () => {
    it('should return true for application reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'APPLICATION',
        },
      };

      expect(selectIsApplicationReport(state)).toBe(true);
    });

    it('should return false for HRC reports', () => {
      const state = {
        applicationReport: {
          ownerType: 'HOSTED_REPOSITORY_COMPONENT',
        },
      };

      expect(selectIsApplicationReport(state)).toBe(false);
    });
  });

  describe('selectOwnerPublicId', () => {
    it('should return applicationPublicId for application reports', () => {
      const state = {
        applicationReport: {
          reportParameters: {
            applicationPublicId: 'my-app',
            scanId: 'scan-123',
          },
        },
      };

      expect(selectOwnerPublicId(state)).toBe('my-app');
    });

    it('should return hrcId for HRC reports', () => {
      const state = {
        applicationReport: {
          reportParameters: {
            hrcId: 'hrc-uuid-123',
            scanId: 'scan-456',
          },
        },
      };

      expect(selectOwnerPublicId(state)).toBe('hrc-uuid-123');
    });
  });
});
