/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxH2,
  NxPageMain,
  NxTile,
  NxFieldset,
  NxRadio,
  NxFormSelect,
  NxDateInput,
  NxTooltip,
  NxTextInput,
  NxButton,
  NxButtonBar,
  NxInfoAlert,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { selectAggregate } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import { actions as firewallBulkWaiverActions } from '../firewallBulkWaiverSlice';
import BulkWaiveTitle from '../bulkWaiveTitle/BulkWaiveTitle';
import FirewallScopeDropdown from './firewallIqScopeDropdown/FirewallScopeDropdown';
import {
  useFirewallWaiverExpirations,
  waiverMatcherStrategy,
  isCustomExpiryTimeSelected,
  isCustomExpiryTimeValid,
  getExpirationDaysMessage,
} from '../firewallWaiverUtils';
import {
  selectFirewallBulkWaiverConfiguration,
  selectOnlyUnknownViolations,
  selectFirewallWaiverReasons,
  selectFirewallSelectedCount,
  selectHasMixedViolations,
  selectFirewallLoadingWaiverReasons,
  selectFirewallWaiverReasonsError,
  selectFirewallSelectedWaiverScope,
  selectBulkWaiveSource,
  selectSourceContext,
  selectOriginalAggregateState,
} from '../firewallBulkWaiverSelectors';
import { loadFirewallWaiverReasons } from '../firewallBulkWaiverActions';

export default function FirewallBulkWaiveConfigurationPage() {
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const { repositoryId } = routerParams;
  const waiverConfiguration = useSelector(selectFirewallBulkWaiverConfiguration);
  const onlyUnknownViolations = useSelector(selectOnlyUnknownViolations);
  const hasMixedViolations = useSelector(selectHasMixedViolations);
  const waiverReasons = useSelector(selectFirewallWaiverReasons);
  const selectedCount = useSelector(selectFirewallSelectedCount);
  const loadingWaiverReasons = useSelector(selectFirewallLoadingWaiverReasons);
  const waiverReasonsError = useSelector(selectFirewallWaiverReasonsError);
  const selectedWaiverScope = useSelector(selectFirewallSelectedWaiverScope);
  const source = useSelector(selectBulkWaiveSource);
  const sourceContext = useSelector(selectSourceContext);
  const originalAggregateState = useSelector(selectOriginalAggregateState);
  const currentAggregate = useSelector(selectAggregate);

  const [waiverReasonId, setWaiverReasonId] = useState(waiverConfiguration?.waiverReasonId || '');
  const [expiryTime, setExpiryTime] = useState(waiverConfiguration?.expiryTime || '');
  const [customExpiryTime, setCustomExpiryTime] = useState({
    value: waiverConfiguration?.customExpiryTime?.value || '',
    isPristine: waiverConfiguration?.customExpiryTime?.isPristine ?? true,
  });
  const [comments, setComments] = useState(waiverConfiguration?.comments || '');
  const [componentMatcherStrategy, setComponentMatcherStrategy] = useState(
    waiverConfiguration?.componentMatcherStrategy || waiverMatcherStrategy.ALL_VERSIONS
  );

  const waiverExpirations = useFirewallWaiverExpirations(false);

  const waiverReasonsToRender = waiverReasons || [];

  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);

  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  const loading = loadingWaiverReasons;

  const getErrorMessage = () => {
    if (waiverReasonsError) {
      return 'Error loading waiver reasons';
    }
    return '';
  };

  const retryHandler = () => {
    dispatch(loadFirewallWaiverReasons());
    if (repositoryId) {
      dispatch(actions.getRepositoryInformation(repositoryId));
    }
  };

  useEffect(() => {
    if (repositoryId) {
      dispatch(actions.getRepositoryInformation(repositoryId));
    }
    dispatch(loadFirewallWaiverReasons());
  }, [dispatch, repositoryId]);

  useEffect(() => {
    if (onlyUnknownViolations && componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS) {
      setComponentMatcherStrategy(waiverMatcherStrategy.EXACT_COMPONENT);
    }
  }, [onlyUnknownViolations, componentMatcherStrategy]);

  useEffect(() => {
    if (!customExpiryTimeSelected) {
      setCustomExpiryTime({ value: '', isPristine: true });
    }
  }, [customExpiryTimeSelected]);

  const onReasonChange = (value) => {
    setWaiverReasonId(value);
  };

  const onExpiryTimeChange = (value) => {
    setExpiryTime(value);
  };

  const handleCustomExpiryTimeChange = (time) => {
    const newValue = typeof time === 'string' ? time : time?.value || '';
    setCustomExpiryTime({
      value: newValue,
      isPristine: false,
    });
  };

  const onCommentsChange = (value) => {
    setComments(value);
  };

  const handleComponentMatcherStrategyChange = (value) => {
    setComponentMatcherStrategy(value);
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
      if (originalAggregateState !== null && currentAggregate !== originalAggregateState) {
        dispatch(actions.toggleAggregate());
      }
      dispatch(actions.clearFilters());
      dispatch(stateGo('firewall.repository-report', { repositoryId }));
      dispatch(firewallBulkWaiverActions.clearSourceContext());
    }

    dispatch(firewallBulkWaiverActions.clearOriginalAggregateState());
  };

  const handleBack = () => {
    if (source !== 'component-details') {
      if (originalAggregateState !== null && currentAggregate !== originalAggregateState) {
        dispatch(actions.toggleAggregate());
      }
    }

    dispatch(stateGo('firewall.bulkWaive', { repositoryId }));
  };

  const handleNext = () => {
    dispatch(
      firewallBulkWaiverActions.setWaiverConfiguration({
        waiverReasonId,
        expiryTime,
        customExpiryTime,
        comments,
        componentMatcherStrategy,
        selectedWaiverScope,
      })
    );
    dispatch(stateGo('firewall.bulkWaiveConfirmation', { repositoryId }));
  };

  return (
    <>
      <NxPageMain className="fw-bulk-waiver-configuration-page">
        <NxLoadWrapper loading={loading} error={getErrorMessage()} retryHandler={retryHandler}>
          <BulkWaiveTitle />
          <NxTile>
            <NxTile.Header>
              <NxH2>Waiver configuration for {selectedCount} selected violations</NxH2>
            </NxTile.Header>
            <NxTile.Content>
              <NxFieldset label="Scope" isRequired>
                <FirewallScopeDropdown id="fw-bulk-waive-scope-dropdown" />
              </NxFieldset>

              <NxFieldset label="Components" isRequired>
                <NxRadio
                  id="fw-bulk-waive-exact-component"
                  name="fw-bulk-waive-components"
                  value={waiverMatcherStrategy.EXACT_COMPONENT}
                  isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                  onChange={handleComponentMatcherStrategyChange}
                >
                  Exact
                </NxRadio>
                {onlyUnknownViolations ? (
                  <NxTooltip title="Claim these components to apply all versions waiver">
                    <NxRadio
                      id="fw-bulk-waive-all-versions"
                      name="fw-bulk-waive-components"
                      value={waiverMatcherStrategy.ALL_VERSIONS}
                      isChecked={false}
                      onChange={() => {}}
                      disabled={true}
                    >
                      All Versions
                    </NxRadio>
                  </NxTooltip>
                ) : (
                  <NxRadio
                    id="fw-bulk-waive-all-versions"
                    name="fw-bulk-waive-components"
                    value={waiverMatcherStrategy.ALL_VERSIONS}
                    isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
                    onChange={handleComponentMatcherStrategyChange}
                  >
                    All Versions
                  </NxRadio>
                )}
              </NxFieldset>

              {hasMixedViolations && componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS && (
                <NxInfoAlert id="iq-bulk-waiver-mixed-violations-alert">
                  The selected violations contain unknown/unclaimed components. When &quot;All Versions&quot; is
                  selected, the bulk waiver will only apply to identified components.
                </NxInfoAlert>
              )}

              <NxFieldset label="Waiver Expiration" isRequired>
                <div className="fw-bulk-waiver-form__select-block">
                  <NxFormSelect
                    id="fw-bulk-waiver-expiry-select"
                    value={expiryTime}
                    onChange={onExpiryTimeChange}
                    validatable={true}
                  >
                    <option value="" disabled>
                      Select
                    </option>
                    {waiverExpirations?.map(({ name, value }, index) => (
                      <option key={index} value={value}>
                        {name}
                      </option>
                    ))}
                  </NxFormSelect>
                  {customExpiryTimeSelected && (
                    <NxDateInput
                      className="fw-bulk-waiver-form__date-input"
                      value={customExpiryTime.value}
                      isPristine={customExpiryTime.isPristine}
                      onChange={handleCustomExpiryTimeChange}
                      validatable={true}
                      aria-label="set custom expiration date"
                    />
                  )}
                </div>
                {daysDiffMessage && expiryTime && expiryTime !== '' && (
                  <div className="fw-bulk-waiver-form__expiration-days-diff visual-testing-ignore">
                    {daysDiffMessage}
                  </div>
                )}
              </NxFieldset>

              <NxFieldset label="Reason">
                <NxFormSelect
                  id="fw-bulk-waiver-reason-select"
                  value={waiverReasonId}
                  onChange={onReasonChange}
                  validatable={true}
                >
                  <option value="" disabled>
                    Select
                  </option>
                  {waiverReasonsToRender.map(({ id, reasonText }) => (
                    <option key={id} value={id}>
                      {reasonText}
                    </option>
                  ))}
                </NxFormSelect>
              </NxFieldset>

              <NxFieldset label="Comments">
                <div className="fw-bulk-waiver-form__comments">
                  <NxTextInput
                    type="textarea"
                    maxLength={1000}
                    value={comments}
                    onChange={onCommentsChange}
                    className="nx-text-input--full"
                    isPristine={comments === ''}
                  />
                </div>
              </NxFieldset>

              <NxButtonBar>
                <NxButton variant="tertiary" onClick={handleCancel}>
                  Cancel
                </NxButton>
                <NxButton variant="secondary" onClick={handleBack}>
                  Back
                </NxButton>
                <NxButton
                  variant="primary"
                  disabled={
                    !selectedWaiverScope ||
                    !expiryTime ||
                    (customExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime?.value))
                  }
                  onClick={handleNext}
                >
                  Next
                </NxButton>
              </NxButtonBar>
            </NxTile.Content>
          </NxTile>
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
}
