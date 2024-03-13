/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop, path } from 'ramda';

export const selectNamespaceConfusionProtectionTileSlice = prop('namespaceConfusionProtectionTile');

export const selectCurrentFilterKey = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('currentFilterKey')
);

export const selectComponentsRequestBody = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  selectCurrentFilterKey,
  (state, currentFilterKey) => {
    // formats current table sortConfiguration to the structure required by backend
    const formattedSortFields = state.namePatternsTableConfig[currentFilterKey].sortFields.map((columnObj, index) => ({
      sortableField: columnObj.columnName,
      asc: columnObj.dir === 'asc',
      sortPriority: index + 1,
    }));
    return {
      ...state.namePatternsTableConfig[currentFilterKey],
      sortFields: formattedSortFields,
    };
  }
);
export const selectCurrentPage = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  (state) => state.namePatternsTableConfig[state.currentFilterKey].page
);
export const selectSortFields = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  (state) => state.namePatternsTableConfig[state.currentFilterKey].sortFields
);
export const selectSearchFiltersValues = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  selectCurrentFilterKey,
  (namespaceConfusionProtectionTile, currentFilterKey) =>
    path(['searchFiltersValues', currentFilterKey], namespaceConfusionProtectionTile)
);

export const selectComponentNamePatterns = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  selectCurrentFilterKey,
  (namespaceConfusionProtectionTile, currentFilterKey) => {
    return namespaceConfusionProtectionTile.componentNamePatterns[currentFilterKey] ?? [];
  }
);

export const selectLoadingComponentNamePatterns = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('loadingComponentNamePatterns')
);

export const selectErrorComponentsTable = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('errorComponentsTable')
);

export const selectHasNextPage = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  selectCurrentFilterKey,
  (namespaceConfusionProtectionTile, currentFilterKey) =>
    path(['hasNextPage', currentFilterKey], namespaceConfusionProtectionTile)
);

export const selectErrorUpdatingComponentNamePattern = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('errorUpdatingComponentNamePattern')
);

export const selectUpdatingComponentNamePattern = createSelector(
  selectNamespaceConfusionProtectionTileSlice,
  prop('updatingComponentNamePattern')
);
