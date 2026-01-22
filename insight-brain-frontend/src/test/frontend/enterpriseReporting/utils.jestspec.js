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
  getFormattedRetirementDate,
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

  describe('getFormattedRetirementDate', () => {
    it('should return date in MM/DD/YYYY format (US style)', () => {
      const formatted = getFormattedRetirementDate();
      expect(formatted).toBe('02/23/2026');
    });

    it('should format month with leading zero (US month first)', () => {
      const formatted = getFormattedRetirementDate();
      expect(formatted.split('/')[0]).toBe('02');
    });

    it('should format day with leading zero', () => {
      const formatted = getFormattedRetirementDate();
      expect(formatted.split('/')[1]).toBe('23');
    });

    it('should format year as four digits', () => {
      const formatted = getFormattedRetirementDate();
      expect(formatted.split('/')[2]).toBe('2026');
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
