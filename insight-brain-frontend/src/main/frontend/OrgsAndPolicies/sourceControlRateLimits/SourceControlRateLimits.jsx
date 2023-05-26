/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxButton,
  NxButtonBar,
  NxCollapsibleItems,
  NxFontAwesomeIcon,
  NxH1,
  NxOverflowTooltip,
  NxPageMain,
  NxPageTitle,
  NxTable,
  NxTextLink,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {
  selectLoading,
  selectLoadError,
  selectData,
  selectUserRateLimitsExpanded,
  selectSortColumn,
  selectSortDirection,
  selectUserDefiningOwnersExpanded,
  selectUserAssociatedApplicationsExpanded,
  selectLastUpdated,
} from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/sourceControlRateLimitsSlice';
import { round } from 'MainRoot/util/jsUtil';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { faExclamationCircle, faExclamationTriangle, faCheckCircle, faSync } from '@fortawesome/pro-solid-svg-icons';

export default function SourceControlRateLimits() {
  const dispatch = useDispatch();

  const load = () => dispatch(actions.load());
  const setSort = (column) => dispatch(actions.setSort(column));
  const toggleUserRateLimitsExpanded = (user) => dispatch(actions.toggleUserRateLimitsExpanded(user));
  const toggleUserDefiningOwnersExpanded = (user) => dispatch(actions.toggleUserDefiningOwnersExpanded(user));
  const toggleUserAssociatedApplicationsExpanded = (user) =>
    dispatch(actions.toggleUserAssociatedApplicationsExpanded(user));
  const goToOrganizationEditSourceControl = (organizationId) =>
    dispatch(stateGo('management.edit.organization.edit-source-control', { organizationId }));
  const goToApplicationEditSourceControl = (applicationPublicId) =>
    dispatch(stateGo('management.edit.application.edit-source-control', { applicationPublicId }));

  const goToOwnerEditSourceControl = (owner) => {
    switch (owner.ownerType) {
      case 'ORGANIZATION': {
        goToOrganizationEditSourceControl(owner.ownerId);
        return;
      }
      case 'APPLICATION': {
        goToApplicationEditSourceControl(owner.ownerPublicId);
        return;
      }
      default: {
        return;
      }
    }
  };

  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const data = useSelector(selectData);
  const sortColumn = useSelector(selectSortColumn);
  const sortDirection = useSelector(selectSortDirection);
  const userRateLimitsExpanded = useSelector(selectUserRateLimitsExpanded);
  const userDefiningOwnersExpanded = useSelector(selectUserDefiningOwnersExpanded);
  const userAssociatedApplicationsExpanded = useSelector(selectUserAssociatedApplicationsExpanded);
  const lastUpdated = useSelector(selectLastUpdated);

  useEffect(() => {
    load();
  }, []);

  const getRateLimitPercentIcon = (rateLimitPercent) => {
    if (rateLimitPercent > 50) {
      return <NxFontAwesomeIcon className="source-control-rate-limit-good-icon" icon={faCheckCircle} />;
    }
    if (rateLimitPercent > 0) {
      return <NxFontAwesomeIcon className="source-control-rate-limit-warn-icon" icon={faExclamationTriangle} />;
    }
    return <NxFontAwesomeIcon className="source-control-rate-limit-fail-icon" icon={faExclamationCircle} />;
  };

  return (
    <NxPageMain>
      <NxPageTitle>
        <NxPageTitle.Headings>
          <NxH1>Source Control User Usage</NxH1>
          {data && (
            <NxPageTitle.Subtitle>
              for {data.ownerName + (data.ownerType === 'organization' ? ' and descendants' : '')}
            </NxPageTitle.Subtitle>
          )}
        </NxPageTitle.Headings>
        <NxPageTitle.Description>
          This is an experimental page and subject to change. Only GitHub users are supported at this time.
        </NxPageTitle.Description>
        <NxButtonBar>
          <span>
            {lastUpdated && 'Updated ' + lastUpdated.toLocaleTimeString() + ' ' + lastUpdated.toLocaleDateString()}
          </span>
          <NxButton variant="tertiary" onClick={load} type="button">
            <NxFontAwesomeIcon icon={faSync} />
            <span>Refresh</span>
          </NxButton>
        </NxButtonBar>
      </NxPageTitle>
      <NxTile>
        <NxTile.Content>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell
                  isSortable
                  sortDir={sortColumn === 'provider' ? sortDirection : null}
                  onClick={() => setSort('provider')}
                >
                  <NxTooltip title="The Source Control Management provider the user belongs to." placement="bottom">
                    <span>SCM Provider</span>
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={sortColumn === 'user' ? sortDirection : null}
                  onClick={() => setSort('user')}
                >
                  <NxTooltip
                    title="The ID of the Source Control Management user who owns the associated access tokens."
                    placement="bottom"
                  >
                    <span>SCM User ID</span>
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={sortColumn === 'averageRemainingPercent' ? sortDirection : null}
                  onClick={() => setSort('averageRemainingPercent')}
                >
                  <NxTooltip
                    title="The rate limit remaining percentages for different API categories for the SCM user."
                    placement="bottom"
                  >
                    <span>Rate Limit Remaining</span>
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={sortColumn === 'definingOwners' ? sortDirection : null}
                  onClick={() => setSort('definingOwners')}
                >
                  <NxTooltip
                    title="The organizations and applications that have saved access tokens associated with the SCM user."
                    placement="bottom"
                  >
                    <span>Defining Owners</span>
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={sortColumn === 'associatedApplications' ? sortDirection : null}
                  onClick={() => setSort('associatedApplications')}
                >
                  <NxTooltip
                    title="The applications that are using access tokens associated with the SCM user."
                    placement="bottom"
                  >
                    <span>Associated Applications</span>
                  </NxTooltip>
                </NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body emptyMessage="None" isLoading={loading} error={loadError} retryHandler={load}>
              {!loadError &&
                data &&
                data.userRateLimits &&
                data.userRateLimits.map(
                  ({ provider, user, definingOwners, associatedApplications, averageRemainingPercent, rateLimits }) => (
                    <NxTable.Row key={user}>
                      <NxTable.Cell>{provider}</NxTable.Cell>
                      <NxTable.Cell>{user}</NxTable.Cell>
                      <NxTable.Cell>
                        <NxCollapsibleItems
                          onToggleCollapse={() => toggleUserRateLimitsExpanded(user)}
                          isOpen={!!userRateLimitsExpanded[user]}
                          triggerContent={
                            <>
                              {getRateLimitPercentIcon(averageRemainingPercent)}
                              <span>{round(averageRemainingPercent, 2)}% Average</span>
                            </>
                          }
                        >
                          <NxCollapsibleItems.Child>
                            <div className="source-control-rate-limits-container">
                              {rateLimits.map(({ category, remainingPercent, timeUntilReset }) => (
                                <span key={category} className="source-control-rate-limit-container">
                                  <span>
                                    {getRateLimitPercentIcon(remainingPercent)} {category}
                                  </span>
                                  <span>{round(remainingPercent, 2)}%</span>
                                  <span>{`(resets in ${timeUntilReset})`}</span>
                                </span>
                              ))}
                            </div>
                          </NxCollapsibleItems.Child>
                        </NxCollapsibleItems>
                      </NxTable.Cell>
                      <NxTable.Cell>
                        <NxCollapsibleItems
                          onToggleCollapse={() => toggleUserDefiningOwnersExpanded(user)}
                          isOpen={!!userDefiningOwnersExpanded[user]}
                          triggerContent={`${definingOwners.length} Total`}
                        >
                          {definingOwners.map((definingOwner) => (
                            <NxOverflowTooltip key={definingOwner.ownerId}>
                              <NxCollapsibleItems.Child>
                                <NxTextLink onClick={() => goToOwnerEditSourceControl(definingOwner)}>
                                  {definingOwner.ownerName}
                                </NxTextLink>
                              </NxCollapsibleItems.Child>
                            </NxOverflowTooltip>
                          ))}
                        </NxCollapsibleItems>
                      </NxTable.Cell>
                      <NxTable.Cell>
                        <NxCollapsibleItems
                          onToggleCollapse={() => toggleUserAssociatedApplicationsExpanded(user)}
                          isOpen={!!userAssociatedApplicationsExpanded[user]}
                          triggerContent={`${associatedApplications.length} Total`}
                        >
                          {associatedApplications.map((associatedApplication) => (
                            <NxOverflowTooltip key={associatedApplication.ownerId}>
                              <NxCollapsibleItems.Child>
                                <NxTextLink onClick={() => goToOwnerEditSourceControl(associatedApplication)}>
                                  {associatedApplication.ownerName}
                                </NxTextLink>
                              </NxCollapsibleItems.Child>
                            </NxOverflowTooltip>
                          ))}
                        </NxCollapsibleItems>
                      </NxTable.Cell>
                    </NxTable.Row>
                  )
                )}
            </NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
