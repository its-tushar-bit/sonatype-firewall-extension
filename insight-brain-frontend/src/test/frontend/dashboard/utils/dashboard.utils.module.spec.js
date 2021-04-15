/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardUtilsModule from '../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('dashboard.utils.module', function () {
  beforeEach(angular.mock.module(dashboardUtilsModule.name));

  describe('createDashboardDataRequestPayload()', function () {
    var createDashboardDataRequestPayload;

    beforeEach(inject(function ($injector) {
      createDashboardDataRequestPayload = $injector.get(
        'createDashboardDataRequestPayload'
      );
    }));

    it('sets maxResults as specified', function () {
      expect(createDashboardDataRequestPayload(null, 1)).toEqual({
        maxResults: 1,
      });
    });

    it('converts policyThreatLevel to string', function () {
      var params = createDashboardDataRequestPayload({
        policyThreatLevels: [2, 7],
      });
      expect(params.policyThreatLevelRange).toBe('2,7');
    });

    it('does not set policyThreatLevelRange if policyThreatLevels is undefined', function () {
      var params = createDashboardDataRequestPayload({
        policyThreatLevels: undefined,
      });
      expect(params.policyThreatLevelRange).toBeUndefined();
    });

    it('sets applicationIds to array of provided ids', function () {
      var filter = { applications: new Set(['app1', 'app2']) };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.applicationIds).toEqual(['app1', 'app2']);
    });

    it('does not set applicationIds if applications is undefined', function () {
      var params = createDashboardDataRequestPayload({
        applications: undefined,
      });
      expect(params.applicationIds).toBeUndefined();
    });

    it('sets organizationIds to array of provided ids', function () {
      var filter = { organizations: new Set(['org1', 'org2']) };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.organizationIds).toEqual(['org1', 'org2']);
    });

    it('does not set organizationIds if organizations is undefined', function () {
      var params = createDashboardDataRequestPayload({
        organizations: undefined,
      });
      expect(params.organizationIds).toBeUndefined();
    });

    it('does not set policyThreatCategories if policyTypes is empty', function () {
      var params = createDashboardDataRequestPayload({
        policyTypes: new Set(),
      });
      expect(params.policyThreatCategories).toBeUndefined();
    });

    it('converts policyTypes to string', function () {
      var params = createDashboardDataRequestPayload({
        policyTypes: new Set(['SECURITY', 'LICENSE']),
      });
      expect(params.policyThreatCategories).toBe('SECURITY,LICENSE');
    });

    it('sets stageIds to array of provided stages', function () {
      var filter = { stages: new Set(['stage1', 'stage2']) };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.stageIds).toEqual(['stage1', 'stage2']);
    });

    it('does not set stageIds if stages is undefined', function () {
      var params = createDashboardDataRequestPayload({
        stages: undefined,
      });
      expect(params.stageIds).toBeUndefined();
    });

    it('sets tagIds to array of provided categories', function () {
      var filter = { categories: new Set(['tag1', 'tag2']) };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.tagIds).toEqual(['tag1', 'tag2']);
    });

    it('does not set tagIds if categories is undefined', function () {
      var params = createDashboardDataRequestPayload({
        categories: undefined,
      });
      expect(params.tagIds).toBeUndefined();
    });

    it('sets policyViolationStates to array of provided states', function () {
      var filter = { policyViolationStates: new Set(['OPEN', 'WAIVED']) };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.policyViolationStates).toEqual(['OPEN', 'WAIVED']);
    });

    it('does not set policyViolationStates if provided policyViolationStates is undefined', function () {
      var params = createDashboardDataRequestPayload({
        policyViolationStates: undefined,
      });
      expect(params.policyViolationStates).toBeUndefined();
    });

    it('sets maxDaysOld to provided value', function () {
      var filter = {
        maxDaysOld: 90,
      };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.maxDaysOld).toBe(90);
    });

    it('does not set maxDaysOld if provided value is undefined', function () {
      var params = createDashboardDataRequestPayload({});
      expect(params.maxDaysOld).toBeUndefined();
    });

    it('ignores null sortFields', function () {
      var request = createDashboardDataRequestPayload({}, null, null);
      expect(request.orderBy).toBeUndefined();
    });

    it('ignores undefined sortFields', function () {
      var request = createDashboardDataRequestPayload({});
      expect(request.orderBy).toBeUndefined();
    });

    it('ignores empty sortFields array', function () {
      var request = createDashboardDataRequestPayload({}, null, []);
      expect(request.orderBy).toBeUndefined();
    });

    it('sets orderBy if sortFields array is not empty', function () {
      var request = createDashboardDataRequestPayload({}, null, [
        '-foo',
        'bar',
      ]);
      expect(request.orderBy).toBe('-foo,bar');
    });
  });
});
