/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectExportRequestData, selectExportUrl } from '../../../main/frontend/dashboard/dashboardSelectors';
import * as CLMLocations from '../../../main/frontend/util/CLMLocation';

describe('dashboardSelectors', function () {
  let state;
  beforeEach(() => {
    state = {
      router: { currentState: { name: 'dashboard.overview.violations' } },
      dashboardFilter: {
        appliedFilter: {
          organizations: new Set(),
          applications: new Set(),
          repositories: new Set(),
          categories: new Set(),
          stages: new Set(),
          policyTypes: new Set(),
          policyWaiverReasonIds: new Set(['some-id']),
          policyViolationStates: new Set(['OPEN']),
          maxDaysOld: 30,
          policyThreatLevels: [2, 10],
          expirationDate: 'ALL',
        },
      },
      dashboard: {
        applications: { sortFields: [] },
        components: { sortFields: [] },
        repositories: { sortFields: [] },
        violations: { sortFields: [] },
        waivers: { sortFields: [] },
        waiverRequests: { sortFields: [] },
      },
    };
  });

  describe('selectExportRequestData', () => {
    it('combines the filters and the router slices, along with dashboard slice into a request data object', () => {
      const expected = {
        organizationIds: [],
        applicationIds: [],
        repositoryIds: [],
        stageIds: [],
        tagIds: [],
        policyViolationStates: ['OPEN'],
        maxDaysOld: 30,
        policyThreatLevelRange: '2,10',
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };
      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });

    it('returns an empty object when state name is not one of the dashboard views', function () {
      state.router.currentState.name = 'Foo';
      const actual = selectExportRequestData(state);
      expect(actual).toEqual({});
    });

    it('converts filters to json string with default violations sortFields', function () {
      state.router.currentState.name = 'dashboard.overview.violations';
      state.dashboard.violations.sortFields = ['-firstOccurrenceTime', '-threatLevel'];

      const expected = {
        applicationIds: [],
        maxDaysOld: 30,
        orderBy: '-AGE,-THREAT_LEVEL',
        organizationIds: [],
        repositoryIds: [],
        policyThreatLevelRange: '2,10',
        policyViolationStates: ['OPEN'],
        stageIds: [],
        tagIds: [],
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };

      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });

    it('converts filters to json string with default components sortFields', function () {
      state.router.currentState.name = 'dashboard.overview.components';
      state.dashboard.components.sortFields = ['-score'];

      // Hmm, same as applications? - BigAB
      const expected = {
        applicationIds: [],
        maxDaysOld: 30,
        orderBy: '-TOTAL_RISK',
        organizationIds: [],
        repositoryIds: [],
        policyThreatLevelRange: '2,10',
        policyViolationStates: ['OPEN'],
        stageIds: [],
        tagIds: [],
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };

      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });

    it('converts filters to json string with default applications sortFields', function () {
      state.router.currentState.name = 'dashboard.overview.applications';
      state.dashboard.applications.sortFields = ['-totalApplicationRisk.totalRisk'];

      const expected = {
        applicationIds: [],
        maxDaysOld: 30,
        orderBy: '-TOTAL_RISK',
        organizationIds: [],
        repositoryIds: [],
        policyThreatLevelRange: '2,10',
        policyViolationStates: ['OPEN'],
        stageIds: [],
        tagIds: [],
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };

      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });

    it('converts filters to json string with default waivers sortFields', function () {
      state.router.currentState.name = 'dashboard.overview.waivers';
      state.dashboard.waivers.sortFields = ['-scope'];

      const expected = {
        applicationIds: [],
        maxDaysOld: 30,
        orderBy: '-OWNER_SCOPE',
        organizationIds: [],
        repositoryIds: [],
        policyThreatLevelRange: '2,10',
        policyViolationStates: ['OPEN'],
        stageIds: [],
        tagIds: [],
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };

      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });

    it('converts filters to json string with default waivers sortFields for waiver requests', function () {
      state.router.currentState.name = 'dashboard.overview.waiverRequests';
      state.dashboard.waivers.sortFields = ['-scope'];

      const expected = {
        applicationIds: [],
        maxDaysOld: 30,
        organizationIds: [],
        repositoryIds: [],
        policyThreatLevelRange: '2,10',
        policyViolationStates: ['OPEN'],
        stageIds: [],
        tagIds: [],
        expirationDate: 'ALL',
        policyWaiverReasonIds: ['some-id'],
      };

      const actual = selectExportRequestData(state);
      expect(actual).toEqual(expected);
    });
  });

  // These tests...  they're not... you know, good - BigAB
  describe('selectExportUrl', () => {
    it('returns an empty string when router state current state name is not one of the dashboard views', () => {
      state.router.currentState.name = 'Foo';
      const actual = selectExportUrl(state);
      expect(actual).toBe('');
    });

    it('uses violations export URL when on violations view', () => {
      state.router.currentState.name = 'dashboard.overview.violations';
      const actual = selectExportUrl(state);
      expect(actual).toBe(CLMLocations.getNewestRisksExportUrl());
    });

    it('uses components export URL when on components view', () => {
      state.router.currentState.name = 'dashboard.overview.components';
      const actual = selectExportUrl(state);
      expect(actual).toBe(CLMLocations.getComponentRisksExportUrl());
    });

    it('uses applications export URL when on applications view', () => {
      state.router.currentState.name = 'dashboard.overview.applications';
      const actual = selectExportUrl(state);
      expect(actual).toBe(CLMLocations.getApplicationRisksExportUrl());
    });

    it('uses applications export URL when on waivers view', () => {
      state.router.currentState.name = 'dashboard.overview.waivers';
      const actual = selectExportUrl(state);
      expect(actual).toBe(CLMLocations.getWaiversExportUrl());
    });
  });
});
