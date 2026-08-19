/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import classNames from 'classnames';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxLoadWrapper,
  NxButton,
  NxFontAwesomeIcon,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import {
  selectLoadingApplicableCategories,
  selectLoadApplicableCategoriesError,
  selectLoadingAppliedCategories,
  selectLoadAppliedCategoriesError,
  selectSubmitApplyCategoriesError,
  selectIsDirty,
  selectCategories,
  selectAssignAppCategoriesSubmitMaskState,
  selectPreviewAppliedMockIds,
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import {
  selectHasCustomAppCategories,
  selectIsEnterprisePreviewMode,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { IqAssociationEditor, FieldType } from 'MainRoot/react/IqAssociationEditor';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import enterpriseCategories from 'MainRoot/OrgsAndPolicies/shared/enterpriseCategories';
import './_assignAppCategory.scss';

export default function AssignAppCategory() {
  const dispatch = useDispatch();
  const loadingApplied = useSelector(selectLoadingAppliedCategories);
  const loadingApplicable = useSelector(selectLoadingApplicableCategories);
  const loadAppliedError = useSelector(selectLoadAppliedCategoriesError);
  const loadApplicableError = useSelector(selectLoadApplicableCategoriesError);
  const submitError = useSelector(selectSubmitApplyCategoriesError);
  const categories = useSelector(selectCategories);
  const isDirty = useSelector(selectIsDirty);
  const submitMaskState = useSelector(selectAssignAppCategoriesSubmitMaskState);
  const ownerName = useSelector(selectSelectedOwnerName);
  const isEnterprisePreviewMode = useSelector(selectIsEnterprisePreviewMode);
  const previewAppliedMockIds = useSelector(selectPreviewAppliedMockIds);
  const hasCustomAppCategories = useSelector(selectHasCustomAppCategories);
  const isFeatureGated = !hasCustomAppCategories;
  const loading = loadingApplied || loadingApplicable;
  const loadError = loadApplicableError || loadAppliedError;

  const doLoad = () => {
    dispatch(actions.loadApplicableCategories());
  };

  const handleSubmit = () => {
    dispatch(actions.saveAppliedCategories());
  };

  // Real categories go through updateAppliedCategories (source of truth for save).
  // Mock enterprise categories go through togglePreviewMockApplied — visual-only,
  // never flow into appliedCategories and never set isDirty.
  const handleCheckedChange = (category) => {
    if (category?.isEnterpriseMock) {
      dispatch(actions.togglePreviewMockApplied(category.id));
    } else {
      dispatch(actions.updateAppliedCategories(category));
    }
  };

  // Preserve selections across Default↔Custom switches. Both real and mock
  // state live in Redux and survive the toggle with no server reload.
  const handleToggleMode = () => {
    const newMode = !isEnterprisePreviewMode;
    dispatch(productFeaturesActions.setEnterprisePreviewMode(newMode));
  };

  // Reset preview mode on unmount
  useEffect(() => {
    return () => {
      dispatch(productFeaturesActions.setEnterprisePreviewMode(false));
    };
  }, [dispatch]);

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <div id="application-category-editor">
      <div className="iq-assign-app-category__header">
        <NxPageTitle>
          <NxH1>Assign Application Categories</NxH1>
        </NxPageTitle>
        {isFeatureGated && (
          <div className="iq-assign-app-category__mode-switch">
            <NxButton
              variant={!isEnterprisePreviewMode ? 'primary' : 'secondary'}
              onClick={() => !isEnterprisePreviewMode || handleToggleMode()}
              className="iq-assign-app-category__mode-button iq-assign-app-category__mode-button--left"
            >
              Default
            </NxButton>
            <NxTooltip title="Enterprise Feature">
              <NxButton
                variant={isEnterprisePreviewMode ? 'primary' : 'secondary'}
                onClick={() => isEnterprisePreviewMode || handleToggleMode()}
                className="iq-assign-app-category__mode-button iq-assign-app-category__mode-button--right"
              >
                Custom <NxFontAwesomeIcon icon={faLock} />
              </NxButton>
            </NxTooltip>
          </div>
        )}
      </div>
      <NxTile
        className={classNames({
          'iq-banner-flush-top': isEnterprisePreviewMode,
          'iq-enterprise-mode-footer': isEnterprisePreviewMode, // Hide footer only in Custom mode
        })}
      >
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
          <NxStatefulForm
            submitBtnText={isEnterprisePreviewMode ? undefined : 'Update'}
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            validationErrors={!isDirty ? MSG_NO_CHANGES_TO_SAVE : null}
            onSubmit={isEnterprisePreviewMode ? undefined : handleSubmit}
            doLoad={doLoad}
            loadError={loadError}
            submitError={submitError}
            loading={loading}
            additionalFooterBtns={
              isEnterprisePreviewMode ? (
                <NxButton variant="secondary" onClick={() => window.history.back()} type="button">
                  Back
                </NxButton>
              ) : undefined
            }
          >
            {!isEnterprisePreviewMode ? (
              <NxTile.Content>
                <IqAssociationEditor
                  items={categories}
                  label={`Categories assigned to ${ownerName}`}
                  fieldType={FieldType.CheckBox}
                  icon="hexagon"
                  description="name"
                  isRequired
                  selectedParam="isApplied"
                  onChange={handleCheckedChange}
                />
              </NxTile.Content>
            ) : (
              <NxTile.Content>
                <EnterpriseFullWidthBanner
                  title="Custom Categories"
                  description="Target policies more effectively by organizing applications based on risk and environment."
                />
                <IqAssociationEditor
                  items={[
                    ...categories,
                    ...enterpriseCategories.map((c) => ({
                      ...c,
                      isApplied: previewAppliedMockIds.includes(c.id),
                    })),
                  ]}
                  label={`Categories for ${ownerName}`}
                  fieldType={FieldType.CheckBox}
                  icon="hexagon"
                  description="name"
                  isRequired
                  selectedParam="isApplied"
                  onChange={handleCheckedChange}
                />
              </NxTile.Content>
            )}
          </NxStatefulForm>
        </NxLoadWrapper>
      </NxTile>
    </div>
  );
}
