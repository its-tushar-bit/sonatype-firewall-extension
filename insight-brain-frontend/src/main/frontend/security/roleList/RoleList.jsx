/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { groupBy } from 'ramda';
import { NxButton, NxFontAwesomeIcon, NxInfoAlert, NxTextLink } from '@sonatype/react-shared-components';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import LoadWrapper from '../../react/LoadWrapper';
import RoleListItem from './RoleListItem';

const byBuiltIn = groupBy((role) => (role.builtIn ? 'builtInRoles' : 'customRoles'));

export default function RoleList(props) {
  const { load, stateGo } = props;
  const { readOnly, roles, loading, loadError } = props;

  useEffect(() => {
    load();
  }, []);

  const createRole = () => {
    stateGo('addRole');
  };

  const { builtInRoles = [], customRoles = [] } = byBuiltIn(roles);

  return (
    <main id="role-management" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Roles</h1>
      </div>
      <NxInfoAlert>
        Looking for how to assign a user to a role? Check the{' '}
        <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/user-management/role-management" external>
          Docs
        </NxTextLink>
        .
      </NxInfoAlert>
      <LoadWrapper loading={loading} error={loadError} retryHandler={load}>
        <section className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Roles</h2>
            </div>
            <div className="nx-tile__actions">
              <NxButton variant="tertiary" id="create-role" onClick={createRole} disabled={readOnly}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Create Role</span>
              </NxButton>
            </div>
          </header>
          <div className="nx-tile-content">
            <div className="nx-tile-header__subtitle">Built-In</div>
            <ul className="nx-list nx-list--clickable" id="builtin-roles">
              {builtInRoles.map((role) => (
                <RoleListItem role={role} key={role.id} />
              ))}
            </ul>
            <div className="nx-tile-header__subtitle">Custom</div>
            <ul className={`nx-list nx-list${customRoles.length ? '--clickable' : ''}`} id="custom-roles">
              {!customRoles.length && (
                <li className="nx-list__item nx-list__item--empty">
                  <span className="nx-list__text">
                    No custom roles defined. Click &quot;Create Role&quot; in the upper right to add one.
                  </span>
                </li>
              )}
              {customRoles.map((role) => (
                <RoleListItem role={role} key={role.id} />
              ))}
            </ul>
          </div>
        </section>
      </LoadWrapper>
    </main>
  );
}

RoleList.propTypes = {
  stateGo: PropTypes.func.isRequired,
  load: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  readOnly: PropTypes.bool,
  loading: PropTypes.bool,
  roles: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
      builtIn: PropTypes.bool.isRequired,
      permissionCategories: PropTypes.array,
    })
  ),
};
