/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { faPlus, faUser, faUsers } from '@fortawesome/pro-solid-svg-icons';
import {
  NxFontAwesomeIcon,
  NxTile,
  NxList,
  NxDescriptionList,
  NxH2,
  NxH3,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import {
  selectIsRepositoriesRelated,
  selectRouterCurrentParams,
  selectRouterState,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import classnames from 'classnames';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { reformatRouteStateParams } from './accessTileUtil';
import {
  selectLoading,
  selectExtendedMembersByRole,
  selectRolesWithoutLocalMembersExist,
} from 'MainRoot/OrgsAndPolicies/access/accessSelectors';

import { actions as accessActions } from 'MainRoot/OrgsAndPolicies/access/accessSlice';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { isEmpty } from 'ramda';

export default function AccessTile() {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const routerState = useSelector(selectRouterState);
  const loading = useSelector(selectLoading);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const extMembersRoles = useSelector(selectExtendedMembersByRole);
  const rolesWithoutLocalMembersExist = useSelector(selectRolesWithoutLocalMembersExist);
  const ownerName = useSelector(selectSelectedOwnerName);
  const localRoles = extMembersRoles?.filter((role) => role.isInherited !== true)[0]?.roles || [];

  const inheritedRoles =
    extMembersRoles
      ?.filter((role) => role.isInherited === true)
      .map((owner) => ({ roles: [...owner.roles], ownerName: owner.ownerName, ownerId: owner.ownerId }))
      .flat() || [];

  useEffect(() => {
    dispatch(accessActions.loadRoles());
  }, []);

  const getAddRoleButtonUrl = () => {
    const routerInfoRewritten = reformatRouteStateParams(routerState, routerParams);
    return uiRouterState.href(routerInfoRewritten.to, { ...routerInfoRewritten.params });
  };

  const mapMembersData = (m, idx) => (
    <span key={idx} className="iq-access-tile-member-container">
      <NxFontAwesomeIcon icon={m.type === 'GROUP' ? faUsers : faUser} />
      <span className="iq-access-member-text">{m.displayName}</span>
    </span>
  );

  const editRoleUrl = (roleId) => {
    const routerInfoRewritten = reformatRouteStateParams(routerState, routerParams);
    return uiRouterState.href(routerInfoRewritten.to.replace('add-access', 'edit-access'), {
      ...routerInfoRewritten.params,
      roleId,
    });
  };

  const mapLocalAccessDataRow = (accessDataRow) => (
    <NxDescriptionList.LinkItem
      key={accessDataRow.roleId}
      term={accessDataRow.roleName}
      description={accessDataRow?.members?.map(mapMembersData)}
      href={editRoleUrl(accessDataRow.roleId)}
    />
  );

  const mapInheritedAccessRoles = (accessDataRow) => (
    <NxDescriptionList.Item key={accessDataRow.roleId}>
      <NxDescriptionList.Term>
        <NxList.Text>{accessDataRow.roleName}</NxList.Text>
      </NxDescriptionList.Term>
      <NxDescriptionList.Description>{accessDataRow?.members?.map(mapMembersData)}</NxDescriptionList.Description>
    </NxDescriptionList.Item>
  );

  const mapInheritedAccessDataRow = (inheritedOwner) => {
    return (
      inheritedOwner.roles &&
      !isEmpty(inheritedOwner.roles) && (
        <section key={inheritedOwner.ownerId}>
          <NxH3>Inherited from {inheritedOwner.ownerName}</NxH3>
          <NxDescriptionList>{inheritedOwner.roles?.map(mapInheritedAccessRoles)}</NxDescriptionList>
        </section>
      )
    );
  };

  return (
    <NxTile id="access-tile-pill-access" data-testid="repositories_access">
      <NxLoadWrapper retryHandler={() => accessActions.loadRoles()} loading={loading}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Access</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>
              {isRepositoriesRelated ? 'All Repositories' : ownerName} users by role.
            </NxTile.HeaderSubtitle>
            <NxTile.HeaderActions>
              <a
                id="add-role-button"
                data-testid="add-role-button"
                className={classnames({ disabled: !rolesWithoutLocalMembersExist }, 'nx-btn', 'nx-btn--tertiary')}
                href={rolesWithoutLocalMembersExist ? getAddRoleButtonUrl() : undefined}
              >
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add a Role</span>
              </a>
            </NxTile.HeaderActions>
          </NxTile.Headings>
        </NxTile.Header>
        <NxTile.Content>
          <section key="iq-access-tile-local-access-section">
            <NxH3>Local</NxH3>
            <NxDescriptionList emptyMessage={'No local access configured.'} id="iq-access-tile-local-access-list">
              {localRoles?.map(mapLocalAccessDataRow)}
            </NxDescriptionList>
          </section>

          {inheritedRoles.map(mapInheritedAccessDataRow)}
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
