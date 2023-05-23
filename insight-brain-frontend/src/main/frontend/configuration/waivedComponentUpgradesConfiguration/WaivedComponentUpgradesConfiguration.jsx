/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxStatefulForm,
  NxToggle,
  NxButton,
  NxP,
  NxPageTitle,
  NxH1,
  NxTile,
  NxH2,
  NxFormGroup,
  NxTextLink,
  NxPageMain,
} from '@sonatype/react-shared-components';
import { actions } from './waivedComponentUpgradesConfigurationSlice';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectFormState,
  selectIsDirty,
  selectLoadError,
  selectLoading,
  selectSubmitMaskState,
  selectUpdateError,
} from './waivedComponentUpgradesConfigurationSelectors';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const WaivedComponentUpgradesConfiguration = () => {
  const dispatch = useDispatch();

  const update = () => dispatch(actions.update());
  const load = () => dispatch(actions.load());
  const resetForm = () => dispatch(actions.resetForm());
  const toggleIsEnabled = () => dispatch(actions.toggleIsEnabled());

  const loadError = useSelector(selectLoadError);
  const loading = useSelector(selectLoading);
  const isDirty = useSelector(selectIsDirty);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const updateError = useSelector(selectUpdateError);
  const formState = useSelector(selectFormState);

  useEffect(() => {
    load();
  }, []);

  return (
    <NxPageMain id="waived-component-upgrades-configuration">
      <NxPageTitle>
        <NxH1>Waived Component Upgrades</NxH1>
        <NxPageTitle.Description>
          <NxP>
            Monitor for non-violating versions of waived components and indicate upgrade availability on the Waiver
            Dashboard. For more information, see{' '}
            <NxTextLink
              external
              href="https://links.sonatype.com/products/nxiq/doc/waived-component-upgrades-configuration"
            >
              Waived Component Upgrades
            </NxTextLink>
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile id="waived-component-upgrades-configuration">
        <NxStatefulForm
          onSubmit={update}
          loadError={loadError}
          loading={loading}
          doLoad={load}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Update"
          validationErrors={isDirty ? null : MSG_NO_CHANGES_TO_UPDATE}
          additionalFooterBtns={
            <NxButton type="button" id="waived-component-upgrade-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Component Upgrade Availability</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxFormGroup label="Dashboard indicator" sublabel="Indicate when non-violating versions are available">
              <NxToggle
                id="waived-component-upgrade-toggle"
                className="nx-toggle--no-gap"
                onChange={toggleIsEnabled}
                isChecked={formState.waivedComponentUpgradeMonitoringEnabled}
              >
                {formState.waivedComponentUpgradeMonitoringEnabled ? 'Enabled' : 'Disabled'}
              </NxToggle>
            </NxFormGroup>
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
    </NxPageMain>
  );
};
export default WaivedComponentUpgradesConfiguration;
