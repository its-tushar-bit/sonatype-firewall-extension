/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';
import {
  selectGrandfatheringStatusMessageFromServer,
  selectPolicyViolationGrandfatheringConfig,
  selectPolicyViolationGrandfatheringSlice,
} from '../policyViolationGrandfatheringSelectors';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsApplication, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  NxLoadWrapper,
  NxPageTitle,
  NxH1,
  NxTile,
  NxErrorAlert,
  NxStatefulForm,
  NxFieldset,
  NxRadio,
  NxCheckbox,
  NxInfoAlert,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function PolicyViolationsGrandfatheringEditor() {
  const dispatch = useDispatch();

  const { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(
    selectPolicyViolationGrandfatheringSlice
  );
  const { allowChange, allowOverride, enabled, inheritedFromOrganizationName } = useSelector(
    selectPolicyViolationGrandfatheringConfig
  );

  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const isApp = useSelector(selectIsApplication);
  const isRootOrg = useSelector(selectIsRootOrganization);

  const allowOverrideLabel = isRootOrg
    ? 'Allow Override by Child Organizations and Applications'
    : 'Allow Override by Child Applications';

  const grandfatheringStatusMessage = useSelector(selectGrandfatheringStatusMessageFromServer);

  const doLoad = () => dispatch(actions.loadPolicyViolationGrandfathering());

  const handleSubmit = () => {
    dispatch(actions.savePolicyViolationGrandfathering());
  };

  const handleChange = (val) => {
    dispatch(actions.setGrandfatheringStatus(val));
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <NxPageTitle>
        <NxH1>Policy Violation Grandfathering</NxH1>
        <NxPageTitle.Description>
          Policy violation grandfathering can be enabled on organizations and applications. These settings can be
          inherited by child organizations and applications.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        {isGrandfatheringSupported ? (
          <NxTile>
            <NxStatefulForm
              submitBtnText="Update"
              submitMaskState={submitMaskState}
              submitMaskMessage="Saving…"
              validationErrors={isDirty ? undefined : MSG_NO_CHANGES_TO_SAVE}
              onSubmit={handleSubmit}
              doLoad={doLoad}
              loadError={loadError}
              submitError={submitError}
            >
              <NxTile.Content>
                <NxReadOnly>
                  <NxReadOnly.Label>Status</NxReadOnly.Label>
                  <NxReadOnly.Data>{grandfatheringStatusMessage}</NxReadOnly.Data>
                </NxReadOnly>
                {!allowChange && (
                  <NxInfoAlert id="violation-grandfathering-disabled-message">
                    The parent selection cannot be overridden.
                  </NxInfoAlert>
                )}
                <NxFieldset label="Enable Policy Violation Grandfathering" isRequired>
                  {!isRootOrg && (
                    <NxRadio
                      name="grandfatheringStatus"
                      value="inherit"
                      onChange={handleChange}
                      isChecked={!!inheritedFromOrganizationName}
                      disabled={!allowChange}
                    >
                      Inherit from parent organization
                    </NxRadio>
                  )}
                  <NxRadio
                    name="grandfatheringStatus"
                    value="enabled"
                    onChange={handleChange}
                    isChecked={inheritedFromOrganizationName ? false : !!enabled}
                    disabled={!allowChange}
                  >
                    Enabled
                  </NxRadio>
                  <NxRadio
                    name="grandfatheringStatus"
                    value="disabled"
                    onChange={handleChange}
                    isChecked={inheritedFromOrganizationName ? false : !enabled}
                    disabled={!allowChange}
                  >
                    Disabled
                  </NxRadio>
                </NxFieldset>
                {!isApp && (
                  <NxFieldset label={allowOverrideLabel} isRequired>
                    <NxCheckbox
                      isChecked={allowOverride || false}
                      disabled={!allowChange}
                      onChange={() => dispatch(actions.toggleOverride())}
                    >
                      Allow Override
                    </NxCheckbox>
                  </NxFieldset>
                )}
              </NxTile.Content>
            </NxStatefulForm>
          </NxTile>
        ) : (
          <NxErrorAlert>Policy Violation Grandfathering is not supported by your license.</NxErrorAlert>
        )}
      </NxLoadWrapper>
    </>
  );
}
