/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createDashboardDataRequestPayload } from 'MainRoot/dashboard/utils/dashboardUtils';

describe('dashboardUtils', function () {
  describe('createDashboardDataRequestPayload()', function () {
    it('sets pageSize as specified', function () {
      expect(createDashboardDataRequestPayload(null, 1)).toEqual({
        pageSize: 1,
      });
    });

    it('sets page as specified', function () {
      expect(createDashboardDataRequestPayload(null, null, null, 12)).toEqual({
        page: 12,
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
      var request = createDashboardDataRequestPayload({}, null, ['-foo', 'bar']);
      expect(request.orderBy).toBe('-foo,bar');
    });

    it('sets componentName to provided value', function () {
      var filter = {
        componentName: 'commons-io',
      };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.componentName).toBe('commons-io');
    });

    it('does not set componentName if provided value is empty string', function () {
      var params = createDashboardDataRequestPayload({
        componentName: '',
      });
      expect(params.componentName).toBeUndefined();
    });

    it('does not set componentName if provided value is undefined', function () {
      var params = createDashboardDataRequestPayload({});
      expect(params.componentName).toBeUndefined();
    });

    it('sets repositoryPublicId to provided value', function () {
      var filter = {
        repositoryPublicId: 'my-repo',
      };
      var params = createDashboardDataRequestPayload(filter);
      expect(params.repositoryPublicId).toBe('my-repo');
    });

    it('does not set repositoryPublicId if provided value is empty string', function () {
      var params = createDashboardDataRequestPayload({
        repositoryPublicId: '',
      });
      expect(params.repositoryPublicId).toBeUndefined();
    });

    it('does not set repositoryPublicId if provided value is undefined', function () {
      var params = createDashboardDataRequestPayload({});
      expect(params.repositoryPublicId).toBeUndefined();
    });
  });
});
