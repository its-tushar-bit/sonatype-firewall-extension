/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';
import applicationsHistoryTileSlice from './applicationsHistoryTile/applicationsHistoryTileSlice';
import highPriorityVulnerabilitiesTileSlice from './highPriorityVulnerabilitiesTile/highPriorityVulnerabilitiesTileSlice';
import vulnerabilitiesByThreatLevelTileSlice from './vulnerabilitiesByThreatLevelTile/vulnerabilitiesByThreatLevelTileSlice';
import recentlyImportedSbomsTileSlice from './recentlyImportedSbomsTile/recentlyImportedSbomsTileSlice';
import sbomCountsSlice from './sbomCountsSlice';

export default combineReducers({
  sbomCounts: sbomCountsSlice,
  applicationsHistoryTile: applicationsHistoryTileSlice,
  highPriorityVulnerabilitiesTile: highPriorityVulnerabilitiesTileSlice,
  vulnerabilitiesByThreatLevelTile: vulnerabilitiesByThreatLevelTileSlice,
  recentlyImportedSbomsTile: recentlyImportedSbomsTileSlice,
});
