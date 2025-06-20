/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxCheckbox,
  NxErrorAlert,
  NxFieldset,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxRadio,
  NxStatefulForm,
  NxTile,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSlice';
import { actions as rootActions } from 'MainRoot/OrgsAndPolicies/rootSlice';
import {
  selectCpeConfiguration,
  selectPublicDataSourcesSlice,
} from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSelectors';
import { selectIsCpeMatchingSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsApplication, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function PublicDataSourcesEditor() {
  const dispatch = useDispatch();
  const { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectPublicDataSourcesSlice) || {};
  const {
    allowOverride,
    enabled,
    enabledInParent,
    inheritedFromOrganizationName,
    inheritedFromOrganizationAllowOverride,
  } = useSelector(selectCpeConfiguration) || {};
  const parentAllowOverride = inheritedFromOrganizationAllowOverride ?? true;
  const isCpeMatchingSupported = useSelector(selectIsCpeMatchingSupported);
  const isApp = useSelector(selectIsApplication);
  const isRootOrg = useSelector(selectIsRootOrganization);
  const ownerId = useSelector(selectSelectedOwnerId);
  const doLoad = () => dispatch(actions.loadCpeConfiguration());
  const handleSubmit = () => {
    dispatch(actions.saveCpeConfiguration());
  };
  const handleChange = (value) => {
    if (value === 'inherit') {
      dispatch(actions.setCpeStatus({ inherited: true, enabled: null }));
    } else if (value === 'enabled') {
      dispatch(actions.setCpeStatus({ inherited: false, enabled: true }));
    } else if (value === 'disabled') {
      dispatch(actions.setCpeStatus({ inherited: false, enabled: false }));
    }
  };

  useEffect(() => {
    dispatch(rootActions.loadSelectedOwner());
  }, [dispatch]);

  useEffect(() => {
    if (ownerId) {
      doLoad();
    }
  }, [dispatch, ownerId]);

  return (
    <>
      <NxPageTitle id="public-data-sources-title">
        <NxH1>Public Data Sources</NxH1>
        <NxPageTitle.Description>
          Add public data to your results. This includes CPE (Common Platform Enumeration) matching data and public
          research vulnerabilities from the NVD (National Vulnerability Database).
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper id="public-data-sources-loader" loading={loading} error={loadError} retryHandler={doLoad}>
        {isCpeMatchingSupported ? (
          <NxTile id="public-data-sources-settings">
            <NxStatefulForm
              id="public-data-sources-form"
              aria-labelledby="public-data-sources-title"
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
                <NxFieldset
                  id="use-public-data-sources"
                  label="Use public data sources"
                  sublabel="Enabling it will show CPE match vulnerabilities in your reports."
                >
                  {!isRootOrg && (
                    <NxRadio
                      id="public-data-sources-inherit"
                      name="publicDataSourcesStatus"
                      value="inherit"
                      onChange={() => handleChange('inherit')}
                      isChecked={!!inheritedFromOrganizationName}
                      disabled={!parentAllowOverride && !isRootOrg}
                    >
                      {`Inherit from parent (${enabledInParent ? 'Enabled' : 'Disabled'})`}
                    </NxRadio>
                  )}
                  <NxRadio
                    id="public-data-sources-enabled"
                    name="publicDataSourcesStatus"
                    value="enabled"
                    onChange={() => handleChange('enabled')}
                    isChecked={!!enabled && !inheritedFromOrganizationName}
                    disabled={!parentAllowOverride && !isRootOrg}
                  >
                    Enabled
                  </NxRadio>
                  <NxRadio
                    id="public-data-sources-disabled"
                    name="publicDataSourcesStatus"
                    value="disabled"
                    onChange={() => handleChange('disabled')}
                    isChecked={!enabled && !inheritedFromOrganizationName}
                    disabled={!parentAllowOverride && !isRootOrg}
                  >
                    Disabled
                  </NxRadio>
                </NxFieldset>
                {!isApp && (
                  <NxFieldset
                    id="public-data-overrides"
                    label="Public Data Overrides"
                    sublabel="This allows the setting above to be overriden."
                  >
                    <NxCheckbox
                      id="allow-public-data-override"
                      isChecked={allowOverride || false}
                      disabled={!parentAllowOverride && !isRootOrg}
                      onChange={() => dispatch(actions.toggleCpeOverride())}
                    >
                      Allow users to enable public data sources per organization or application
                    </NxCheckbox>
                  </NxFieldset>
                )}
              </NxTile.Content>
            </NxStatefulForm>
          </NxTile>
        ) : (
          <NxErrorAlert id="public-data-license-error" aria-live="assertive">
            Public Data Sources are not supported by your license.
          </NxErrorAlert>
        )}
      </NxLoadWrapper>
    </>
  );
}
