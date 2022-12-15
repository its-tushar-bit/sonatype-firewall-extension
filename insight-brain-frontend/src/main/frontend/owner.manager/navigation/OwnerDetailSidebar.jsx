/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { sort } from 'ramda';
import {
  NxCollapsibleItems,
  NxTextLink,
  NxFontAwesomeIcon,
  useToggle,
  NxH4,
  NxH3,
  NxThreatIndicator,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faPlus, faPencilAlt, faTag, faUser } from '@fortawesome/free-solid-svg-icons';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectOwnerDetails,
  selectRolesWithoutLocalMembersExist,
} from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import {
  selectIsApplication,
  selectRouterState,
  selectRouterCurrentParams,
  selectIsRepositories,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsMonitoringSupported,
  selectIsGrandfatheringSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';
import Hexagon from 'MainRoot/react/Hexagon';
import { angularToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectLabelsSiblings } from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import { selectRolesSiblings } from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import { selectSiblings as selectApplicationCategoriesSiblings } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectSiblings as selectPolicySiblings } from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectLicenseThreatGroupSiblings } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { selectAreAnyCategoriesDefined } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';

export default function OwnerDetailSidebar() {
  const dispatch = useDispatch();

  const [categoryOpen, onCategoryCollapse, setCategoryOpenState] = useToggle(false);
  const [policiesOpen, onPoliciesCollapse, setPoliciesOpenState] = useToggle(false);
  const [labelsOpen, onLabelsCollapse, setLabelsOpenState] = useToggle(false);
  const [ltgOpen, onLtgCollapse, setLtgOpenState] = useToggle(false);
  const [accessOpen, onAccessCollapse, setAccessOpenState] = useToggle(false);

  const owner = useSelector(selectSelectedOwner);
  const isApp = useSelector(selectIsApplication);
  const { url } = useSelector(selectRouterState);
  const { categoryId, policyId, labelId, licenseThreatGroupId, roleId: currentRoleId } = useSelector(
    selectRouterCurrentParams
  );
  const { tags, policies, labels, licenseThreatGroups, roles } = useSelector(selectOwnerDetails);
  const doesRolesWithoutLocalMembersExist = useSelector(selectRolesWithoutLocalMembersExist);
  const isRepositories = useSelector(selectIsRepositories);
  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const labelsSublings = useSelector(selectLabelsSiblings);
  const rolesSublings = useSelector(selectRolesSiblings);
  const categoriesSublings = useSelector(selectApplicationCategoriesSiblings);
  const policiesSublings = useSelector(selectPolicySiblings);
  const licenseThreatGroupSiblings = useSelector(selectLicenseThreatGroupSiblings);
  const areAnyCategoriesDefined = useSelector(selectAreAnyCategoriesDefined);

  const uiRouterState = useRouterState();

  const backButtonHref = uiRouterState.href(
    `management.view.${isRepositories ? 'repositories' : isApp ? 'application' : 'organization'}`,
    isApp
      ? {
          applicationPublicId: owner.publicId,
        }
      : {
          organizationId: owner.id,
        }
  );

  const linkMainHref = uiRouterState.href(
    `management.edit.${isRepositories ? 'repositories' : isApp ? 'application' : 'organization'}`,
    isApp
      ? {
          applicationPublicId: owner.publicId,
        }
      : {
          organizationId: owner.id,
        }
  );

  const doLoad = () => dispatch(actions.loadSidebar());

  useEffect(() => {
    doLoad();
  }, [labelsSublings, rolesSublings, categoriesSublings, policiesSublings, licenseThreatGroupSiblings]);

  useEffect(() => {
    // remove this useEffect, when main sideBar will be same background-color as edit sideBar
    const sidebar = document.querySelector('.nx-page-sidebar');
    if (!sidebar) {
      return;
    }
    sidebar.style.backgroundColor = 'var(--nx-swatch-indigo-90)';
  }, []);

  useEffect(() => {
    if (url.includes('/category')) {
      setCategoryOpenState(true);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (url.includes('/policy')) {
      setCategoryOpenState(false);
      setPoliciesOpenState(true);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (url.includes('/label')) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(true);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (url.includes('/licenseThreatGroup')) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(true);
      setAccessOpenState(false);
    }
    if (url.includes('/access')) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(true);
    }
  }, [url]);

  return (
    <div id="owner-detail-sidebar">
      <MenuBarBackButton href={backButtonHref} text={isRepositories ? 'All Repositories' : `Back to ${owner.name}`} />
      <NxH3>{owner.name}</NxH3>
      <NxH4>Policy Management</NxH4>

      {/* Categories */}
      {!isRepositories && (
        <NxCollapsibleItems
          id="application-category-group"
          role="menu"
          onToggleCollapse={onCategoryCollapse}
          isOpen={categoryOpen}
          triggerContent="Application Categories"
          className={url.includes('/category') ? 'active' : ''}
        >
          <NxTooltip title={isApp && !areAnyCategoriesDefined ? 'No application categories defined.' : ''}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink
                className={url.includes('/category') && !categoryId ? 'selected' : ''}
                href={`${linkMainHref}/category`}
                disabled={isApp && !areAnyCategoriesDefined}
              >
                <NxFontAwesomeIcon icon={isApp ? faPencilAlt : faPlus} />
                {isApp ? 'Assign App Categories' : 'New Category'}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          </NxTooltip>

          {tags?.map(({ name, id, color }) => (
            <NxCollapsibleItems.Child role="menuitem" key={name}>
              <NxTextLink
                className={categoryId && id === categoryId ? 'selected' : ''}
                href={`${linkMainHref}/category/${id}`}
              >
                <Hexagon
                  className={angularToRscColorMap[color] ? `nx-selectable-color--${angularToRscColorMap[color]}` : ''}
                />
                {name}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          ))}
        </NxCollapsibleItems>
      )}
      {/* Policies */}
      {!isRepositories && (
        <NxCollapsibleItems
          id="policy-group"
          role="menu"
          onToggleCollapse={onPoliciesCollapse}
          isOpen={policiesOpen}
          triggerContent="Policies"
          className={url.includes('/policy') ? 'active' : ''}
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              className={url.includes('/policy') && !policyId ? 'selected' : ''}
              href={`${linkMainHref}/policy`}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              New Policy
            </NxTextLink>
          </NxCollapsibleItems.Child>
          {policies &&
            sort((a, b) => b.threatLevel - a.threatLevel, policies).map(({ name, id, threatLevel }) => (
              <NxCollapsibleItems.Child role="menuitem" key={name}>
                <NxTextLink className={id === policyId ? 'selected' : ''} href={`${linkMainHref}/policy/${id}`}>
                  <NxThreatIndicator policyThreatLevel={threatLevel} />
                  {name}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            ))}
        </NxCollapsibleItems>
      )}
      {/* Grandfathering */}
      {!isRepositories && (
        <NxTooltip
          title={!isGrandfatheringSupported ? 'Policy Violation Grandfathering is not supported by your license' : ''}
        >
          <NxCollapsibleItems.Child>
            <NxTextLink
              id="grandfathering-link"
              className={`iq-noncollapsible ${url.includes('/grandfathering') && !currentRoleId ? 'selected' : ''}`}
              href={`${linkMainHref}/grandfathering`}
              disabled={!isGrandfatheringSupported}
            >
              Grandfathering
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>
      )}
      {/* Monitoring */}
      {!isRepositories && (
        <NxTooltip title={!isMonitoringSupported ? 'Policy Monitoring is not supported by your license' : ''}>
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              id="continous-monitoring-link"
              className={`iq-noncollapsible ${url.includes('/monitoring') && !currentRoleId ? 'selected' : ''}`}
              href={`${linkMainHref}/monitoring`}
              disabled={!isMonitoringSupported}
            >
              Continuous Monitoring
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>
      )}
      {/* Proprietary */}
      {!isRepositories && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            id="proprietary-components-link"
            className={`iq-noncollapsible last ${url.includes('/proprietary') && !currentRoleId ? 'selected' : ''}`}
            href={`${linkMainHref}/proprietary`}
          >
            Proprietary Components
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      {/* Labels */}
      {!isRepositories && (
        <NxCollapsibleItems
          id="label-group"
          role="menu"
          onToggleCollapse={onLabelsCollapse}
          isOpen={labelsOpen}
          triggerContent="Component Labels"
          className={`label-list-menu label-group ${url.includes('/label') ? 'active' : ''}`}
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink className={url.includes('/label') && !labelId ? 'selected' : ''} href={`${linkMainHref}/label`}>
              <NxFontAwesomeIcon icon={faPlus} />
              New Component Label
            </NxTextLink>
          </NxCollapsibleItems.Child>
          {labels?.map(({ label, id, color }) => (
            <NxCollapsibleItems.Child role="menuitem" key={label}>
              <NxTextLink className={id === labelId ? 'selected' : ''} href={`${linkMainHref}/label/${id}`}>
                <NxFontAwesomeIcon
                  icon={faTag}
                  className={angularToRscColorMap[color] ? `nx-selectable-color--${angularToRscColorMap[color]}` : ''}
                />
                {label}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          ))}
        </NxCollapsibleItems>
      )}
      {/* License Threat Groups */}
      {!isRepositories && !isApp && (
        <NxCollapsibleItems
          id="license-threat-group-group"
          role="menu"
          onToggleCollapse={onLtgCollapse}
          isOpen={ltgOpen}
          triggerContent="License Threat Groups"
          className={url.includes('/licenseThreatGroup') ? 'active' : ''}
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              className={url.includes('/licenseThreatGroup') && !licenseThreatGroupId ? 'selected' : ''}
              href={`${linkMainHref}/licenseThreatGroup`}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              New License Threat Group
            </NxTextLink>
          </NxCollapsibleItems.Child>
          {licenseThreatGroups &&
            sort((a, b) => b.threatLevel - a.threatLevel, licenseThreatGroups).map(({ name, id, threatLevel }) => (
              <NxCollapsibleItems.Child role="menuitem" key={name}>
                <NxTextLink
                  className={id === licenseThreatGroupId ? 'selected' : ''}
                  href={`${linkMainHref}/licenseThreatGroup/${id}`}
                >
                  <NxThreatIndicator policyThreatLevel={threatLevel} />
                  {name}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            ))}
        </NxCollapsibleItems>
      )}
      {/* Source Control */}
      {!isRepositories && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            className={`iq-noncollapsible ${url.includes('/source-control') ? 'selected' : ''}`}
            href={`${linkMainHref}/source-control`}
          >
            Source Control
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      <NxCollapsibleItems
        id="access-group"
        role="menu"
        onToggleCollapse={onAccessCollapse}
        isOpen={accessOpen}
        triggerContent="Access"
        className={url.includes('/access') ? 'active' : ''}
      >
        <NxTooltip
          title={
            !doesRolesWithoutLocalMembersExist
              ? `All of the available roles are already associated with ${owner.name}.`
              : ''
          }
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              className={url.includes('/access') && !currentRoleId ? 'selected' : ''}
              href={`${linkMainHref}/access`}
              disabled={!doesRolesWithoutLocalMembersExist}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              Add a Role
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>

        {roles?.map(({ roleName, roleId }) => (
          <NxCollapsibleItems.Child role="menuitem" key={roleId}>
            <NxTextLink
              className={roleId === currentRoleId ? 'selected' : ''}
              href={`${linkMainHref}/access/${roleId}`}
            >
              <NxFontAwesomeIcon icon={faUser} />
              {roleName}
            </NxTextLink>
          </NxCollapsibleItems.Child>
        ))}
      </NxCollapsibleItems>
    </div>
  );
}
