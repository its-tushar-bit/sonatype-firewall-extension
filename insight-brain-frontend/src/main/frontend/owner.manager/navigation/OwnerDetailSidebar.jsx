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
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import { faPlus, faPencilAlt, faTag, faUser } from '@fortawesome/free-solid-svg-icons';
import MenuBarStatefulBreadcrumb from 'MainRoot/mainHeader/MenuBar/MenuBarStatefulBreadcrumb';
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
  selectIsRepositoriesRelated,
  selectIsCategory,
  selectIsPolicy,
  selectIsGrandfathering,
  selectIsMonitoring,
  selectIsProprietary,
  selectIsLabel,
  selectIsLicenseThreatGroup,
  selectIsSourceControl,
  selectIsAccess,
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
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isCategory = useSelector(selectIsCategory);
  const isPolicy = useSelector(selectIsPolicy);
  const isGrandfathering = useSelector(selectIsGrandfathering);
  const isMonitoring = useSelector(selectIsMonitoring);
  const isProprietary = useSelector(selectIsProprietary);
  const isLabel = useSelector(selectIsLabel);
  const isLicenseThreatGroup = useSelector(selectIsLicenseThreatGroup);
  const isSourceControl = useSelector(selectIsSourceControl);
  const isAccess = useSelector(selectIsAccess);
  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const labelsSiblings = useSelector(selectLabelsSiblings);
  const rolesSiblings = useSelector(selectRolesSiblings);
  const categoriesSiblings = useSelector(selectApplicationCategoriesSiblings);
  const policiesSiblings = useSelector(selectPolicySiblings);
  const licenseThreatGroupSiblings = useSelector(selectLicenseThreatGroupSiblings);
  const areAnyCategoriesDefined = useSelector(selectAreAnyCategoriesDefined);

  const uiRouterState = useRouterState();

  const getBackButtonHref = (isApp, isRepositoriesRelated, owner) => {
    if (isRepositoriesRelated) {
      return uiRouterState.href('management.view.repository_container', {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });
    } else if (isApp) {
      return uiRouterState.href('management.view.application', { applicationPublicId: owner.publicId });
    } else {
      return uiRouterState.href('management.view.organization', { organizationId: owner.id });
    }
  };

  const backButtonHref = getBackButtonHref(isApp, isRepositoriesRelated, owner);

  const getLinkMainHref = (isApp, isRepositoriesRelated, owner) => {
    if (isRepositoriesRelated) {
      return uiRouterState.href('management.edit.repository_container', {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });
    } else if (isApp) {
      return uiRouterState.href('management.edit.application', { applicationPublicId: owner.publicId });
    } else {
      return uiRouterState.href('management.edit.organization', { organizationId: owner.id });
    }
  };

  const linkMainHref = getLinkMainHref(isApp, isRepositoriesRelated, owner);

  const doLoad = () => dispatch(actions.loadSidebar());

  useEffect(() => {
    doLoad();
  }, [labelsSiblings, rolesSiblings, categoriesSiblings, policiesSiblings, licenseThreatGroupSiblings]);

  useEffect(() => {
    // remove this useEffect, when main sideBar will be same background-color as edit sideBar
    const sidebar = document.querySelector('.nx-page-sidebar');
    if (!sidebar) {
      return;
    }
    sidebar.style.backgroundColor = 'var(--nx-swatch-indigo-90)';
  }, []);

  useEffect(() => {
    if (isCategory) {
      setCategoryOpenState(true);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (isPolicy) {
      setCategoryOpenState(false);
      setPoliciesOpenState(true);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (isLabel) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(true);
      setLtgOpenState(false);
      setAccessOpenState(false);
    }
    if (isLicenseThreatGroup) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(true);
      setAccessOpenState(false);
    }
    if (isAccess) {
      setCategoryOpenState(false);
      setPoliciesOpenState(false);
      setLabelsOpenState(false);
      setLtgOpenState(false);
      setAccessOpenState(true);
    }
  }, [url]);

  return (
    <div id="owner-detail-sidebar">
      {isRepositoriesRelated ? (
        <MenuBarBackButton href={backButtonHref} text={'All Repositories'} />
      ) : (
        <MenuBarStatefulBreadcrumb />
      )}
      <NxH3>{owner.name}</NxH3>
      <NxH4>Policy Management</NxH4>

      {/* Categories */}
      {!isRepositoriesRelated && (
        <NxCollapsibleItems
          id="application-category-group"
          role="menu"
          onToggleCollapse={onCategoryCollapse}
          isOpen={categoryOpen}
          triggerContent="Application Categories"
          className={isCategory ? 'active' : ''}
        >
          <NxTooltip title={isApp && !areAnyCategoriesDefined ? 'No application categories defined.' : ''}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink
                className={isCategory && !categoryId ? 'selected' : ''}
                href={`${linkMainHref}/category`}
                disabled={isApp && !areAnyCategoriesDefined}
              >
                <NxFontAwesomeIcon icon={isApp ? faPencilAlt : faPlus} />
                {isApp ? 'Assign App Categories' : 'New Category'}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          </NxTooltip>

          {tags?.map(({ name, id, color }) => (
            <NxOverflowTooltip key={name}>
              <NxCollapsibleItems.Child role="menuitem">
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
            </NxOverflowTooltip>
          ))}
        </NxCollapsibleItems>
      )}
      {/* Policies */}
      <NxCollapsibleItems
        id="policy-group"
        role="menu"
        onToggleCollapse={onPoliciesCollapse}
        isOpen={policiesOpen}
        triggerContent="Policies"
        className={isPolicy ? 'active' : ''}
      >
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink className={isPolicy && !policyId ? 'selected' : ''} href={`${linkMainHref}/policy`}>
            <NxFontAwesomeIcon icon={faPlus} />
            New Policy
          </NxTextLink>
        </NxCollapsibleItems.Child>
        {policies &&
          sort((a, b) => b.threatLevel - a.threatLevel, policies).map(({ name, id, threatLevel }) => (
            <NxOverflowTooltip key={name}>
              <NxCollapsibleItems.Child role="menuitem">
                <NxTextLink className={id === policyId ? 'selected' : ''} href={`${linkMainHref}/policy/${id}`}>
                  <NxThreatIndicator policyThreatLevel={threatLevel} />
                  {name}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            </NxOverflowTooltip>
          ))}
      </NxCollapsibleItems>

      {/* Grandfathering */}
      {!isRepositoriesRelated && (
        <NxTooltip
          title={!isGrandfatheringSupported ? 'Policy Violation Grandfathering is not supported by your license' : ''}
        >
          <NxCollapsibleItems.Child>
            <NxTextLink
              id="grandfathering-link"
              className={`iq-noncollapsible ${isGrandfathering && !currentRoleId ? 'selected' : ''}`}
              href={`${linkMainHref}/grandfathering`}
              disabled={!isGrandfatheringSupported}
            >
              Grandfathering
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>
      )}
      {/* Monitoring */}
      {!isRepositoriesRelated && (
        <NxTooltip title={!isMonitoringSupported ? 'Policy Monitoring is not supported by your license' : ''}>
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              id="continous-monitoring-link"
              className={`iq-noncollapsible ${isMonitoring && !currentRoleId ? 'selected' : ''}`}
              href={`${linkMainHref}/monitoring`}
              disabled={!isMonitoringSupported}
            >
              Continuous Monitoring
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>
      )}
      {/* Waived Component Upgrades */}
      {!isRepositoriesRelated && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            id="upgrade-available-link"
            className={`iq-noncollapsible ${
              url.includes('/waivedComponentUpgrades') && !currentRoleId ? 'selected' : ''
            }`}
            href={`${linkMainHref}/waivedComponentUpgrades`}
          >
            Waived Component Upgrades
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      {/* Proprietary */}
      {!isRepositoriesRelated && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            id="proprietary-components-link"
            className={`iq-noncollapsible last ${isProprietary && !currentRoleId ? 'selected' : ''}`}
            href={`${linkMainHref}/proprietary`}
          >
            Proprietary Components
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      {/* Labels */}
      {!isRepositoriesRelated && (
        <NxCollapsibleItems
          id="label-group"
          role="menu"
          onToggleCollapse={onLabelsCollapse}
          isOpen={labelsOpen}
          triggerContent="Component Labels"
          className={`label-list-menu label-group ${isLabel ? 'active' : ''}`}
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink className={isLabel && !labelId ? 'selected' : ''} href={`${linkMainHref}/label`}>
              <NxFontAwesomeIcon icon={faPlus} />
              New Component Label
            </NxTextLink>
          </NxCollapsibleItems.Child>
          {labels?.map(({ label, id, color }) => (
            <NxOverflowTooltip key={label}>
              <NxCollapsibleItems.Child role="menuitem">
                <NxTextLink className={id === labelId ? 'selected' : ''} href={`${linkMainHref}/label/${id}`}>
                  <NxFontAwesomeIcon
                    icon={faTag}
                    className={angularToRscColorMap[color] ? `nx-selectable-color--${angularToRscColorMap[color]}` : ''}
                  />
                  {label}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            </NxOverflowTooltip>
          ))}
        </NxCollapsibleItems>
      )}
      {/* License Threat Groups */}
      {!isRepositoriesRelated && !isApp && (
        <NxCollapsibleItems
          id="license-threat-group-group"
          role="menu"
          onToggleCollapse={onLtgCollapse}
          isOpen={ltgOpen}
          triggerContent="License Threat Groups"
          className={isLicenseThreatGroup ? 'active' : ''}
        >
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              className={isLicenseThreatGroup && !licenseThreatGroupId ? 'selected' : ''}
              href={`${linkMainHref}/licenseThreatGroup`}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              New License Threat Group
            </NxTextLink>
          </NxCollapsibleItems.Child>
          {licenseThreatGroups &&
            sort((a, b) => b.threatLevel - a.threatLevel, licenseThreatGroups).map(({ name, id, threatLevel }) => (
              <NxOverflowTooltip key={name}>
                <NxCollapsibleItems.Child role="menuitem">
                  <NxTextLink
                    className={id === licenseThreatGroupId ? 'selected' : ''}
                    href={`${linkMainHref}/licenseThreatGroup/${id}`}
                  >
                    <NxThreatIndicator policyThreatLevel={threatLevel} />
                    {name}
                  </NxTextLink>
                </NxCollapsibleItems.Child>
              </NxOverflowTooltip>
            ))}
        </NxCollapsibleItems>
      )}
      {/* Source Control */}
      {!isRepositoriesRelated && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            className={`iq-noncollapsible ${isSourceControl ? 'selected' : ''}`}
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
        className={isAccess ? 'active' : ''}
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
              className={isAccess && !currentRoleId ? 'selected' : ''}
              href={`${linkMainHref}/access`}
              disabled={!doesRolesWithoutLocalMembersExist}
            >
              <NxFontAwesomeIcon icon={faPlus} />
              Add a Role
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>

        {roles?.map(({ roleName, roleId }) => (
          <NxOverflowTooltip key={roleId}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink
                className={roleId === currentRoleId ? 'selected' : ''}
                href={`${linkMainHref}/access/${roleId}`}
              >
                <NxFontAwesomeIcon icon={faUser} />
                {roleName}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          </NxOverflowTooltip>
        ))}
      </NxCollapsibleItems>
    </div>
  );
}
