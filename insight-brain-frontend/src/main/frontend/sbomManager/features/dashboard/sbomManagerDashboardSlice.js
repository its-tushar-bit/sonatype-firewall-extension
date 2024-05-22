/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { combineReducers } from 'redux';

import totalSbomsStoredTileSlice from './totalSbomsStoredTile/totalSbomsStoredTileSlice';
import applicationsHistoryTileSlice from './applicationsHistoryTile/applicationsHistoryTileSlice';
import vulnerabilitiesByThreatLevelTileSlice from './vulnerabilitiesByThreatLevelTile/vulnerabilitiesByThreatLevelTileSlice';

export default combineReducers({
  totalSbomsStoredTile: totalSbomsStoredTileSlice,
  applicationsHistoryTile: applicationsHistoryTileSlice,
  vulnerabilitiesByThreatLevelTile: vulnerabilitiesByThreatLevelTileSlice,
});
