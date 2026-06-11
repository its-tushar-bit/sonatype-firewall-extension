/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxPageMain,
  NxTile,
  NxH2,
  NxFieldset,
  NxReadOnly,
  categoryByPolicyThreatLevel,
  NxThreatCounter,
  NxInfoAlert,
  NxErrorAlert,
  NxButtonBar,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import {
  selectAggregate,
  selectComponentsRequestBody,
} from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import {
  selectFirewallBulkWaiverSelectedViolations,
  selectFirewallBulkWaiverConfiguration,
  selectHasMixedViolations,
  selectFirewallWaiverReasons,
  selectBulkWaiveSource,
  selectSourceContext,
  selectFirewallSelectedCount,
  selectFirewallSelectAllMode,
  selectFirewallCheckboxState,
  selectAllFilteredViolations,
  selectLoadingAllViolations,
  selectAllViolationsError,
  selectSubmitting,
  selectSubmitSuccess,
  selectSubmitError,
} from '../firewallBulkWaiverSelectors';
import { actions as firewallBulkWaiverActions } from '../firewallBulkWaiverSlice';
import { loadAllFilteredViolations, submitFirewallBulkWaiver } from '../firewallBulkWaiverActions';
import { isCustomExpiryTimeSelected, isCustomExpiryTimeValid, waiverMatcherStrategy } from '../firewallWaiverUtils';
import moment from 'moment';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import BulkWaiveTitle from '../bulkWaiveTitle/BulkWaiveTitle';

export default function FirewallBulkWaiveConfirmationPage() {
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const { repositoryId } = routerParams;
  const selectedViolations = useSelector(selectFirewallBulkWaiverSelectedViolations);
  const storedSelectedCount = useSelector(selectFirewallSelectedCount);
  const selectAllMode = useSelector(selectFirewallSelectAllMode);
  const checkboxState = useSelector(selectFirewallCheckboxState);
  const selectedViolationsCount = selectAllMode ? storedSelectedCount : selectedViolations?.length || 0;
  const waiverConfiguration = useSelector(selectFirewallBulkWaiverConfiguration);
  const waiverReasons = useSelector(selectFirewallWaiverReasons);
  const hasMixedViolations = useSelector(selectHasMixedViolations);
  const source = useSelector(selectBulkWaiveSource);
  const sourceContext = useSelector(selectSourceContext);

  const allFilteredViolations = useSelector(selectAllFilteredViolations);
  const loadingAllViolations = useSelector(selectLoadingAllViolations);
  const allViolationsError = useSelector(selectAllViolationsError);

  const submitting = useSelector(selectSubmitting);
  const submitSuccess = useSelector(selectSubmitSuccess);
  const submitError = useSelector(selectSubmitError);

  const currentAggregate = useSelector(selectAggregate);
  const componentsRequestBody = useSelector(selectComponentsRequestBody);

  const selectedAllFilteredViolations = useMemo(
    () =>
      selectAllMode && allFilteredViolations.length > 0
        ? allFilteredViolations.filter((violation) => checkboxState?.[violation.policyViolationId] !== false)
        : [],
    [selectAllMode, allFilteredViolations, checkboxState]
  );
  const selectedAllFilteredViolationIds = selectedAllFilteredViolations
    .map((violation) => violation.policyViolationId)
    .join('|');
  const selectedViolationIds = (selectedViolations || []).map((violation) => violation.policyViolationId).join('|');

  const violationsForCounts =
    selectAllMode && selectedAllFilteredViolations.length > 0 ? selectedAllFilteredViolations : selectedViolations;

  const componentCount =
    selectAllMode && violationsForCounts.length <= 5
      ? 0
      : new Set(violationsForCounts.map((violation) => violation.componentDisplayText)).size;

  const getPolicyThreatLevelCounts = () => {
    const counts = {};

    violationsForCounts.forEach((violation) => {
      const category = categoryByPolicyThreatLevel[violation.threatLevel];
      if (category) {
        const countKey = `${category}Count`;
        counts[countKey] = (counts[countKey] || 0) + 1;
      }
    });

    return counts;
  };

  const policyThreatCounts = getPolicyThreatLevelCounts();

  const quarantinedCount = violationsForCounts.filter((v) => v.quarantineTime != null).length;
  const nonQuarantinedCount = violationsForCounts.filter((v) => v.quarantineTime == null).length;
  const hasNonQuarantinedViolations = nonQuarantinedCount > 0;

  useEffect(() => {
    if (selectAllMode && source !== 'component-details' && repositoryId) {
      dispatch(loadAllFilteredViolations(repositoryId, componentsRequestBody));
    }
  }, [dispatch, selectAllMode, source, repositoryId, componentsRequestBody]);

  useEffect(() => {
    if (
      selectAllMode &&
      allFilteredViolations.length > 0 &&
      !loadingAllViolations &&
      selectedAllFilteredViolationIds !== selectedViolationIds
    ) {
      dispatch(firewallBulkWaiverActions.setSelectedViolations(selectedAllFilteredViolations));
    }
  }, [
    selectAllMode,
    allFilteredViolations,
    loadingAllViolations,
    selectedAllFilteredViolations,
    selectedAllFilteredViolationIds,
    selectedViolationIds,
  ]); // dispatch is stable (initialized once) and intentionally omitted

  const formatScope = () => {
    const scope = waiverConfiguration?.selectedWaiverScope;
    if (!scope) {
      return '--';
    }

    const type =
      scope.ownerType === 'application'
        ? 'Application'
        : scope.ownerType === 'organization'
        ? 'Organization'
        : 'Repository';

    const name = scope.ownerName || scope.name || scope.id || '--';
    return `${type} : ${name}`;
  };

  const formatComponents = () => {
    if (waiverConfiguration?.componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS) {
      return 'All Versions';
    } else if (waiverConfiguration?.componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT) {
      return 'Exact';
    }
    return '--';
  };

  const formatExpiration = () => {
    const expiry = waiverConfiguration?.expiryTime;
    const customExpiry = waiverConfiguration?.customExpiryTime;

    if (expiry === null || expiry === 'never') return 'Never';
    if (isCustomExpiryTimeSelected(expiry) && isCustomExpiryTimeValid(customExpiry?.value)) {
      const today = moment().startOf('day');
      const customDate = moment(customExpiry.value, 'YYYY-MM-DD');
      const diff = Math.round(moment.duration(customDate.diff(today)).asDays());
      return `${diff} days`;
    }
    if (expiry) return `${expiry} days`;
    return '--';
  };

  const formatReason = () => {
    const reasonId = waiverConfiguration?.waiverReasonId;
    if (!reasonId) return '--';

    const reason = waiverReasons?.find((r) => r.id === reasonId);
    return reason?.reasonText || reasonId;
  };

  const formatComments = () => {
    return waiverConfiguration?.comments || '--';
  };

  const formatViolationsSummary = (violationsCount, componentsCount) => {
    const violationsText = violationsCount === 1 ? 'violation' : 'violations';
    if (selectAllMode && componentsCount === 0) {
      return `${violationsCount} total ${violationsText}`;
    }
    const componentsText = componentsCount === 1 ? 'component' : 'components';
    return `${violationsCount} total ${violationsText} across ${componentsCount} ${componentsText}`;
  };

  const handleBack = () => {
    dispatch(stateGo('firewall.bulkWaiveConfiguration', { repositoryId }));
  };

  const handleSubmit = async () => {
    const violationsToSubmit =
      selectAllMode && selectedAllFilteredViolations.length > 0 ? selectedAllFilteredViolations : selectedViolations;

    try {
      await dispatch(
        submitFirewallBulkWaiver({
          repositoryId,
          selectedViolations: violationsToSubmit,
          waiverConfiguration,
        })
      );
    } catch (error) {
      // Error is handled by Redux state (submitError) and displayed via toast
    }
  };

  const handleCancel = () => {
    dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
    dispatch(firewallBulkWaiverActions.setSelectedCount(0));
    dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
    dispatch(firewallBulkWaiverActions.setCheckboxState({}));
    dispatch(firewallBulkWaiverActions.setAllFilteredViolations([]));
    dispatch(firewallBulkWaiverActions.clearWaiverConfiguration());

    if (source === 'component-details' && sourceContext) {
      dispatch(
        stateGo('firewall.componentDetailsPage.violations', {
          repositoryId: sourceContext.repositoryId,
          componentIdentifier: sourceContext.componentIdentifier,
          componentHash: sourceContext.componentHash,
          matchState: sourceContext.matchState,
          pathname: sourceContext.pathname,
          componentDisplayName: sourceContext.componentDisplayName,
          tabId: sourceContext.tabId || 'violations',
        })
      );
      dispatch(firewallBulkWaiverActions.clearSourceContext());
    } else {
      if (!currentAggregate) {
        dispatch(actions.toggleAggregate());
      }
      dispatch(actions.clearFilters());
      dispatch(stateGo('firewall.repository-report', { repositoryId }));
      dispatch(firewallBulkWaiverActions.clearSourceContext());
    }

    dispatch(firewallBulkWaiverActions.clearOriginalAggregateState());
  };

  useEffect(() => {
    if (submitSuccess) {
      const currentSource = source;
      const currentSourceContext = sourceContext;

      if (currentSource === 'component-details' && currentSourceContext) {
        dispatch(
          stateGo('firewall.componentDetailsPage.violations', {
            repositoryId: currentSourceContext.repositoryId,
            componentIdentifier: currentSourceContext.componentIdentifier,
            componentHash: currentSourceContext.componentHash,
            matchState: currentSourceContext.matchState,
            pathname: currentSourceContext.pathname,
            componentDisplayName: currentSourceContext.componentDisplayName,
            tabId: currentSourceContext.tabId || 'violations',
          })
        );
      } else {
        if (!currentAggregate) {
          dispatch(actions.toggleAggregate());
        }
        dispatch(actions.clearFilters());
        dispatch(stateGo('firewall.repository-report', { repositoryId }));
      }

      dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
      dispatch(firewallBulkWaiverActions.setSelectedCount(0));
      dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
      dispatch(firewallBulkWaiverActions.setCheckboxState({}));
      dispatch(firewallBulkWaiverActions.setAllFilteredViolations([]));
      dispatch(firewallBulkWaiverActions.clearWaiverConfiguration());
      dispatch(firewallBulkWaiverActions.setSubmitSuccess(false));
      dispatch(firewallBulkWaiverActions.clearSourceContext());
      dispatch(firewallBulkWaiverActions.clearOriginalAggregateState());

      setTimeout(() => {
        dispatch(
          toastActions.addToast({
            type: 'success',
            message: 'Bulk Waivers will apply when report is re-evaluated',
          })
        );
      }, 500);
    }
  }, [submitSuccess, dispatch, repositoryId, source, sourceContext]);

  useEffect(() => {
    if (submitError) {
      dispatch(
        toastActions.addToast({
          type: 'error',
          message: submitError,
        })
      );
    }
  }, [submitError, dispatch]);

  const retryLoadingAllViolations = () => {
    if (selectAllMode && source !== 'component-details' && repositoryId) {
      dispatch(loadAllFilteredViolations(repositoryId, componentsRequestBody));
    }
  };

  return (
    <NxPageMain className="fw-bulk-waiver-confirmation-page">
      <BulkWaiveTitle />
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Confirmation</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>

        <NxTile.Content>
          {submitError && <NxErrorAlert id="fw-bulk-waiver-submit-error">{submitError}</NxErrorAlert>}
          {hasNonQuarantinedViolations && (
            <NxInfoAlert id="fw-bulk-waiver-non-quarantined-warning">
              {nonQuarantinedCount} of the selected violations are not quarantined and will be skipped. Only quarantined
              components can be bulk waived. {quarantinedCount} quarantined
              {quarantinedCount === 1 ? ' violation' : ' violations'} will be waived.
            </NxInfoAlert>
          )}
          <NxFieldset label="Violations being waived">
            <NxReadOnly>
              <NxReadOnly.Data>{formatViolationsSummary(selectedViolationsCount, componentCount)}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>
          <NxFieldset label="Policy Violations being waived">
            {selectAllMode && source !== 'component-details' ? (
              <NxLoadWrapper
                loading={loadingAllViolations}
                error={allViolationsError ? 'Failed to load threat level breakdown' : null}
                retryHandler={retryLoadingAllViolations}
              >
                <NxThreatCounter
                  criticalCount={policyThreatCounts.criticalCount || null}
                  severeCount={policyThreatCounts.severeCount || null}
                  moderateCount={policyThreatCounts.moderateCount || null}
                  lowCount={policyThreatCounts.lowCount || null}
                  noneCount={policyThreatCounts.noneCount || null}
                  unspecifiedCount={policyThreatCounts.unspecifiedCount || null}
                  layout="column"
                />
              </NxLoadWrapper>
            ) : (
              <NxThreatCounter
                criticalCount={policyThreatCounts.criticalCount || null}
                severeCount={policyThreatCounts.severeCount || null}
                moderateCount={policyThreatCounts.moderateCount || null}
                lowCount={policyThreatCounts.lowCount || null}
                noneCount={policyThreatCounts.noneCount || null}
                unspecifiedCount={policyThreatCounts.unspecifiedCount || null}
                layout="column"
              />
            )}
          </NxFieldset>
          <NxFieldset label="Scope">
            <NxReadOnly>
              <NxReadOnly.Data>{formatScope()}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>
          <NxFieldset label="Components">
            <NxReadOnly>
              <NxReadOnly.Data>{formatComponents()}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>
          {hasMixedViolations &&
            waiverConfiguration?.componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS && (
              <NxInfoAlert id="fw-bulk-waiver-mixed-violations-alert">
                The selected violations contain unknown/unclaimed components. When &quot;All Versions&quot; is selected,
                the bulk waiver will only apply to identified components.
              </NxInfoAlert>
            )}
          <NxFieldset label="Waiver Expiration">
            <NxReadOnly>
              <NxReadOnly.Data>{formatExpiration()}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>
          <NxFieldset label="Waiver Reason">
            <NxReadOnly>
              <NxReadOnly.Data>{formatReason()}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>
          <NxFieldset label="Comment">
            <NxReadOnly>
              <NxReadOnly.Data>{formatComments()}</NxReadOnly.Data>
            </NxReadOnly>
          </NxFieldset>

          <NxButtonBar>
            <NxButton
              variant="tertiary"
              data-analytics-id="fw-bulk-waive-step3-cancel-button"
              onClick={handleCancel}
              disabled={submitting}
            >
              Cancel
            </NxButton>
            <NxButton
              variant="secondary"
              data-analytics-id="fw-bulk-waive-step3-back-button"
              onClick={handleBack}
              disabled={submitting}
            >
              Back
            </NxButton>
            <NxButton
              variant="primary"
              data-analytics-id="fw-bulk-waive-step3-submit-button"
              onClick={handleSubmit}
              disabled={
                submitting ||
                (selectAllMode && source !== 'component-details' && (loadingAllViolations || !!allViolationsError))
              }
            >
              {submitting ? 'Submitting...' : 'Submit'}
            </NxButton>
          </NxButtonBar>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
