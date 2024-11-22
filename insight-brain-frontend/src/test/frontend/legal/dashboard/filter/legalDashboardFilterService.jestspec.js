/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { filterToJson } from '../../../../../main/frontend/legal/dashboard/filter/legalDashboardFilterService';

describe('legalDashboardFilterService', function () {
  describe('filterToJson()', function () {
    var filter = {
      organizations: new Set(['orgId1', 'orgId2']),
      applications: new Set(['applicationIdZ', 'applicationIdA', 'applicationIdQ', 'applicationIdR']),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set(['tagId1', 'tagId2', null]),
      progressOptions: new Set(['NOT_REVIEWED']),
    };

    it('creates proper filter json representation', function () {
      var filterJson = filterToJson(filter);
      expect(filterJson.organizationFilters).toEqual(['orgId1', 'orgId2']);
      expect(filterJson.stageTypeFilters).toEqual(['release', 'stage-release', 'build']);
      expect(filterJson.categoryFilters).toEqual(['tagId1', 'tagId2', null]);
      expect(filterJson.applicationFilters).toEqual([
        'applicationIdZ',
        'applicationIdA',
        'applicationIdQ',
        'applicationIdR',
      ]);
      expect(filterJson.progressOptionsFilters).toEqual(['NOT_REVIEWED']);
    });
  });
});
