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
  NxDescriptionList,
  NxH2,
  NxH3,
  NxLoadWrapper,
  NxCollapsibleItems,
} from '@sonatype/react-shared-components';
import { selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import classnames from 'classnames';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectLoading,
  selectExtendedMembersByRole,
  selectRolesWithoutLocalMembersExist,
  selectInheritedAccessOpen,
} from 'MainRoot/OrgsAndPolicies/access/accessSelectors';

import { actions as accessActions } from 'MainRoot/OrgsAndPolicies/access/accessSlice';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectHasEditIqPermission } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { isEmpty } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

export default function AccessTile() {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const router = useSelector(selectRouterSlice);
  const loading = useSelector(selectLoading);
  const extMembersRoles = useSelector(selectExtendedMembersByRole);
  const rolesWithoutLocalMembersExist = useSelector(selectRolesWithoutLocalMembersExist);
  const ownerName = useSelector(selectSelectedOwnerName);
  const localRoles = extMembersRoles?.filter((role) => role.isInherited !== true)[0]?.roles || [];
  const inheritedAccessOpen = useSelector(selectInheritedAccessOpen);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);

  const inheritedRoles =
    extMembersRoles
      ?.filter((role) => role.isInherited === true)
      .map((owner) => ({ roles: [...owner.roles], ownerName: owner.ownerName, ownerId: owner.ownerId }))
      .flat() || [];

  useEffect(() => {
    dispatch(accessActions.loadRoles());
    if (!ownerName) {
      dispatch(actions.loadOwnerSummary());
    }
  }, []);

  const getAddRoleButtonUrl = () => {
    const { to, params } = deriveEditRoute(router, 'add-access');
    return uiRouterState.href(to, params);
  };

  const mapMembersData = (m, idx) => (
    <span key={idx} className="iq-access-tile-member-container">
      <NxFontAwesomeIcon icon={m.type === 'GROUP' ? faUsers : faUser} />
      <span className="iq-access-member-text">{m.displayName}</span>
    </span>
  );

  const editRoleUrl = (roleId) => {
    const { to, params } = deriveEditRoute(router, 'edit-access', { roleId });
    return uiRouterState.href(to, params);
  };

  const mapLocalAccessDataRow = (accessDataRow) => {
    return hasEditIqPermission ? (
      <NxDescriptionList.LinkItem
        key={accessDataRow.roleId}
        term={accessDataRow.roleName}
        description={accessDataRow?.members?.map(mapMembersData)}
        href={editRoleUrl(accessDataRow.roleId)}
      />
    ) : (
      <NxDescriptionList.Item key={accessDataRow.roleId}>
        <NxDescriptionList.Term>{accessDataRow.roleName}</NxDescriptionList.Term>
        <NxDescriptionList.Description>{accessDataRow?.members?.map(mapMembersData)}</NxDescriptionList.Description>
      </NxDescriptionList.Item>
    );
  };

  const toggleInheritedAccessOpen = (ownerId) => dispatch(accessActions.toggleInheritedAccessOpen(ownerId));

  const mapInheritedAccessDataRow = (inheritedOwner) => {
    return (
      inheritedOwner.roles &&
      !isEmpty(inheritedOwner.roles) && (
        <NxTile.Subsection key={inheritedOwner.ownerId}>
          <NxCollapsibleItems
            onToggleCollapse={() => toggleInheritedAccessOpen(inheritedOwner.ownerId)}
            isOpen={inheritedAccessOpen && inheritedAccessOpen[inheritedOwner.ownerId]}
            triggerContent={
              <>
                <h3 className="nx-h3 access-header">Inherited from {inheritedOwner.ownerName}</h3>
                {inheritedAccessOpen && !inheritedAccessOpen[inheritedOwner.ownerId]
                  ? getToggleInfo(inheritedOwner.roles)
                  : null}
              </>
            }
          >
            <dl id={'access-for-' + inheritedOwner.ownerId}>{inheritedOwner.roles?.map(mapInheritedAccessRoles)}</dl>
          </NxCollapsibleItems>
        </NxTile.Subsection>
      )
    );
  };

  const getToggleInfo = (membersByOwner) => {
    let totalUsers = 0;
    let totalGroups = 0;
    membersByOwner?.forEach((owner) => {
      if (owner) {
        const { users, groups } = countElements(owner.members);
        totalUsers += users;
        totalGroups += groups;
      }
    });
    return <div className="access-header-text">{totalUsers + ' Users and ' + totalGroups + ' User Groups'}</div>;
  };

  const countElements = (members) => {
    return members?.reduce(
      (accumulator, member) => {
        if (member.type === 'USER') {
          accumulator.users++;
        } else if (member.type === 'GROUP') {
          accumulator.groups++;
        }
        return accumulator;
      },
      { users: 0, groups: 0 }
    );
  };

  const mapInheritedAccessRoles = (accessDataRow) => {
    return (
      <NxCollapsibleItems.Child key={accessDataRow.roleId}>
        <div className="access-element">
          <dt className="access-label">
            <span>{accessDataRow.roleName}</span>
          </dt>
          <dd className="access-description">{accessDataRow?.members?.map(mapMembersData)}</dd>
        </div>
      </NxCollapsibleItems.Child>
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
            <NxTile.HeaderSubtitle>{ownerName} users by role.</NxTile.HeaderSubtitle>
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
            <NxH3> {'Local to ' + ownerName}</NxH3>
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
