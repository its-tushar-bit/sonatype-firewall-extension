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
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectAllowChange,
  selectEnabled,
  selectInheritedFromOrgEnabled,
  selectInnerSourceRepositoryBaseConfigurationsSlice,
  selectIsDirty,
  selectRepositoryConnections,
  selectValidationErrors,
  selectFormState,
  selectInheritedFromOrganizationName,
  selectOwnerPublicId,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
import {
  actions,
  MUST_UPDATE_ENABLED_ADD_MESSAGE,
  MUST_UPDATE_ENABLED_EDIT_MESSAGE,
  PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';

import InnerSourceRepositoryDeleteConfigurationModal from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryDeleteConfigurationModal';
import { actions as deleteModalActions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSlice';

import { selectOwnerTypeAndOwnerId } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as configurationModalActions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryConfigurationModalSlice';

import { faPen, faPlus, faTrash } from '@fortawesome/pro-solid-svg-icons';

import classnames from 'classnames';

import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import InnerSourceRepositoryConfigurationModal from './InnerSourceRepositoryConfigurationModal';

export default function InnerSourceRepositoryBaseConfigurations() {
  const dispatch = useDispatch();

  const { loading, loadError, saveError, submitMaskState, submitMaskMessage } = useSelector(
    selectInnerSourceRepositoryBaseConfigurationsSlice
  );

  const { enabled: formEnabled, allowOverride } = useSelector(selectFormState);
  const inheritedFromOrganizationName = useSelector(selectInheritedFromOrganizationName);
  const enabled = useSelector(selectEnabled);
  const inheritedFromOrgEnabled = useSelector(selectInheritedFromOrgEnabled);
  const allowChange = useSelector(selectAllowChange);
  const validationErrors = useSelector(selectValidationErrors);
  const repositoryConnections = useSelector(selectRepositoryConnections);
  const ownerTypeAndOwnerId = useSelector(selectOwnerTypeAndOwnerId);
  const ownerType = ownerTypeAndOwnerId?.ownerType;
  const ownerId = ownerTypeAndOwnerId?.ownerId;
  const ownerPublicId = useSelector(selectOwnerPublicId);
  const isDirty = useSelector(selectIsDirty);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);

  const load = () => dispatch(actions.load());
  const save = () => dispatch(actions.save());
  const cancel = () => dispatch(actions.cancel());

  useEffect(() => {
    load();
  }, []);

  const setEnabled = (value) => dispatch(actions.setEnabled(value));
  const setAllowOverride = (value) => dispatch(actions.setAllowOverride(value));

  const openAddConfigurationModal = () => {
    if (allowChange && enabled) {
      dispatch(configurationModalActions.loadConfiguration());
    }
  };

  const openEditConfigurationModal = (repositoryConnectionId) => {
    if (allowChange && enabled) {
      dispatch(configurationModalActions.loadConfiguration(repositoryConnectionId));
    }
  };

  const openDeleteConfigurationModal = (repositoryConnectionId) => {
    if (allowChange && enabled) {
      dispatch(deleteModalActions.openModal(repositoryConnectionId));
    }
  };

  const getBackHref = () => {
    const firewallPrefix = isStandaloneFirewall ? 'malware-defense/' : '';
    if (ownerType === 'application') {
      return `#/${firewallPrefix}management/view/${ownerType}/${ownerPublicId}`;
    }
    return `#/${firewallPrefix}management/view/${ownerType}/${ownerId}`;
  };

  const getAddOrEditTooltip = (defaultMessage, mustUpdateEnabledMessage) => {
    return allowChange
      ? enabled
        ? defaultMessage
        : mustUpdateEnabledMessage
      : PARENT_ORGANIZATIONS_MUST_ALLOW_OVERRIDE;
  };

  return (
    <NxPageMain id="innersource-repository-base-configurations-page-container">
      <MenuBarBackButton href={getBackHref()} text="Back" />

      <NxTile>
        <InnerSourceRepositoryConfigurationModal />
        <InnerSourceRepositoryDeleteConfigurationModal />

        <NxStatefulForm
          id="innersource-repository-base-configurations-form"
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={save}
          submitError={saveError}
          validationErrors={validationErrors}
          submitBtnText="Update"
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          additionalFooterBtns={
            <NxButton
              id="innersource-repository-base-configurations-cancel-button"
              type="button"
              onClick={cancel}
              variant="tertiary"
              disabled={!isDirty}
            >
              Cancel
            </NxButton>
          }
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>InnerSource Repository Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>Configure the repositories you want to use to identify InnerSource components.</NxP>
            <dl className="nx-read-only">
              <dt className="nx-read-only__label">Status</dt>
              <dd className="nx-read-only__data">
                {(enabled && inheritedFromOrgEnabled === null) || inheritedFromOrgEnabled ? 'Enabled' : 'Disabled'}
                {inheritedFromOrganizationName ? ` (inherited from ${inheritedFromOrganizationName})` : ''}
              </dd>
            </dl>
            {!allowChange && <NxInfoAlert>The inherited configuration cannot be overridden.</NxInfoAlert>}
            {ownerType !== 'application' && (
              <NxFieldset label="Allow Override by Child Organizations and Applications" isRequired>
                <NxCheckbox
                  id="innersource-repository-base-configurations-allow-override"
                  isChecked={allowOverride}
                  onChange={() => setAllowOverride(!allowOverride)}
                  disabled={!allowChange}
                >
                  Allow Override
                </NxCheckbox>
              </NxFieldset>
            )}
            <NxFieldset label="InnerSource Repository Connections" isRequired>
              {ownerId !== 'ROOT_ORGANIZATION_ID' && (
                <NxRadio
                  id="innersource-repository-base-configurations-inherit-radio"
                  name="repositoryBaseConfigurationsAreEnabled"
                  value="0"
                  isChecked={formEnabled === null}
                  onChange={() => setEnabled(null)}
                  disabled={!allowChange}
                >
                  Inherit
                </NxRadio>
              )}
              <NxRadio
                id="innersource-repository-base-configurations-disable-radio"
                name="repositoryBaseConfigurationsAreEnabled"
                value="1"
                isChecked={formEnabled === false}
                onChange={() => setEnabled(false)}
                disabled={!allowChange}
              >
                Disable
              </NxRadio>
              <NxRadio
                id="innersource-repository-base-configurations-enable-radio"
                name="repositoryBaseConfigurationsAreEnabled"
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
                <div id="innersource-repository-base-configurations-list-header">
                  <NxH3>LOCAL</NxH3>
                  <NxTooltip title={getAddOrEditTooltip('', MUST_UPDATE_ENABLED_ADD_MESSAGE)}>
                    <NxButton
                      id="innersource-repository-base-configurations-add-button"
                      type="button"
                      variant="tertiary"
                      onClick={openAddConfigurationModal}
                      className={classnames({ disabled: !allowChange || !enabled })}
                      aria-disabled={!allowChange || !enabled}
                    >
                      <NxFontAwesomeIcon icon={faPlus} />
                      <span>Add a Repository</span>
                    </NxButton>
                  </NxTooltip>
                </div>
                <NxList emptyMessage="No InnerSource repository connections are configured">
                  {repositoryConnections &&
                    repositoryConnections.map((repositoryConnection) => {
                      return (
                        <NxList.Item
                          key={repositoryConnection.repositoryConnectionId}
                          id={`innersource-repository-base-configurations-${repositoryConnection.repositoryConnectionId}`}
                        >
                          <NxList.Text>
                            {repositoryConnection.baseUrl}
                            <br />
                            {repositoryConnection.format}
                          </NxList.Text>
                          <NxList.Actions>
                            <NxButton
                              type="button"
                              variant="icon-only"
                              onClick={() => openEditConfigurationModal(repositoryConnection.repositoryConnectionId)}
                              title={getAddOrEditTooltip(
                                'Edit Repository Configuration',
                                MUST_UPDATE_ENABLED_EDIT_MESSAGE
                              )}
                              className={classnames('innersource-repository-base-configurations-edit-button', {
                                disabled: !allowChange || !enabled,
                              })}
                              aria-disabled={!allowChange || !enabled}
                            >
                              <NxFontAwesomeIcon icon={faPen} />
                            </NxButton>
                            <NxButton
                              type="button"
                              variant="icon-only"
                              onClick={() => openDeleteConfigurationModal(repositoryConnection.repositoryConnectionId)}
                              title={getAddOrEditTooltip(
                                'Delete Repository Configuration',
                                MUST_UPDATE_ENABLED_EDIT_MESSAGE
                              )}
                              className={classnames('innersource-repository-base-configurations-delete-button', {
                                disabled: !allowChange || !enabled,
                              })}
                              aria-disabled={!allowChange || !enabled}
                            >
                              <NxFontAwesomeIcon icon={faTrash} />
                            </NxButton>
                          </NxList.Actions>
                        </NxList.Item>
                      );
                    })}
                </NxList>
              </>
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
    </NxPageMain>
  );
}
