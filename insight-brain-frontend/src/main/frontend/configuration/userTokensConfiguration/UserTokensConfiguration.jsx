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
  NxPageMain,
  NxTextInput,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { actions } from './userTokensConfigurationSlice';
import { useDispatch, useSelector } from 'react-redux';
import { showUserTokenModal } from 'MainRoot/mainHeader/MenuBar/UserMenu/UserToken/userTokenActions';
import UserTokenModalContainer from 'MainRoot/mainHeader/MenuBar/UserMenu/UserToken/UserTokenModalContainer';
import {
  selectFormState,
  selectIsDirty,
  selectLoadError,
  selectLoading,
  selectSubmitMaskState,
  selectUpdateError,
} from './userTokensConfigurationSelectors';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const UserTokensConfiguration = () => {
  const dispatch = useDispatch();

  const update = () => dispatch(actions.update());
  const load = () => dispatch(actions.load());
  const resetForm = () => dispatch(actions.resetForm());
  const toggleExpirationEnabled = () => dispatch(actions.toggleExpirationEnabled());
  const setExpirationDays = (value) => dispatch(actions.setExpirationDays(value));

  const loadError = useSelector(selectLoadError);
  const loading = useSelector(selectLoading);
  const isDirty = useSelector(selectIsDirty);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const updateError = useSelector(selectUpdateError);
  const formState = useSelector(selectFormState);
  const isUserTokenModalVisible = useSelector((state) => state.userToken?.isUserTokenModalVisible);

  const onManageUserToken = () => dispatch(showUserTokenModal());

  useEffect(() => {
    load();
  }, []);

  const getValidationErrors = () => {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    }
    if (formState.expirationEnabled && formState.expirationDays.validationErrors) {
      return formState.expirationDays.validationErrors;
    }
    return null;
  };

  return (
    <NxPageMain id="user-tokens-configuration">
      <NxPageTitle>
        <NxH1>User Tokens</NxH1>
        <NxPageTitle.Description>
          <NxP>Manage user token configuration</NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile id="user-tokens-configuration-tile">
        <NxStatefulForm
          onSubmit={update}
          loadError={loadError}
          loading={loading}
          doLoad={load}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitError={updateError}
          submitBtnText="Update"
          validationErrors={getValidationErrors()}
          additionalFooterBtns={
            <NxButton type="button" id="user-tokens-cancel" onClick={resetForm} disabled={!isDirty}>
              Cancel
            </NxButton>
          }
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Token Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              The user tokens feature allows users to authenticate securely without typical user credentials such as
              those used by LDAP or Crowd. User tokens generated for this server are only valid for use on this server.
              Once enabled, users can access their user token from{' '}
              <NxTextLink onClick={onManageUserToken}>Manage User Token</NxTextLink>.
            </NxP>
            <NxFormGroup label="User Tokens">
              <NxToggle
                id="user-tokens-enabled-toggle"
                className="nx-toggle--no-gap"
                isChecked={formState.userTokensEnabled}
                disabled={true}
              >
                Enable User Tokens
              </NxToggle>
            </NxFormGroup>
            <NxFormGroup label="User Token Expiration" sublabel="Applies only to new user tokens">
              <NxToggle
                id="user-token-expiration-toggle"
                className="nx-toggle--no-gap"
                onChange={toggleExpirationEnabled}
                isChecked={formState.expirationEnabled}
              >
                Enable User Token Expiration
              </NxToggle>
            </NxFormGroup>
            <NxFormGroup
              label="User Token Expiry"
              sublabel="Specify the number of days for which a user token is valid. This defaults to 30 days when token expiration is enabled. (E.g. 1-365)"
            >
              <NxTextInput
                {...formState.expirationDays}
                id="user-token-expiry-days"
                onChange={setExpirationDays}
                disabled={!formState.expirationEnabled}
                validatable={true}
              />
            </NxFormGroup>
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
      {isUserTokenModalVisible && <UserTokenModalContainer />}
    </NxPageMain>
  );
};

export default UserTokensConfiguration;
