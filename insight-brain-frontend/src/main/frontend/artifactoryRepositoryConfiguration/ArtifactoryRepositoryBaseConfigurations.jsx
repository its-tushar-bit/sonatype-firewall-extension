/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxH2,
  NxH3,
  NxInfoAlert,
  NxList,
  NxP,
  NxPageMain,
  NxRadio,
  NxReadOnly,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectAllowChange,
  selectEnabled,
  selectInheritedFromOrgEnabled,
  selectArtifactoryRepositoryBaseConfigurationsSlice,
  selectArtifactoryConnection,
  selectValidationErrors,
  selectFormState,
  selectInheritedFromOrganizationName,
  selectOwnerPublicId,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors';
import {
  actions,
  MUST_UPDATE_ENABLED_ADD_MESSAGE,
  MUST_UPDATE_ENABLED_EDIT_MESSAGE,
  PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';

import ArtifactoryRepositoryDeleteConfigurationModal from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryDeleteConfigurationModal';
import { actions as deleteModalActions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSlice';

import { selectOwnerTypeAndOwnerId } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors';
import { actions as configurationModalActions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';

import { faPen, faPlus, faTrash } from '@fortawesome/pro-solid-svg-icons';

import classnames from 'classnames';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import ArtifactoryRepositoryConfigurationModal from './ArtifactoryRepositoryConfigurationModal';

export default function ArtifactoryRepositoryBaseConfigurations() {
  const dispatch = useDispatch();

  const { loading, loadError, saveError, submitMaskState, submitMaskMessage } = useSelector(
    selectArtifactoryRepositoryBaseConfigurationsSlice
  );

  const { enabled: formEnabled, allowOverride } = useSelector(selectFormState);
  const inheritedFromOrganizationName = useSelector(selectInheritedFromOrganizationName);
  const enabled = useSelector(selectEnabled);
  const inheritedFromOrgEnabled = useSelector(selectInheritedFromOrgEnabled);
  const allowChange = useSelector(selectAllowChange);
  const validationErrors = useSelector(selectValidationErrors);
  const artifactoryConnection = useSelector(selectArtifactoryConnection);
  const ownerTypeAndOwnerId = useSelector(selectOwnerTypeAndOwnerId);
  const ownerType = ownerTypeAndOwnerId?.ownerType;
  const ownerId = ownerTypeAndOwnerId?.ownerId;
  const ownerPublicId = useSelector(selectOwnerPublicId);

  const load = () => dispatch(actions.load());
  const save = () => dispatch(actions.save());

  useEffect(() => {
    load();
  }, []);

  const setEnabled = (value) => dispatch(actions.setEnabled(value));
  const setAllowOverride = (value) => dispatch(actions.setAllowOverride(value));

  const openAddConfigurationModal = () => {
    if (allowChange && enabled && !artifactoryConnection) {
      dispatch(configurationModalActions.loadConfiguration());
    }
  };

  const openEditConfigurationModal = (artifactoryConnectionId) => {
    if (allowChange && enabled) {
      dispatch(configurationModalActions.loadConfiguration(artifactoryConnectionId));
    }
  };

  const openDeleteConfigurationModal = (artifactoryConnectionId) => {
    if (allowChange && enabled) {
      dispatch(deleteModalActions.openModal(artifactoryConnectionId));
    }
  };

  const getBackHref = () => {
    if (ownerType === 'application') {
      return `#/management/view/${ownerType}/${ownerPublicId}`;
    }
    return `#/management/view/${ownerType}/${ownerId}`;
  };

  const getAddOrEditTooltip = (defaultMessage, mustUpdateEnabledMessage) => {
    return allowChange
      ? enabled
        ? defaultMessage
        : mustUpdateEnabledMessage
      : PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE;
  };

  return (
    <NxPageMain id="artifactory-repository-base-configurations-page-container">
      <MenuBarBackButton href={getBackHref()} text="Back" />

      <NxTile>
        <ArtifactoryRepositoryConfigurationModal />
        <ArtifactoryRepositoryDeleteConfigurationModal />

        <NxStatefulForm
          id="artifactory-repository-base-configurations-form"
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={save}
          submitError={saveError}
          validationErrors={validationErrors}
          submitBtnText="Update"
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Artifactory Repository Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>Configure the Artifactory repository you want to use to identify components.</NxP>
            <NxReadOnly>
              <NxReadOnly.Label>Status</NxReadOnly.Label>
              <NxReadOnly.Data>
                {(enabled && inheritedFromOrgEnabled === null) || inheritedFromOrgEnabled ? 'Enabled' : 'Disabled'}
                {inheritedFromOrganizationName ? ` (inherited from ${inheritedFromOrganizationName})` : ''}
              </NxReadOnly.Data>
            </NxReadOnly>
            {!allowChange && <NxInfoAlert>The inherited configuration cannot be overridden.</NxInfoAlert>}
            {ownerType !== 'application' && (
              <NxFieldset label="Allow Override by Child Organizations and Applications" isRequired>
                <NxCheckbox
                  id="artifactory-repository-base-configurations-allow-override"
                  isChecked={allowOverride}
                  onChange={() => setAllowOverride(!allowOverride)}
                  disabled={!allowChange}
                >
                  Allow Override
                </NxCheckbox>
              </NxFieldset>
            )}
            <NxFieldset label="Artifactory Repository Connections" isRequired>
              {ownerId !== 'ROOT_ORGANIZATION_ID' && (
                <NxRadio
                  id="artifactory-repository-base-configurations-inherit-radio"
                  name="artifactoryBaseConfigurationsAreEnabled"
                  value="0"
                  isChecked={formEnabled === null}
                  onChange={() => setEnabled(null)}
                  disabled={!allowChange}
                >
                  Inherit
                </NxRadio>
              )}
              <NxRadio
                id="artifactory-repository-base-configurations-disable-radio"
                name="artifactoryBaseConfigurationsAreEnabled"
                value="1"
                isChecked={formEnabled === false}
                onChange={() => setEnabled(false)}
                disabled={!allowChange}
              >
                Disable
              </NxRadio>
              <NxRadio
                id="artifactory-repository-base-configurations-enable-radio"
                name="artifactoryBaseConfigurationsAreEnabled"
                value="2"
                isChecked={formEnabled === true}
                onChange={() => setEnabled(true)}
                disabled={!allowChange}
              >
                Enable{ownerId !== 'ROOT_ORGANIZATION_ID' ? ' and Override Repository Connections' : ''}
              </NxRadio>
            </NxFieldset>
            {formEnabled && (
              <>
                <div id="artifactory-repository-base-configurations-list-header">
                  <NxH3>LOCAL</NxH3>
                  <NxTooltip title={getAddOrEditTooltip('', MUST_UPDATE_ENABLED_ADD_MESSAGE)}>
                    <NxButton
                      id="artifactory-repository-base-configurations-add-button"
                      type="button"
                      variant="tertiary"
                      onClick={openAddConfigurationModal}
                      className={classnames({ disabled: !allowChange || !enabled || artifactoryConnection })}
                      aria-disabled={!allowChange || !enabled || artifactoryConnection}
                    >
                      <NxFontAwesomeIcon icon={faPlus} />
                      <span>Add a Repository</span>
                    </NxButton>
                  </NxTooltip>
                </div>
                <NxList emptyMessage="No Artifactory repository connection is configured">
                  {artifactoryConnection && (
                    <NxList.Item
                      key={artifactoryConnection.artifactoryConnectionId}
                      id={`artifactory-repository-base-configurations-${artifactoryConnection.artifactoryConnectionId}`}
                    >
                      <NxList.Text>{artifactoryConnection.baseUrl}</NxList.Text>
                      <NxList.Actions>
                        <NxButton
                          type="button"
                          variant="icon-only"
                          onClick={() => openEditConfigurationModal(artifactoryConnection.artifactoryConnectionId)}
                          title={getAddOrEditTooltip('Edit Repository Configuration', MUST_UPDATE_ENABLED_EDIT_MESSAGE)}
                          className={classnames('artifactory-repository-base-configurations-edit-button', {
                            disabled: !allowChange || !enabled,
                          })}
                          aria-disabled={!allowChange || !enabled}
                        >
                          <NxFontAwesomeIcon icon={faPen} />
                        </NxButton>
                        <NxButton
                          type="button"
                          variant="icon-only"
                          onClick={() => openDeleteConfigurationModal(artifactoryConnection.artifactoryConnectionId)}
                          title={getAddOrEditTooltip(
                            'Delete Repository Configuration',
                            MUST_UPDATE_ENABLED_EDIT_MESSAGE
                          )}
                          className={classnames('artifactory-repository-base-configurations-delete-button', {
                            disabled: !allowChange || !enabled,
                          })}
                          aria-disabled={!allowChange || !enabled}
                        >
                          <NxFontAwesomeIcon icon={faTrash} />
                        </NxButton>
                      </NxList.Actions>
                    </NxList.Item>
                  )}
                </NxList>
              </>
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
    </NxPageMain>
  );
}
