/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectData,
  selectLoading,
  selectLoadError,
  selectSourceControlRateLimitsSlice,
  selectSortColumn,
  selectSortDirection,
  selectUserRateLimitsExpanded,
  selectUserDefiningOwnersExpanded,
  selectUserAssociatedApplicationsExpanded,
  selectLastUpdated,
} from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSelectors';

describe('sourceControlRateLimitsSelectors', () => {
  describe('selectSourceControlRateLimitsSlice', () => {
    it('selects sourceControlRateLimitsSlice', () => {
      const appState = {
        sourceControlRateLimits: null,
      };

      const selected = selectSourceControlRateLimitsSlice(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectData', () => {
    it('selects data', () => {
      const appState = {
        sourceControlRateLimits: {
          data: null,
        },
      };

      const selected = selectData(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectLoading', () => {
    it('selects loading', () => {
      const appState = {
        sourceControlRateLimits: {
          loading: null,
        },
      };

      const selected = selectLoading(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectLoadError', () => {
    it('selects loadError', () => {
      const appState = {
        sourceControlRateLimits: {
          loadError: null,
        },
      };

      const selected = selectLoadError(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectSortColumn', () => {
    it('selects sortColumn', () => {
      const appState = {
        sourceControlRateLimits: {
          sortColumn: null,
        },
      };

      const selected = selectSortColumn(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectSortDirection', () => {
    it('selects sortDirection', () => {
      const appState = {
        sourceControlRateLimits: {
          sortDirection: null,
        },
      };

      const selected = selectSortDirection(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectUserRateLimitsExpanded', () => {
    it('selects userRateLimitsExpanded', () => {
      const appState = {
        sourceControlRateLimits: {
          userRateLimitsExpanded: null,
        },
      };

      const selected = selectUserRateLimitsExpanded(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectUserDefiningOwnersExpanded', () => {
    it('selects userDefiningOwnersExpanded', () => {
      const appState = {
        sourceControlRateLimits: {
          userDefiningOwnersExpanded: null,
        },
      };

      const selected = selectUserDefiningOwnersExpanded(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectUserAssociatedApplicationsExpanded', () => {
    it('selects userAssociatedApplicationsExpanded', () => {
      const appState = {
        sourceControlRateLimits: {
          userAssociatedApplicationsExpanded: null,
        },
      };

      const selected = selectUserAssociatedApplicationsExpanded(appState);

      expect(selected).toBeNull();
    });
  });

  describe('selectLastUpdated', () => {
    it('selects lastUpdated', () => {
      const appState = {
        sourceControlRateLimits: {
          lastUpdated: null,
        },
      };

      const selected = selectLastUpdated(appState);

      expect(selected).toBeNull();
    });
  });
});
