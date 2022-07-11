/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxH3,
  NxList,
  NxLoadWrapper,
  NxTile,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';

import Hexagon from 'MainRoot/react/Hexagon';
import { angularToRscColorMap, deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';

import { selectLoadApplicationsError, selectLoadingApplications } from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import {
  selectAppliedCategories,
  selectAreAnyCategoriesDefined,
  selectLoadApplicableCategoriesError,
  selectLoadAppliedCategoriesError,
  selectLoadingApplicableCategories,
  selectLoadingAppliedCategories,
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import {
  selectLoadError,
  selectIsLoading,
  selectAppCategoryOwners,
} from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsOrganization, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';

import { actions as assignApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as createEditApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { curryN } from 'ramda';
import { selectSelectedOwner } from '../orgsAndPoliciesSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function ApplicationCategoriesTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const ownerName = useSelector(selectSelectedOwnerName);

  const loadingApplications = useSelector(selectLoadingApplications);
  const loadingApplicableCategories = useSelector(selectLoadingApplicableCategories);
  const isLoading = useSelector(selectIsLoading);
  const loadingAppliedCategories = useSelector(selectLoadingAppliedCategories);

  const loadApplicationsError = useSelector(selectLoadApplicationsError);
  const loadApplicableCategoriesError = useSelector(selectLoadApplicableCategoriesError);
  const loadError = useSelector(selectLoadError);
  const loadAppliedCategoriesError = useSelector(selectLoadAppliedCategoriesError);

  const loading = loadingApplications || loadingApplicableCategories || isLoading || loadingAppliedCategories;
  const error = loadApplicationsError || loadApplicableCategoriesError || loadError || loadAppliedCategoriesError;

  const appliedCategories = useSelector(selectAppliedCategories);
  const areAnyCategoriesDefined = useSelector(selectAreAnyCategoriesDefined);
  const appCategoryOwners = useSelector(selectAppCategoryOwners);

  const isApp = useSelector(selectIsApplication);
  const isOrg = useSelector(selectIsOrganization);

  const selectedOwner = useSelector(selectSelectedOwner);

  const router = useSelector(selectRouterSlice);

  const loadAssignableCategories = () => dispatch(assignApplicationCategoriesActions.loadApplicableCategories());
  const loadAppliedCategories = () => dispatch(assignApplicationCategoriesActions.loadAppliedCategories());
  const goToAssignCategories = () => dispatch(assignApplicationCategoriesActions.goToEditCategories());

  const loadApplicableCategories = () => dispatch(createEditApplicationCategoriesActions.loadApplicableCategories());
  const editCategoryHref = (categoryId) => {
    const { to, params } = deriveEditRoute(router, 'category', { categoryId });

    return uiStateRouter.href(to, params);
  };
  const goToCreateCategory = () => dispatch(createEditApplicationCategoriesActions.goToCreateCategory());

  const headerButtonAction = () => {
    if (isApp && areAnyCategoriesDefined) goToAssignCategories();
    if (isOrg) goToCreateCategory();
  };

  const loadData = () => {
    if (isApp) {
      loadAssignableCategories();
      loadAppliedCategories();
    } else if (isOrg) loadApplicableCategories();
  };

  const renderListItem = curryN(2, (isLink, category) => {
    const ListItem = isLink ? NxList.LinkItem : NxList.Item;
    return (
      <ListItem key={category.id} href={isLink ? editCategoryHref(category.id) : undefined}>
        <NxList.Text>
          <Hexagon
            className={
              angularToRscColorMap[category.color] ? `nx-selectable-color--${angularToRscColorMap[category.color]}` : ''
            }
          />
          <span>{category.name}</span>
        </NxList.Text>
        {category.description && <NxList.Subtext>{category.description}</NxList.Subtext>}
      </ListItem>
    );
  });

  const renderList = (categories, title, emptyMessage, isLink) => {
    const items = categories.map(renderListItem(isLink));
    return (
      <NxTile.Subsection>
        <NxTile.SubsectionHeader>
          <NxH3>{title}</NxH3>
        </NxTile.SubsectionHeader>
        <NxList emptyMessage={emptyMessage}>{items}</NxList>
      </NxTile.Subsection>
    );
  };

  const renderContent = () => {
    if (isApp) {
      return renderList(
        appliedCategories,
        'Assigned',
        `No application categories ${areAnyCategoriesDefined ? 'assigned' : 'defined'}`,
        false
      );
    }
    return appCategoryOwners.map((owner) => (
      <Fragment key={owner.ownerId}>
        {renderList(
          owner.applicationCategories,
          owner.parent ? `Inherited from ${owner.ownerName}` : 'Local',
          'No application categories defined',
          !owner.parent
        )}
      </Fragment>
    ));
  };

  const editButtonText = isApp ? (
    <>
      <NxFontAwesomeIcon icon={faPen} />
      <span>Assign a Category</span>
    </>
  ) : (
    <>
      <NxFontAwesomeIcon icon={faPlus} />
      <span>Add a Category</span>
    </>
  );

  const subtitleText = `${isApp ? 'assigned to' : 'available to apps in'} ${ownerName}`;

  const isEditDisabled = isApp && !areAnyCategoriesDefined;

  useEffect(() => {
    loadData();
  }, [selectedOwner]);

  return (
    <NxTile id="owner-pill-app-categories">
      <NxLoadWrapper loading={loading} retryHandler={loadData} error={error}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Application Categories</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>{subtitleText}</NxTile.HeaderSubtitle>
          </NxTile.Headings>
          <NxTile.HeaderActions>
            <NxButton
              variant="tertiary"
              onClick={headerButtonAction}
              className={isEditDisabled ? 'disabled' : ''}
              id="add-category-button"
              title={isEditDisabled ? 'No application categories defined.' : ''}
            >
              {editButtonText}
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>{renderContent()}</NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
