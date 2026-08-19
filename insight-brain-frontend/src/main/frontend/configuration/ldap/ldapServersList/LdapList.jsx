/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxPageMain,
  NxTile,
  NxPageTitle,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxStatefulForm,
} from '@sonatype/react-shared-components';
import { faPlus, faAngleRight, faPen } from '@fortawesome/pro-solid-svg-icons';
import { faArrowDown, faArrowUp } from '@fortawesome/pro-regular-svg-icons';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function LdapList({
  loadServers,
  moveServerUpInTheList,
  enterReorderMode,
  exitReorderMode,
  saveOrder,
  loading,
  servers,
  reorderedServers,
  stateGo,
  loadError,
  saveServerOrderSuccess,
  saveServerOrderError,
  isDirty,
}) {
  useEffect(() => {
    loadServers();
  }, []);

  const isReordering = reorderedServers != null;
  const renderReorderedServers = () =>
    reorderedServers.map(({ name, id }, index) => (
      <li className="nx-list__item" key={id}>
        <Fragment>
          <span className="nx-list__text">{name}</span>
          <div className="nx-list__actions">
            <NxButton
              type="button"
              variant="icon-only"
              disabled={index === 0}
              onClick={() => moveServerUpInTheList(index)}
            >
              <NxFontAwesomeIcon icon={faArrowUp} />
            </NxButton>
            <NxButton
              type="button"
              variant="icon-only"
              disabled={index === servers.length - 1}
              onClick={() => moveServerUpInTheList(index + 1)}
            >
              <NxFontAwesomeIcon icon={faArrowDown} />
            </NxButton>
          </div>
        </Fragment>
      </li>
    ));

  const renderServers = () =>
    servers.map(({ name, id }) => (
      <li
        onClick={() => stateGo('edit-ldap-connection', { ldapId: id })}
        className="nx-list__item nx-list__item--clickable"
        key={id}
      >
        <button className="nx-list__btn">
          <span className="nx-list__text">{name}</span>
          <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
        </button>
      </li>
    ));

  const serverItems = isReordering ? renderReorderedServers() : renderServers();

  const emptyList = <li className="nx-list__item nx-list__item--empty">No LDAP servers are defined</li>;

  const tileContent = (
    <Fragment>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <h2 className="nx-h2">Configure LDAP</h2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderSubtitle>LDAP Servers will be queried in the order listed below</NxTile.HeaderSubtitle>
        <NxTile.HeaderActions>
          <NxButton
            id="reorder-ldap-list-btn"
            type="button"
            variant="tertiary"
            onClick={enterReorderMode}
            disabled={isReordering || servers.length < 2}
          >
            <NxFontAwesomeIcon icon={faPen}></NxFontAwesomeIcon>
            <span>Reorder List</span>
          </NxButton>
          <NxButton
            id="add-ldap-server-btn"
            type="button"
            variant="primary"
            onClick={() => stateGo('create-ldap')}
            disabled={isReordering}
          >
            <NxFontAwesomeIcon icon={faPlus}></NxFontAwesomeIcon>
            <span>Add a Server</span>
          </NxButton>
        </NxTile.HeaderActions>
      </NxTile.Header>
      <NxTile.Content>
        <ul id="ldap-server-list" className="nx-list">
          {servers.length ? serverItems : emptyList}
        </ul>
      </NxTile.Content>
    </Fragment>
  );

  return (
    <NxPageMain>
      <NxPageTitle>
        <h1 className="nx-h1">LDAP</h1>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} retryHandler={loadServers} error={loadError}>
        <NxTile>
          {isReordering ? (
            <NxStatefulForm
              id="reorder-ldap-servers-form"
              onSubmit={saveOrder}
              onCancel={exitReorderMode}
              submitMaskState={saveServerOrderSuccess}
              submitError={saveServerOrderError}
              submitBtnText="Save Order"
              validationErrors={isDirty ? undefined : MSG_NO_CHANGES_TO_SAVE}
            >
              {tileContent}
            </NxStatefulForm>
          ) : (
            tileContent
          )}
        </NxTile>
      </NxLoadWrapper>
    </NxPageMain>
  );
}

LdapList.propTypes = {
  loadServers: PropTypes.func.isRequired,
  moveServerUpInTheList: PropTypes.func.isRequired,
  enterReorderMode: PropTypes.func.isRequired,
  exitReorderMode: PropTypes.func.isRequired,
  saveOrder: PropTypes.func.isRequired,
  servers: PropTypes.array.isRequired,
  reorderedServers: PropTypes.array,
  loading: PropTypes.bool.isRequired,
  stateGo: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  saveServerOrderSuccess: PropTypes.bool,
  saveServerOrderError: PropTypes.string,
  isDirty: PropTypes.bool.isRequired,
};
