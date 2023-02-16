/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectNamespaceConfusionProtectionTileSlice = prop('namespaceConfusionProtectionTile');

export const selectComponentsRequestBody = createSelector(selectNamespaceConfusionProtectionTileSlice, (state) => {
  // formats current table sortConfiguration to the structure required by backend
  const formattedSortFields = state.namePatternsTableConfig.sortFields.map((columnObj, index) => ({
    sortableField: columnObj.columnName,
    asc: columnObj.dir === 'asc',
    sortPriority: index + 1,
  }));
  return { ...state.namePatternsTableConfig, sortFields: formattedSortFields };
});

export const selectCurrentPage = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  (state) => state.namePatternsTableConfig.page
);
export const selectSortFields = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  (state) => state.namePatternsTableConfig.sortFields
);
export const selectSearchFiltersValues = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('searchFiltersValues')
);

export const selectComponentNamePatterns = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('componentNamePatterns')
);

export const selectLoadingComponentNamePatterns = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('loadingComponentNamePatterns')
);

export const selectErrorComponentsTable = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('errorComponentsTable')
);

export const selectHasNextPage = createSelector(selectNamespaceConfusionProtectionTileSlice, prop('hasNextPage'));
