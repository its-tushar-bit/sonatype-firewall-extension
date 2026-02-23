/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectFirewallEnterpriseReporting,
  selectAllDashboards,
  selectDashboards,
  selectLoading,
  selectLoadError,
  selectIqVersion,
  selectBaseUrl,
  selectSelectedDashboard,
  selectSelectedDashboardName,
} from 'MainRoot/firewall/enterpriseReporting/firewallEnterpriseReportingSelectors';

describe('firewallEnterpriseReportingSelectors', () => {
  const mockState = {
    firewallEnterpriseReporting: {
      dashboards: [
        {
          dashboardId: 'malware-insights',
          title: 'Malware Insights',
          category: 'firewall',
          dashboardPath: 'firewall_malware::firewall_malware',
        },
        {
          dashboardId: 'security-overview',
          title: 'Security Overview',
          category: 'firewall',
          dashboardPath: 'firewall_security::firewall_security',
        },
        {
          dashboardId: 'lifecycle-dashboard',
          title: 'Lifecycle Dashboard',
          category: 'enterprise',
          dashboardPath: 'lifecycle::lifecycle',
        },
      ],
      loading: false,
      loadError: null,
      iqVersion: '1.170.0',
      baseUrl: 'looker.example.com',
      selectedDashboard: {
        dashboardId: 'malware-insights',
        dashboardPath: 'firewall_malware::firewall_malware',
        category: 'firewall',
      },
      selectedDashboardName: 'Malware Insights',
    },
  };

  describe('selectFirewallEnterpriseReporting', () => {
    it('should select the firewall enterprise reporting slice', () => {
      const result = selectFirewallEnterpriseReporting(mockState);
      expect(result).toEqual(mockState.firewallEnterpriseReporting);
    });
  });

  describe('selectAllDashboards', () => {
    it('should select all dashboards', () => {
      const result = selectAllDashboards(mockState);
      expect(result).toEqual(mockState.firewallEnterpriseReporting.dashboards);
      expect(result).toHaveLength(3);
    });

    it('should return empty array when no dashboards', () => {
      const emptyState = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          dashboards: [],
        },
      };
      const result = selectAllDashboards(emptyState);
      expect(result).toEqual([]);
    });
  });

  describe('selectDashboards', () => {
    it('should select only firewall category dashboards', () => {
      const result = selectDashboards(mockState);
      expect(result).toHaveLength(2);
      expect(result[0].category).toBe('firewall');
      expect(result[1].category).toBe('firewall');
    });

    it('should filter out non-firewall dashboards', () => {
      const result = selectDashboards(mockState);
      const lifecycleDashboard = result.find((d) => d.dashboardId === 'lifecycle-dashboard');
      expect(lifecycleDashboard).toBeUndefined();
    });

    it('should return empty array when no firewall dashboards', () => {
      const stateWithoutFirewall = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          dashboards: [
            {
              dashboardId: 'lifecycle-dashboard',
              title: 'Lifecycle Dashboard',
              category: 'enterprise',
            },
          ],
        },
      };
      const result = selectDashboards(stateWithoutFirewall);
      expect(result).toEqual([]);
    });
  });

  describe('selectLoading', () => {
    it('should select loading state', () => {
      const result = selectLoading(mockState);
      expect(result).toBe(false);
    });

    it('should return true when loading', () => {
      const loadingState = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          loading: true,
        },
      };
      const result = selectLoading(loadingState);
      expect(result).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('should select load error', () => {
      const result = selectLoadError(mockState);
      expect(result).toBe(null);
    });

    it('should return error message when present', () => {
      const errorState = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          loadError: 'Failed to load dashboards',
        },
      };
      const result = selectLoadError(errorState);
      expect(result).toBe('Failed to load dashboards');
    });
  });

  describe('selectIqVersion', () => {
    it('should select IQ version', () => {
      const result = selectIqVersion(mockState);
      expect(result).toBe('1.170.0');
    });

    it('should return null when version not loaded', () => {
      const stateWithoutVersion = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          iqVersion: null,
        },
      };
      const result = selectIqVersion(stateWithoutVersion);
      expect(result).toBe(null);
    });
  });

  describe('selectBaseUrl', () => {
    it('should select base URL', () => {
      const result = selectBaseUrl(mockState);
      expect(result).toBe('looker.example.com');
    });

    it('should return null when base URL not loaded', () => {
      const stateWithoutUrl = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          baseUrl: null,
        },
      };
      const result = selectBaseUrl(stateWithoutUrl);
      expect(result).toBe(null);
    });
  });

  describe('selectSelectedDashboard', () => {
    it('should select selected dashboard', () => {
      const result = selectSelectedDashboard(mockState);
      expect(result).toEqual({
        dashboardId: 'malware-insights',
        dashboardPath: 'firewall_malware::firewall_malware',
        category: 'firewall',
      });
    });

    it('should return null when no dashboard selected', () => {
      const stateWithoutSelection = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          selectedDashboard: null,
        },
      };
      const result = selectSelectedDashboard(stateWithoutSelection);
      expect(result).toBe(null);
    });
  });

  describe('selectSelectedDashboardName', () => {
    it('should select selected dashboard name', () => {
      const result = selectSelectedDashboardName(mockState);
      expect(result).toBe('Malware Insights');
    });

    it('should return null when no dashboard name', () => {
      const stateWithoutName = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          selectedDashboardName: null,
        },
      };
      const result = selectSelectedDashboardName(stateWithoutName);
      expect(result).toBe(null);
    });
  });

  describe('selector memoization', () => {
    it('should return same reference when state has not changed', () => {
      const result1 = selectDashboards(mockState);
      const result2 = selectDashboards(mockState);
      expect(result1).toBe(result2);
    });

    it('should return new reference when dashboards change', () => {
      const result1 = selectDashboards(mockState);

      const newState = {
        firewallEnterpriseReporting: {
          ...mockState.firewallEnterpriseReporting,
          dashboards: [
            ...mockState.firewallEnterpriseReporting.dashboards,
            {
              dashboardId: 'new-dashboard',
              category: 'firewall',
            },
          ],
        },
      };

      const result2 = selectDashboards(newState);
      expect(result1).not.toBe(result2);
    });
  });
});
