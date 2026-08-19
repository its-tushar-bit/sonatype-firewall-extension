/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { filterToJson } from '../../../../main/frontend/dashboard/filter/dashboardFilterService';

describe('dashboardFilterService', function () {
  describe('filterToJson()', function () {
    var filter = {
      organizations: new Set(['orgId1', 'orgId2']),
      policyTypes: new Set(['QUALITY', 'OTHER', 'SECURITY']),
      policyWaiverReasons: new Set(),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set(['tagId1', 'tagId2', null]),
      applications: new Set(['applicationIdZ', 'applicationIdA', 'applicationIdQ', 'applicationIdR']),
      policyViolationStates: new Set(['OPEN', 'WAIVED']),
      maxDaysOld: 90,
      policyThreatLevels: [3, 6],
    };

    it('creates proper filter json representation', function () {
      var filterJson = filterToJson(filter);
      expect(filterJson.organizationFilters).toEqual(['orgId1', 'orgId2']);
      expect(filterJson.policyThreatCategoryFilters).toEqual(['QUALITY', 'OTHER', 'SECURITY']);
      expect(filterJson.stageTypeFilters).toEqual(['release', 'stage-release', 'build']);
      expect(filterJson.tagFilters).toEqual(['tagId1', 'tagId2', null]);
      expect(filterJson.applicationFilters).toEqual([
        'applicationIdZ',
        'applicationIdA',
        'applicationIdQ',
        'applicationIdR',
      ]);
      expect(filterJson.policyViolationStates).toEqual(['OPEN', 'WAIVED']);
      expect(filterJson.maxDaysOld).toEqual(90);
      expect(filterJson.minPolicyThreatLevel).toEqual(3);
      expect(filterJson.maxPolicyThreatLevel).toEqual(6);
    });
  });
});
