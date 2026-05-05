/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as ReactDOM from 'react-dom';
import { useSelector, useDispatch } from 'react-redux';
import { sort } from 'ramda';
import {
  NxCollapsibleItems,
  NxTextLink,
  NxFontAwesomeIcon,
  useToggle,
  NxH3,
  NxThreatIndicator,
  NxTooltip,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import { faPlus, faPencilAlt, faTag, faUser } from '@fortawesome/free-solid-svg-icons';
import { faLock } from '@fortawesome/pro-regular-svg-icons';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectOwnerDetails,
  selectRolesWithoutLocalMembersExist,
} from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import {
  selectIsRootOrganization,
  selectIsApplication,
  selectRouterState,
  selectRouterCurrentParams,
  selectIsRepositoriesRelated,
  selectIsRepositoryManager,
  selectIsRepository,
  selectIsCategory,
  selectIsPolicy,
  selectIsLegacyViolation,
  selectIsMonitoring,
  selectIsProprietary,
  selectIsLabel,
  selectIsLicenseThreatGroup,
  selectIsSourceControl,
  selectIsAccess,
  selectIsSbomManager,
  selectIsWaivers,
  selectIsPublicDataSources,
  selectIsFirewall,
  selectPrefixRoute,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsLegacyViolationSupported,
  selectIsMonitoringSupported,
  selectIsOrgsAndAppsEnabled,
  selectIsProprietaryComponentsEnabled,
  selectIsSourceControlForSourceTileSupported,
  selectIsScmEnabled,
  selectIsSbomContinuousMonitoringUiEnabled,
  selectIsDeveloperDashboardEnabled,
  selectIsAutoWaiversEnabled,
  selectIsCpeMatchingSupported,
  selectHasCustomPolicies,
  selectHasCustomAppCategories,
  selectHasCustomComponentLabels,
  selectHasCustomLicenseThreatGroups,
  selectHasAutoWaiverManagement,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';
import Hexagon from 'MainRoot/react/Hexagon';
import { backendToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';
import { selectLabelsSiblings } from 'MainRoot/OrgsAndPolicies/labelsSelectors';
import { selectRolesSiblings } from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import { selectSiblings as selectApplicationCategoriesSiblings } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectSiblings as selectPolicySiblings } from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectLicenseThreatGroupSiblings } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { selectAreAnyCategoriesDefined } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import { IQ_SIDEBAR_CONTAINER_ID } from 'MainRoot/util/constants';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

export default function OwnerDetailSidebar() {
  const dispatch = useDispatch();

  const portalContainer = document.getElementById(IQ_SIDEBAR_CONTAINER_ID);

  const [categoryOpen, onCategoryCollapse, setCategoryOpenState] = useToggle(false);
  const [policiesOpen, onPoliciesCollapse, setPoliciesOpenState] = useToggle(false);
  const [labelsOpen, onLabelsCollapse, setLabelsOpenState] = useToggle(false);
  const [ltgOpen, onLtgCollapse, setLtgOpenState] = useToggle(false);
  const [accessOpen, onAccessCollapse, setAccessOpenState] = useToggle(false);

  const owner = useSelector(selectSelectedOwner);
  const isRootOrganization = useSelector(selectIsRootOrganization);
  const isApp = useSelector(selectIsApplication);
  const { url } = useSelector(selectRouterState);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const { categoryId, policyId, labelId, licenseThreatGroupId, roleId: currentRoleId } = routerCurrentParams;
  const { tags, policies, labels, licenseThreatGroups, roles } = useSelector(selectOwnerDetails);
  const doesRolesWithoutLocalMembersExist = useSelector(selectRolesWithoutLocalMembersExist);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isRepository = useSelector(selectIsRepository);
  const isCategory = useSelector(selectIsCategory);
  const isPolicy = useSelector(selectIsPolicy);
  const isLegacyViolations = useSelector(selectIsLegacyViolation);
  const isMonitoring = useSelector(selectIsMonitoring);
  const isProprietary = useSelector(selectIsProprietary);
  const isLabel = useSelector(selectIsLabel);
  const isLicenseThreatGroup = useSelector(selectIsLicenseThreatGroup);
  const isSourceControl = useSelector(selectIsSourceControl);
  const isAccess = useSelector(selectIsAccess);
  const isLegacyViolationsSupported = useSelector(selectIsLegacyViolationSupported);
  const isProprietaryComponentsEnabled = useSelector(selectIsProprietaryComponentsEnabled);
  const isSourceControlForSourceTileSupported = useSelector(selectIsSourceControlForSourceTileSupported);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isScmEnabled = useSelector(selectIsScmEnabled);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const labelsSiblings = useSelector(selectLabelsSiblings);
  const rolesSiblings = useSelector(selectRolesSiblings);
  const categoriesSiblings = useSelector(selectApplicationCategoriesSiblings);
  const policiesSiblings = useSelector(selectPolicySiblings);
  const licenseThreatGroupSiblings = useSelector(selectLicenseThreatGroupSiblings);
  const areAnyCategoriesDefined = useSelector(selectAreAnyCategoriesDefined);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isSbomManagerOnlyLicense = useSelector(selectIsSbomManagerOnlyLicense);
  const isSbomContinuousMonitoringUiEnabled = useSelector(selectIsSbomContinuousMonitoringUiEnabled);
  const isWaivers = useSelector(selectIsWaivers);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiverEnabled = useSelector(selectIsAutoWaiversEnabled);

  const hasCustomPolicies = useSelector(selectHasCustomPolicies);
  const hasCustomAppCategories = useSelector(selectHasCustomAppCategories);
  const hasCustomComponentLabels = useSelector(selectHasCustomComponentLabels);
  const hasCustomLicenseThreatGroups = useSelector(selectHasCustomLicenseThreatGroups);
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);
  const isCpeMatchingSupported = useSelector(selectIsCpeMatchingSupported);
  const isPublicDataSources = useSelector(selectIsPublicDataSources);
  const isFirewall = useSelector(selectIsFirewall);
  const prefixRoute = useSelector(selectPrefixRoute);

  const uiRouterState = useRouterState();

  const getLinkMainHref = (isApp, isRepositoriesRelated, owner) => {
    if (isRepositoriesRelated) {
      if (isRepositoryManager) {
        return uiRouterState.href(prefixRoute('management.edit.repository_manager'), {
          repositoryManagerId: owner.id,
        });
      }
      if (isRepository) {
        return uiRouterState.href(prefixRoute('management.edit.repository'), { repositoryId: owner.id });
      }
      return uiRouterState.href(prefixRoute('management.edit.repository_container'), {
        repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      });
    } else if (isApp) {
      return uiRouterState.href(prefixRoute('management.edit.application'), { applicationPublicId: owner.publicId });
    } else {
      return uiRouterState.href(prefixRoute('management.edit.organization'), { organizationId: owner.id });
    }
  };

  const linkMainHref = getLinkMainHref(isApp, isRepositoriesRelated, owner);

  const doLoad = () => dispatch(actions.loadSidebar());

  useEffect(() => {
    doLoad();
  }, [
    labelsSiblings,
    rolesSiblings,
    categoriesSiblings,
    policiesSiblings,
    licenseThreatGroupSiblings,
    routerCurrentParams,
  ]);

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

  return ReactDOM.createPortal(
    <div id="owner-detail-sidebar" className="nx-page-sidebar">
      <NxH3>{owner.name}</NxH3>

      {/* Categories */}
      {!isRepositoriesRelated && isOrgsAndAppsEnabled && !isSbomManager && (
        <NxCollapsibleItems
          id="application-category-group"
          role="menu"
          onToggleCollapse={onCategoryCollapse}
          isOpen={categoryOpen}
          triggerContent="Application Categories"
          className={isCategory ? 'active' : ''}
        >
          <NxTooltip title={!isApp && !hasCustomAppCategories ? 'Enterprise Feature' : (isApp && !areAnyCategoriesDefined ? 'No application categories defined.' : '')}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink
                className={isCategory && !categoryId ? 'selected' : ''}
                href={`${linkMainHref}/category`}
                disabled={isApp && !areAnyCategoriesDefined}
              >
                <NxFontAwesomeIcon icon={isApp ? faPencilAlt : faPlus} />
                <span>{isApp ? 'Assign App Categories' : 'New Category'}</span>
                {!isApp && !hasCustomAppCategories && <>{' '}<NxFontAwesomeIcon icon={faLock} className="iq-sidebar-crown" /></>}
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
                    className={backendToRscColorMap[color] ? `nx-selectable-color--${backendToRscColorMap[color]}` : ''}
                  />
                  {name}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            </NxOverflowTooltip>
          ))}
        </NxCollapsibleItems>
      )}
      {/* Policies */}
      {(!isSbomManager || isRootOrganization) && (
        <NxCollapsibleItems
          id="policy-group"
          role="menu"
          onToggleCollapse={onPoliciesCollapse}
          isOpen={policiesOpen}
          triggerContent="Policies"
          className={isPolicy ? 'active' : ''}
        >
          {!isSbomManager && (
            <NxTooltip title={!hasCustomPolicies ? 'Enterprise Feature' : ''}>
              <NxCollapsibleItems.Child role="menuitem">
                <NxTextLink className={isPolicy && !policyId ? 'selected' : ''} href={`${linkMainHref}/policy`}>
                  <NxFontAwesomeIcon icon={faPlus} />
                  <span>New Policy</span>
                  {!hasCustomPolicies && <>{' '}<NxFontAwesomeIcon icon={faLock} className="iq-sidebar-crown" /></>}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            </NxTooltip>
          )}
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
      )}
      {/* Legacy Violations */}
      {!isRepositoriesRelated && isLegacyViolationsSupported && !isSbomManager && (
        <NxCollapsibleItems.Child>
          <NxTextLink
            id="legacy-violations-link"
            className={`iq-noncollapsible ${isLegacyViolations && !currentRoleId ? 'selected' : ''}`}
            href={`${linkMainHref}/legacyViolations`}
          >
            Legacy Violations
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      {/* Monitoring */}
      {!isRepositoriesRelated && isMonitoringSupported && (!isSbomManager || isSbomContinuousMonitoringUiEnabled) && (
        <NxCollapsibleItems.Child role="menuitem">
          <NxTextLink
            id="continous-monitoring-link"
            className={`iq-noncollapsible ${isMonitoring && !currentRoleId ? 'selected' : ''} ${
              isSbomManager ? 'sbomManager' : ''
            }`}
            href={`${linkMainHref}/monitoring`}
          >
            Continuous Monitoring
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
      {/* Proprietary */}
      {!isRepositoriesRelated && isProprietaryComponentsEnabled && !isSbomManager && (
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
      {!isRepositoriesRelated && !isSbomManager && (
        <NxCollapsibleItems
          id="label-group"
          role="menu"
          onToggleCollapse={onLabelsCollapse}
          isOpen={labelsOpen}
          triggerContent="Component Labels"
          className={`label-list-menu label-group ${isLabel ? 'active' : ''}`}
        >
          <NxTooltip title={!hasCustomComponentLabels ? 'Enterprise Feature' : ''}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink className={isLabel && !labelId ? 'selected' : ''} href={`${linkMainHref}/label`}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>New Component Label</span>
                {!hasCustomComponentLabels && <>{' '}<NxFontAwesomeIcon icon={faLock} className="iq-sidebar-crown" /></>}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          </NxTooltip>
          {labels?.map(({ label, id, color }) => (
            <NxOverflowTooltip key={label}>
              <NxCollapsibleItems.Child role="menuitem">
                <NxTextLink className={id === labelId ? 'selected' : ''} href={`${linkMainHref}/label/${id}`}>
                  <NxFontAwesomeIcon
                    icon={faTag}
                    className={backendToRscColorMap[color] ? `nx-selectable-color--${backendToRscColorMap[color]}` : ''}
                  />
                  {label}
                </NxTextLink>
              </NxCollapsibleItems.Child>
            </NxOverflowTooltip>
          ))}
        </NxCollapsibleItems>
      )}
      {/* License Threat Groups */}
      {!isRepositoriesRelated && !isSbomManager && !isApp && (
        <NxCollapsibleItems
          id="license-threat-group-group"
          role="menu"
          onToggleCollapse={onLtgCollapse}
          isOpen={ltgOpen}
          triggerContent="License Threat Groups"
          className={isLicenseThreatGroup ? 'active' : ''}
        >
          <NxTooltip title={!hasCustomLicenseThreatGroups ? 'Enterprise Feature' : ''}>
            <NxCollapsibleItems.Child role="menuitem">
              <NxTextLink
                className={isLicenseThreatGroup && !licenseThreatGroupId ? 'selected' : ''}
                href={`${linkMainHref}/licenseThreatGroup`}
              >
                <NxFontAwesomeIcon icon={faPlus} />
                <span>New License Threat Group</span>
                {!hasCustomLicenseThreatGroups && <>{' '}<NxFontAwesomeIcon icon={faLock} className="iq-sidebar-crown" /></>}
              </NxTextLink>
            </NxCollapsibleItems.Child>
          </NxTooltip>
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
      {!isRepositoriesRelated && isSourceControlForSourceTileSupported && isScmEnabled && !isSbomManager && (
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
      {/* Waivers */}
      {!isRepositoriesRelated && isDeveloperDashboardEnabled && !isSbomManager && !isFirewall && isAutoWaiverEnabled && (
        <NxTooltip title={!hasAutoWaiverManagement ? 'Enterprise Feature' : ''}>
          <NxCollapsibleItems.Child role="menuitem">
            <NxTextLink
              className={`iq-noncollapsible ${isWaivers ? 'selected' : ''}`}
              href={`${linkMainHref}/autowaivers`}
            >
              <span>Auto-Waivers</span>
              {!hasAutoWaiverManagement && <>{' '}<NxFontAwesomeIcon icon={faLock} className="iq-sidebar-crown" /></>}
            </NxTextLink>
          </NxCollapsibleItems.Child>
        </NxTooltip>
      )}
      {isCpeMatchingSupported && !isSbomManagerOnlyLicense && !isFirewall && (
        <NxCollapsibleItems.Child>
          <NxTextLink
            id="public-data-sources-link"
            className={`iq-noncollapsible ${isPublicDataSources ? 'selected' : ''}`}
            href={`${linkMainHref}/publicDataSourcesEditor`}
          >
            Public Data Sources
          </NxTextLink>
        </NxCollapsibleItems.Child>
      )}
    </div>,
    portalContainer
  );
}
