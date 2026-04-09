/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { prop, path } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

const selectFirewallBulkWaiverSlice = prop('firewallBulkWaiver');

export const selectFirewallBulkWaiverSelectedViolations = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['selectedViolations'])
);

export const selectFirewallSelectedCount = createSelector(selectFirewallBulkWaiverSlice, path(['selectedCount']));

export const selectFirewallSelectAllMode = createSelector(selectFirewallBulkWaiverSlice, path(['selectAllMode']));

export const selectFirewallCheckboxState = createSelector(selectFirewallBulkWaiverSlice, path(['checkboxState']));

export const selectFirewallBulkWaiverConfiguration = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['waiverConfiguration'])
);

export const selectFirewallWaiverReasons = createSelector(selectFirewallBulkWaiverSlice, path(['waiverReasons']));

export const selectFirewallLoadingWaiverReasons = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['loadingWaiverReasons'])
);

export const selectFirewallWaiverReasonsError = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['waiverReasonsError'])
);

export const selectFirewallAvailableWaiverScopes = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['availableWaiverScopes'])
);

export const selectFirewallLoadingWaiverScopes = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['loadingWaiverScopes'])
);

export const selectFirewallWaiverScopesError = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['waiverScopesError'])
);

export const selectFirewallSelectedWaiverScope = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['selectedWaiverScope'])
);

const isUnknownComponent = (component) => {
  return !component.matchState || component.matchState === 'unknown';
};

export const selectHasUnknownViolations = createSelector(selectFirewallBulkWaiverSelectedViolations, (violations) => {
  return violations.some(isUnknownComponent);
});

export const selectHasIdentifiedViolations = createSelector(
  selectFirewallBulkWaiverSelectedViolations,
  (violations) => {
    return violations.some((v) => !isUnknownComponent(v));
  }
);

export const selectHasMixedViolations = createSelector(
  selectHasUnknownViolations,
  selectHasIdentifiedViolations,
  (hasUnknown, hasIdentified) => {
    return hasUnknown && hasIdentified;
  }
);

export const selectOnlyUnknownViolations = createSelector(
  selectFirewallBulkWaiverSelectedViolations,
  selectHasIdentifiedViolations,
  (violations, hasIdentified) => {
    return violations.length > 0 && !hasIdentified;
  }
);

export const selectAllFilteredViolations = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['allFilteredViolations'])
);

export const selectLoadingAllViolations = createSelector(selectFirewallBulkWaiverSlice, path(['loadingAllViolations']));

export const selectAllViolationsError = createSelector(selectFirewallBulkWaiverSlice, path(['allViolationsError']));

export const selectTotalFilteredCount = createSelector(selectFirewallBulkWaiverSlice, path(['totalFilteredCount']));

export const selectSourceContext = createSelector(selectFirewallBulkWaiverSlice, path(['sourceContext']));

export const selectBulkWaiveSource = createSelector(selectSourceContext, path(['source']));

export const selectBulkWaiveComponentPathname = createSelector(selectSourceContext, path(['pathname']));

export const selectBulkWaiveComponentDisplayName = createSelector(selectSourceContext, path(['componentDisplayName']));

export const selectSubmitting = createSelector(selectFirewallBulkWaiverSlice, path(['submitting']));

export const selectSubmitSuccess = createSelector(selectFirewallBulkWaiverSlice, path(['submitSuccess']));

export const selectSubmitError = createSelector(selectFirewallBulkWaiverSlice, path(['submitError']));

export const selectOriginalAggregateState = createSelector(
  selectFirewallBulkWaiverSlice,
  path(['originalAggregateState'])
);
