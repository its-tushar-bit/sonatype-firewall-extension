/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxStatefulForm, NxPageTitle, NxH1, NxTile, NxLoadWrapper } from '@sonatype/react-shared-components';
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
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { IqAssociationEditor, FieldType } from 'MainRoot/react/IqAssociationEditor';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

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
  const loading = loadingApplied || loadingApplicable;
  const loadError = loadApplicableError || loadAppliedError;

  const doLoad = () => {
    dispatch(actions.loadApplicableCategories());
  };

  const handleSubmit = () => {
    dispatch(actions.saveAppliedCategories());
  };

  const handleCheckedChange = (category) => {
    dispatch(actions.updateAppliedCategories(category));
  };

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <div id="application-category-editor">
      <NxPageTitle>
        <NxH1>Assign Application Categories</NxH1>
      </NxPageTitle>
      <NxTile>
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
          <NxStatefulForm
            submitBtnText="Update"
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            validationErrors={!isDirty ? MSG_NO_CHANGES_TO_SAVE : null}
            onSubmit={handleSubmit}
            doLoad={doLoad}
            loadError={loadError}
            submitError={submitError}
            loading={loading}
          >
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
          </NxStatefulForm>
        </NxLoadWrapper>
      </NxTile>
    </div>
  );
}
