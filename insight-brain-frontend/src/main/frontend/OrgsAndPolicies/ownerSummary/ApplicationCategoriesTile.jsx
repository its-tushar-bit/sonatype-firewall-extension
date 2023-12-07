/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import {
  NxButton,
  NxCollapsibleItems,
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
import { selectEntityId, selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsApplication, selectIsOrganization, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';

import { actions as assignApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import { actions as createEditApplicationCategoriesActions } from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSlice';
import { curryN, isEmpty } from 'ramda';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectHasEditIqPermission } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { selectIsOrgsAndAppsEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function ApplicationCategoriesTile() {
  const dispatch = useDispatch();
  const uiStateRouter = useRouterState();

  const ownerName = useSelector(selectSelectedOwnerName);

  const loadingApplicableCategories = useSelector(selectLoadingApplicableCategories);
  const isLoading = useSelector(selectIsLoading);
  const loadingAppliedCategories = useSelector(selectLoadingAppliedCategories);

  const loadApplicableCategoriesError = useSelector(selectLoadApplicableCategoriesError);
  const loadError = useSelector(selectLoadError);
  const loadAppliedCategoriesError = useSelector(selectLoadAppliedCategoriesError);

  const loading = loadingApplicableCategories || isLoading || loadingAppliedCategories;
  const error = loadApplicableCategoriesError || loadError || loadAppliedCategoriesError;

  const appliedCategories = useSelector(selectAppliedCategories);
  const areAnyCategoriesDefined = useSelector(selectAreAnyCategoriesDefined);
  const appCategoryOwners = useSelector(selectAppCategoryOwners);

  const isApp = useSelector(selectIsApplication);
  const isOrg = useSelector(selectIsOrganization);

  const entityId = useSelector(selectEntityId);

  const router = useSelector(selectRouterSlice);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);

  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);

  const loadAssignableCategories = () => dispatch(assignApplicationCategoriesActions.loadApplicableCategories());
  const loadAppliedCategories = () => dispatch(assignApplicationCategoriesActions.loadAppliedCategories());
  const goToAssignCategories = () => dispatch(assignApplicationCategoriesActions.goToEditCategories());

  const loadApplicableCategories = () => dispatch(createEditApplicationCategoriesActions.loadApplicableCategories());
  const editCategoryHref = (categoryId) => {
    const { to, params } = deriveEditRoute(router, 'category', { categoryId });

    return uiStateRouter.href(to, params);
  };
  const goToCreateCategory = () => dispatch(createEditApplicationCategoriesActions.goToCreateCategory());
  const [inheritedCategoriesExpandedStatus, toggleInheritedCategoriesExpandedStatus] = useState({});

  const onToggleCollapseItem = (index) => {
    toggleInheritedCategoriesExpandedStatus((prevState) => {
      const currentStatus = prevState.hasOwnProperty(index) ? prevState[index] : true;
      return {
        ...prevState,
        [index]: !currentStatus,
      };
    });
  };

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

  const renderHexagon = (category) => (
    <Hexagon
      className={
        angularToRscColorMap[category.color] ? `nx-selectable-color--${angularToRscColorMap[category.color]}` : ''
      }
    />
  );

  const renderCategoriesIcons = (categories) => {
    return <span>{categories.map((category) => renderHexagon(category))}</span>;
  };

  const renderListItem = curryN(2, (isLink, category) => {
    const ListItem = isLink ? NxList.LinkItem : NxList.Item;
    return (
      <ListItem key={category.id} href={isLink ? editCategoryHref(category.id) : undefined}>
        <NxList.Text>
          {renderHexagon(category)}
          <span>{category.name}</span>
        </NxList.Text>
        {category.description && <NxList.Subtext>{category.description}</NxList.Subtext>}
      </ListItem>
    );
  });

  const renderList = (owner, appliedCategories, title, emptyMessage, isLink, index) => {
    const categories = owner?.applicationCategories;
    if (isEmpty(categories) && !isLink && !isApp) return;
    return isLink
      ? renderSimpleList(categories, title, emptyMessage, isLink)
      : renderCollapsibleList(owner, title, index);
  };

  const renderCollapsibleList = (owner, title, index) => {
    const categories = owner?.applicationCategories;
    const categoriesIcons = renderCategoriesIcons(categories);
    const isExpanded = inheritedCategoriesExpandedStatus[index] ?? true;
    return (
      <NxTile.Subsection id={owner?.ownerId}>
        <NxCollapsibleItems
          onToggleCollapse={() => onToggleCollapseItem(index)}
          isOpen={isExpanded}
          triggerContent={
            <>
              <NxH3>{title}</NxH3>
              {!isExpanded && categoriesIcons}
            </>
          }
        >
          <dl id={'application-categories-for-' + owner?.ownerId}>{categories.map(renderCollapsibleListItem)}</dl>
        </NxCollapsibleItems>
      </NxTile.Subsection>
    );
  };

  const renderSimpleList = (categories, title, emptyMessage, isLink) => {
    const items = categories.map(renderListItem(isLink && hasEditIqPermission));
    if (isEmpty(categories) && !isLink && !isApp) return;
    return (
      <>
        <NxTile.Subsection>
          <NxTile.SubsectionHeader>
            <NxH3>{title}</NxH3>
          </NxTile.SubsectionHeader>
          <NxList emptyMessage={emptyMessage}>{items}</NxList>
        </NxTile.Subsection>
      </>
    );
  };

  const renderContent = () => {
    if (isApp) {
      return renderSimpleList(
        appliedCategories,
        'Assigned',
        `No application categories ${areAnyCategoriesDefined ? 'assigned' : 'defined'}`,
        false
      );
    }
    return appCategoryOwners.map((owner, index) => (
      <Fragment key={owner.ownerId}>
        {renderList(
          owner,
          [],
          owner.parent ? `Inherited from ${owner.ownerName}` : `Local to ${owner.ownerName}`,
          'No application categories defined',
          !owner.parent,
          index
        )}
      </Fragment>
    ));
  };

  const renderCollapsibleListItem = (category) => {
    return (
      <NxCollapsibleItems.Child key={category.id}>
        <div className="categories-element">
          <dt>
            {renderHexagon(category)}
            <span className="categories-label">{category.name}</span>
          </dt>
          <dd className="categories-description">{category.description}</dd>
        </div>
      </NxCollapsibleItems.Child>
    );
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
  }, [entityId]);

  return (
    isOrgsAndAppsEnabled && (
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
    )
  );
}
