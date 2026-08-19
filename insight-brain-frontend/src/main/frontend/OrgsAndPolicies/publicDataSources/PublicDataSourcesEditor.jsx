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
  NxInfoAlert,
  NxLoadWrapper,
  NxPageTitle,
  NxRadio,
  NxStatefulForm,
  NxTextLink,
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
import { selectIsCpeMatchingSupported, selectLoadingFeatures } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsApplication, selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectOwnerProperties, selectSelectedOwnerTypeAndId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import classNames from 'classnames';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

export default function PublicDataSourcesEditor() {
  const dispatch = useDispatch();
  const isSbomManager = useSelector(selectIsSbomManager);
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
  const { ownerId: ownerPublicId } = useSelector(selectOwnerProperties);
  const { ownerId: ownerInternalId } = useSelector(selectSelectedOwnerTypeAndId);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isLoadingFeatures = useSelector(selectLoadingFeatures);
  const disabled = isSbomManager || (!parentAllowOverride && !isRootOrg);

  const uiRouterState = useRouterState();
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

  const href = uiRouterState.href(
    `management.edit.${isApp ? 'application' : 'organization'}.public-data-sources-editor`,
    isApp ? { applicationPublicId: ownerPublicId } : { organizationId: ownerPublicId }
  );

  const doLoad = async () => {
    await dispatch(actions.loadCpeConfiguration());
  };

  useEffect(() => {
    dispatch(rootActions.loadSelectedOwner());
  }, [dispatch]);

  useEffect(() => {
    if (ownerInternalId) {
      doLoad();
    }
  }, [ownerInternalId, isSbomManagerOnlyLicense]);

  return (
    <>
      <NxPageTitle id="public-data-sources-title">
        <NxH1>Public Data Sources</NxH1>
        <NxPageTitle.Description>
          Add public data to your results. This includes CPE (Common Platform Enumeration) matching data and public
          research vulnerabilities from the NVD (National Vulnerability Database).
        </NxPageTitle.Description>
      </NxPageTitle>
      {isCpeMatchingSupported && isSbomManager && !isSbomManagerOnlyLicense && (
        <NxInfoAlert>
          Public Data Sources are configured within Lifecycle.{' '}
          <NxTextLink href={href} target="_blank" rel="noopener noreferrer" noReferrer newTab>
            Click here to update configuration
          </NxTextLink>
          .
        </NxInfoAlert>
      )}
      <NxLoadWrapper id="public-data-sources-loader" loading={loading} error={loadError} retryHandler={doLoad}>
        {isCpeMatchingSupported && !isSbomManagerOnlyLicense ? (
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
              submitBtnClasses={classNames({
                hidden: isSbomManager,
              })}
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
                      disabled={disabled}
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
                    disabled={disabled}
                  >
                    Enabled
                  </NxRadio>
                  <NxRadio
                    id="public-data-sources-disabled"
                    name="publicDataSourcesStatus"
                    value="disabled"
                    onChange={() => handleChange('disabled')}
                    isChecked={!enabled && !inheritedFromOrganizationName}
                    disabled={disabled}
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
                      disabled={disabled}
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
          !isLoadingFeatures && (
            <NxErrorAlert id="public-data-license-error" aria-live="assertive">
              Public Data Sources are not supported by your license.
            </NxErrorAlert>
          )
        )}
      </NxLoadWrapper>
    </>
  );
}
