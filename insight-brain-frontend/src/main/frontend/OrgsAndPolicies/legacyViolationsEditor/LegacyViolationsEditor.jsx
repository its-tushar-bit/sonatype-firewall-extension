/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';
import {
  selectLegacyViolationConfig,
  selectLegacyViolationSlice,
  selectParentLegacyViolationStatus,
} from '../legacyViolationSelectors';
import { selectIsLegacyViolationSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsApplication, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  NxLoadWrapper,
  NxPageTitle,
  NxH1,
  NxTile,
  NxErrorAlert,
  NxStatefulForm,
  NxForm,
  NxFieldset,
  NxRadio,
  NxCheckbox,
  NxInfoAlert,
} from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function LegacyViolationsEditor() {
  const dispatch = useDispatch();

  const { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectLegacyViolationSlice);
  const { allowChange, allowOverride, enabled, inheritedFromOrganizationName } = useSelector(
    selectLegacyViolationConfig
  );

  const areLegacyViolationsSupported = useSelector(selectIsLegacyViolationSupported);
  const isApp = useSelector(selectIsApplication);
  const isRootOrg = useSelector(selectIsRootOrganization);
  const parentStatus = useSelector(selectParentLegacyViolationStatus);

  const doLoad = () => dispatch(actions.loadLegacyViolation());

  const handleSubmit = () => {
    dispatch(actions.saveLegacyViolation());
  };

  const handleChange = (val) => {
    dispatch(actions.setLegacyViolationStatus(val));
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <NxPageTitle>
        <NxH1>Legacy Violations</NxH1>
        <NxPageTitle.Description>
          Applications can often accumulate a backlog of existing violations that will trigger enforcement when
          onboarded. Policies can acknowledge these as Legacy Violations, and report without taking action.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        {areLegacyViolationsSupported ? (
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
                <NxForm.RequiredFieldNotice />
                {!allowChange && (
                  <NxInfoAlert id="legacy-violations-disabled-message">
                    The parent selection cannot be overridden.
                  </NxInfoAlert>
                )}
                <NxFieldset
                  label="Legacy Violation status:"
                  sublabel="Individual policies must also be configured to grant legacy status"
                  isRequired
                >
                  {!isRootOrg && (
                    <NxRadio
                      name="legacyViolationStatus"
                      value="inherit"
                      onChange={handleChange}
                      isChecked={!!inheritedFromOrganizationName}
                      disabled={!allowChange}
                    >
                      {`Inherit from parent (${parentStatus})`}
                    </NxRadio>
                  )}
                  <NxRadio
                    name="legacyViolationStatus"
                    value="enabled"
                    onChange={handleChange}
                    isChecked={inheritedFromOrganizationName ? false : !!enabled}
                    disabled={!allowChange}
                  >
                    Enabled
                  </NxRadio>
                  <NxRadio
                    name="legacyViolationStatus"
                    value="disabled"
                    onChange={handleChange}
                    isChecked={inheritedFromOrganizationName ? false : !enabled}
                    disabled={!allowChange}
                  >
                    Disabled
                  </NxRadio>
                </NxFieldset>
                {!isApp && (
                  <NxFieldset label="Inheritance Overrides" isRequired>
                    <NxCheckbox
                      isChecked={allowOverride || false}
                      disabled={!allowChange}
                      onChange={() => dispatch(actions.toggleOverride())}
                    >
                      Allow configuration to be overridden at organization or application level
                    </NxCheckbox>
                  </NxFieldset>
                )}
              </NxTile.Content>
            </NxStatefulForm>
          </NxTile>
        ) : (
          <NxErrorAlert>Legacy Violations are not supported by your license.</NxErrorAlert>
        )}
      </NxLoadWrapper>
    </>
  );
}
