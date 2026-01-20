/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import moment from 'moment';
import {
  RETIREMENT_DATE,
  RETIRING_DASHBOARD_IDS,
  isRetiringDashboard,
  isDashboardRetired,
  filterRetiredDashboards,
  getDaysUntilRetirement,
  getRetirementText,
} from 'MainRoot/enterpriseReporting/utils';

describe('Enterprise Reporting Retirement Utils', () => {
  describe('isRetiringDashboard', () => {
    it('should return true for dashboards in the retiring list', () => {
      expect(isRetiringDashboard('rolling-recap')).toBe(true);
      expect(isRetiringDashboard('upgrade-posture')).toBe(true);
    });

    it('should return false for dashboards not in the retiring list', () => {
      expect(isRetiringDashboard('other-dashboard')).toBe(false);
      expect(isRetiringDashboard('some-other-dashboard')).toBe(false);
    });

    it('should return false for null or undefined', () => {
      expect(isRetiringDashboard(null)).toBe(false);
      expect(isRetiringDashboard(undefined)).toBe(false);
    });
  });

  describe('isDashboardRetired', () => {
    const createDashboard = (dashboardId, spotlightText) => ({
      dashboardId,
      spotlightText,
    });

    let mockUtc;
    const originalUtc = moment.utc;

    beforeEach(() => {
      mockUtc = jest.spyOn(moment, 'utc').mockImplementation((date) => {
        if (date === undefined) {
          return originalUtc('2026-03-01');
        }
        return originalUtc(date);
      });
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('should return false if dashboard is not in retiring list', () => {
      const dashboard = createDashboard('other-dashboard', 'retiring soon');
      expect(isDashboardRetired(dashboard)).toBe(false);
    });

    it('should return false if spotlightText is missing', () => {
      const dashboard = createDashboard('rolling-recap', null);
      expect(isDashboardRetired(dashboard)).toBe(false);
    });

    it('should return false if spotlightText does not contain "retiring"', () => {
      const dashboard = createDashboard('rolling-recap', 'NEW');
      expect(isDashboardRetired(dashboard)).toBe(false);
    });

    it('should return false if current date is before retirement date', () => {
      mockUtc.mockImplementation((date) => {
        if (date === undefined) {
          return originalUtc('2026-01-01');
        }
        return originalUtc(date);
      });
      const dashboard = createDashboard('rolling-recap', 'retiring soon');
      expect(isDashboardRetired(dashboard)).toBe(false);
    });

    it('should return true when all conditions are met and date is after retirement', () => {
      const dashboard = createDashboard('rolling-recap', 'retiring soon');
      expect(isDashboardRetired(dashboard)).toBe(true);
    });

    it('should be case-insensitive for "retiring" check', () => {
      expect(isDashboardRetired(createDashboard('rolling-recap', 'RETIRING SOON'))).toBe(true);
      expect(isDashboardRetired(createDashboard('rolling-recap', 'Retiring Soon'))).toBe(true);
      expect(isDashboardRetired(createDashboard('rolling-recap', 'retiring'))).toBe(true);
    });

    it('should work for both retiring dashboard IDs', () => {
      expect(isDashboardRetired(createDashboard('rolling-recap', 'retiring'))).toBe(true);
      expect(isDashboardRetired(createDashboard('upgrade-posture', 'retiring'))).toBe(true);
    });
  });

  describe('filterRetiredDashboards', () => {
    const createDashboard = (dashboardId, spotlightText) => ({
      dashboardId,
      spotlightText,
    });

    let mockUtc;
    const originalUtc = moment.utc;

    beforeEach(() => {
      mockUtc = jest.spyOn(moment, 'utc').mockImplementation((date) => {
        if (date === undefined) {
          return originalUtc('2026-03-01');
        }
        return originalUtc(date);
      });
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('should return empty array when dashboards is null', () => {
      expect(filterRetiredDashboards(null)).toEqual([]);
    });

    it('should return empty array when dashboards is undefined', () => {
      expect(filterRetiredDashboards(undefined)).toEqual([]);
    });

    it('should return empty array when dashboards is empty', () => {
      expect(filterRetiredDashboards([])).toEqual([]);
    });

    it('should filter out retired dashboards', () => {
      const dashboards = [
        createDashboard('rolling-recap', 'retiring soon'),
        createDashboard('other-dashboard', 'NEW'),
        createDashboard('upgrade-posture', 'retiring'),
        createDashboard('another-dashboard', 'FEATURED'),
      ];

      const result = filterRetiredDashboards(dashboards);

      expect(result).toHaveLength(2);
      expect(result[0].dashboardId).toBe('other-dashboard');
      expect(result[1].dashboardId).toBe('another-dashboard');
    });

    it('should keep retiring dashboards when before retirement date', () => {
      mockUtc.mockImplementation((date) => {
        if (date === undefined) {
          return originalUtc('2026-01-01');
        }
        return originalUtc(date);
      });

      const dashboards = [createDashboard('rolling-recap', 'retiring soon'), createDashboard('other-dashboard', 'NEW')];

      const result = filterRetiredDashboards(dashboards);

      expect(result).toHaveLength(2);
    });

    it('should keep retiring dashboards without "retiring" text', () => {
      const dashboards = [createDashboard('rolling-recap', 'NEW'), createDashboard('other-dashboard', 'FEATURED')];

      const result = filterRetiredDashboards(dashboards);

      expect(result).toHaveLength(2);
    });
  });

  describe('getDaysUntilRetirement', () => {
    let mockUtc;
    const originalUtc = moment.utc;

    beforeEach(() => {
      mockUtc = jest.spyOn(moment, 'utc');
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('should return positive days when before retirement date', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-01-25') : originalUtc(date)));
      const days = getDaysUntilRetirement();
      expect(days).toBe(29);
    });

    it('should return 0 on retirement date', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-02-23') : originalUtc(date)));
      const days = getDaysUntilRetirement();
      expect(days).toBe(0);
    });

    it('should return negative days when after retirement date', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-03-01') : originalUtc(date)));
      const days = getDaysUntilRetirement();
      expect(days).toBe(-6);
    });

    it('should return 1 day before retirement', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-02-22') : originalUtc(date)));
      const days = getDaysUntilRetirement();
      expect(days).toBe(1);
    });

    it('should ignore time component and compare only dates', () => {
      mockUtc.mockImplementation((date) =>
        date === undefined ? originalUtc('2026-02-23T01:00:00') : originalUtc(date)
      );
      const days = getDaysUntilRetirement();
      expect(days).toBe(0);
    });
  });

  describe('getRetirementText', () => {
    let mockUtc;
    const originalUtc = moment.utc;

    beforeEach(() => {
      mockUtc = jest.spyOn(moment, 'utc');
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it('should return "Retired" when after retirement date', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-03-01') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retired');
    });

    it('should return "Retiring today" on retirement date', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-02-23') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retiring today');
    });

    it('should return "Retiring tomorrow" one day before retirement', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-02-22') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retiring tomorrow');
    });

    it('should return "Retiring in X days" when more than 1 day away', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-01-25') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retiring in 29 days');
    });

    it('should return correct text for 2 days', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-02-21') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retiring in 2 days');
    });

    it('should return correct text for 30 days', () => {
      mockUtc.mockImplementation((date) => (date === undefined ? originalUtc('2026-01-24') : originalUtc(date)));
      expect(getRetirementText()).toBe('Retiring in 30 days');
    });
  });

  describe('RETIREMENT_DATE constant', () => {
    it('should be a moment object', () => {
      expect(moment.isMoment(RETIREMENT_DATE)).toBe(true);
    });

    it('should be set to 2026-02-23', () => {
      expect(RETIREMENT_DATE.format('YYYY-MM-DD')).toBe('2026-02-23');
    });

    it('should be in UTC timezone', () => {
      expect(RETIREMENT_DATE.format('Z')).toBe('+00:00');
    });
  });

  describe('RETIRING_DASHBOARD_IDS constant', () => {
    it('should contain exactly 2 dashboard IDs', () => {
      expect(RETIRING_DASHBOARD_IDS).toHaveLength(2);
    });

    it('should contain rolling-recap', () => {
      expect(RETIRING_DASHBOARD_IDS).toContain('rolling-recap');
    });

    it('should contain upgrade-posture', () => {
      expect(RETIRING_DASHBOARD_IDS).toContain('upgrade-posture');
    });
  });
});
