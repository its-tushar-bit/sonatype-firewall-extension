/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
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
  NxForm,
  NxInfoAlert,
} from '@sonatype/react-shared-components';
import {
  selectBulkWaiverSelectedViolations,
  selectBulkWaiverConfiguration,
  selectHasMixedViolations,
} from './bulkWaiverSelectors';
import { selectWaiverReasons } from './requestWaiverSelectors';
import { actions as waiverActions } from './waiverSlice';
import { isCustomExpiryTimeSelected, isCustomExpiryTimeValid } from 'MainRoot/util/waiverUtils';
import moment from 'moment';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { goToBulkWaivePage, cancelBulkWaive, goToWaiverConfigurationPage } from './waiverActions';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import BulkWaiveTitle from './BulkWaiveTitle';

export default function WaiverConfirmationPage() {
  const dispatch = useDispatch();
  const selectedViolations = useSelector(selectBulkWaiverSelectedViolations);
  const selectedViolationsCount = selectedViolations?.length || 0;
  const waiverConfiguration = useSelector(selectBulkWaiverConfiguration);
  const waiverReasons = useSelector(selectWaiverReasons);
  const hasMixedViolations = useSelector(selectHasMixedViolations);
  const { submitMaskState, submitError } = useSelector((state) => state.waivers.bulkWaive);
  const componentCount = new Set(selectedViolations.map((violation) => violation.derivedComponentName)).size;

  const getPolicyThreatLevelCounts = () => {
    const counts = {};

    selectedViolations.forEach((violation) => {
      const category = categoryByPolicyThreatLevel[violation.policyThreatLevel];
      if (category) {
        const countKey = `${category}Count`;
        counts[countKey] = (counts[countKey] || 0) + 1;
      }
    });

    return counts;
  };

  const policyThreatCounts = getPolicyThreatLevelCounts();

  // Helper functions to format display values
  const formatScope = () => {
    const scope = waiverConfiguration?.selectedWaiverScope;
    if (!scope) {
      return '--';
    } else if (scope.label === 'Repository_container') {
      return scope.name;
    } else {
      return `${scope.label} - ${scope.name}`;
    }
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

    if (expiry === null) return 'Never';
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
    return reason?.reasonText || reasonId; // Fallback to ID if text not found
  };

  const formatComments = () => {
    return waiverConfiguration?.comments || '--';
  };

  const formatViolationsSummary = (violationsCount, componentsCount) => {
    const violationsText = violationsCount === 1 ? 'violation' : 'violations';
    const componentsText = componentsCount === 1 ? 'component' : 'components';
    return `${violationsCount} total ${violationsText} across ${componentsCount} ${componentsText}`;
  };

  const backClick = () => {
    dispatch(waiverActions.resetBulkWaiverSubmitState());
    dispatch(goToWaiverConfigurationPage());
  };

  const onSubmit = () => {
    dispatch(waiverActions.addBulkWaiver());
  };

  const cancelClick = () => {
    dispatch(cancelBulkWaive());
  };

  // Handle successful submission
  useEffect(() => {
    if (submitMaskState === true) {
      // Reset bulk waiver state and redirect
      dispatch(waiverActions.clearBulkWaiveCheckboxes());
      dispatch(waiverActions.resetWaiverConfiguration());
      // Cancel bulk waive redirects correctly to the initial page where bulk waive was clicked
      dispatch(cancelBulkWaive());
      dispatch(
        toastActions.addToast({ type: 'success', message: 'Bulk Waivers will apply when report is re-evaluated.' })
      );
    }
  }, [submitMaskState]);

  const additionalFooterBtns = (
    <>
      <NxButton variant="tertiary" onClick={cancelClick} type="button">
        Cancel
      </NxButton>
      <NxButton variant="secondary" onClick={backClick} type="button">
        Back
      </NxButton>
    </>
  );

  if (selectedViolationsCount === 0) {
    // The user managed to navigate here without going via the BulWaivePage
    dispatch(goToBulkWaivePage());
    return null;
  }

  return (
    <NxPageMain className="iq-bulk-waiver-confirmation-page">
      <BulkWaiveTitle />
      <NxTile>
        <NxForm
          onSubmit={onSubmit}
          showValidationErrors={false}
          submitError={submitError}
          submitMaskState={submitMaskState}
          submitBtnText="Submit"
          additionalFooterBtns={additionalFooterBtns}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Confirmation</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>

          <NxTile.Content>
            <NxFieldset label="Violations being waived">
              <NxReadOnly>
                <NxReadOnly.Data>{formatViolationsSummary(selectedViolationsCount, componentCount)}</NxReadOnly.Data>
              </NxReadOnly>
            </NxFieldset>
            <NxFieldset label="Policy Violations being waived">
              <NxThreatCounter
                criticalCount={policyThreatCounts.criticalCount || null}
                severeCount={policyThreatCounts.severeCount || null}
                moderateCount={policyThreatCounts.moderateCount || null}
                lowCount={policyThreatCounts.lowCount || null}
                noneCount={policyThreatCounts.noneCount || null}
                unspecifiedCount={policyThreatCounts.unspecifiedCount || null}
                layout="column"
              />
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
                <NxInfoAlert id="iq-bulk-waiver-mixed-violations-alert">
                  The selected violations contain unknown/unclaimed components. When &quot;All Versions&quot; is
                  selected, the bulk waiver will only apply to identified components.
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
          </NxTile.Content>
        </NxForm>
      </NxTile>
    </NxPageMain>
  );
}
