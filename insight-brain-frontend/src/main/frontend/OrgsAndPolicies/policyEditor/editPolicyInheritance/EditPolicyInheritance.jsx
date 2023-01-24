/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxCheckbox, NxDivider, NxFieldset, NxH2, NxRadio } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { propEq } from 'ramda';

import {
  selectCategories,
  selectHasPolicyCategories,
  selectIsRootOrg,
  selectIsInherited,
  selectCurrentPolicyOwnerName,
  selectCurrentPolicy,
  selectHasEditIqPermission,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { IqAssociationEditor, FieldType } from 'MainRoot/react/IqAssociationEditor';

export default function EditPolicyInheritance() {
  const dispatch = useDispatch();

  const isRootOrg = useSelector(selectIsRootOrg);
  const hasPolicyCategories = useSelector(selectHasPolicyCategories);
  const ownerName = useSelector(selectCurrentPolicyOwnerName);
  const categories = useSelector(selectCategories);
  const currentPolicy = useSelector(selectCurrentPolicy);
  const isInherited = useSelector(selectIsInherited);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const hasCategories = categories?.length;

  const onCategoryToggled = (category) => {
    const categoryIndexForToggle = categories.findIndex(propEq('id', category.id));
    dispatch(policyActions.toggleCategoryIsApplied(categoryIndexForToggle));
  };
  const onHasCategoriesChange = (hasCategories) => dispatch(policyActions.setHasPolicyCategories(!!hasCategories));
  const togglePolicyActionsOverrideAllowed = () => dispatch(policyActions.togglePolicyActionsOverrideAllowed());

  return (
    <div id="policy-edit-inheritance">
      <NxH2>Inheritance</NxH2>

      <NxFieldset id="editor-policy-inherit" label="This Policy Inherits to:" isRequired={true}>
        <NxRadio
          name="hasCategories"
          value={null}
          disabled={isInherited}
          isChecked={!hasPolicyCategories}
          onChange={onHasCategoriesChange}
        >
          All Applications {isRootOrg ? 'and Repositories' : `in ${ownerName}`}
        </NxRadio>

        <NxRadio
          name="hasCategories"
          value={'hasCategories'}
          disabled={!hasCategories || isInherited}
          isChecked={hasPolicyCategories}
          onChange={onHasCategoriesChange}
        >
          Applications of the specified Application Categories in {ownerName}
        </NxRadio>

        {hasPolicyCategories && (
          <IqAssociationEditor
            label="Application Categories:"
            items={categories}
            selectedParam="isApplied"
            description="name"
            icon="hexagon"
            disabled={isInherited}
            onChange={onCategoryToggled}
            fieldType={FieldType.CheckBox}
          />
        )}
      </NxFieldset>

      <NxFieldset id="editor-policy-actions-override-fieldset" label="Policy Actions Override">
        <NxCheckbox
          id="editor-policy-actions-override"
          isChecked={!!currentPolicy.policyActionsOverrideAllowed}
          disabled={isInherited || !hasEditIqPermission}
          onChange={togglePolicyActionsOverrideAllowed}
        >
          Allow action overrides at organization and application levels
        </NxCheckbox>
      </NxFieldset>

      <NxDivider />
    </div>
  );
}
