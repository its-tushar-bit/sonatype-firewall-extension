/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import {
  NxCheckbox,
  NxDivider,
  NxFieldset,
  NxFormGroup,
  NxFormRow,
  NxH2,
  NxStatefulInfoAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectCurrentPolicyName,
  selectCurrentPolicyThreatLevel,
  selectIsInherited,
  selectCurrentPolicyViolationGrandfatheringAllowed,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import { selectIsGrandfatheringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function EditPolicySummary() {
  const dispatch = useDispatch();

  const name = useSelector(selectCurrentPolicyName);

  const threatLevel = useSelector(selectCurrentPolicyThreatLevel);
  const readOnly = useSelector(selectIsInherited);
  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const policyViolationGrandfatheringAllowed = useSelector(selectCurrentPolicyViolationGrandfatheringAllowed);

  const setPolicyName = (val) => dispatch(actions.setPolicyName(val));

  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));

  const togglePolicyViolationGrandfatheringAllowed = (val) =>
    dispatch(actions.togglePolicyViolationGrandfatheringAllowed(val));

  return (
    <div id="policy-edit-summary">
      <NxH2>Summary</NxH2>
      <NxFormRow>
        <NxFormGroup label="Policy Name" isRequired={true}>
          <NxTextInput
            {...name}
            onChange={setPolicyName}
            validatable={true}
            disabled={readOnly}
            id="editor-policy-name"
            name="policy"
            autoFocus
          />
        </NxFormGroup>
        <ThreatDropdownSelector
          className="edit-policy-threat-dropdown"
          threatLevel={threatLevel}
          onSelectThreatLevel={setThreatLevel}
          disabled={readOnly}
          id="editor-policy-threat-level"
        />
      </NxFormRow>
      {!isGrandfatheringSupported && (
        <NxStatefulInfoAlert id="grandfathering-disabled-message">
          Policy Violation Grandfathering is not supported by your license
        </NxStatefulInfoAlert>
      )}
      <NxFieldset label="Policy Violation Grandfathering" isRequired={true}>
        <NxCheckbox
          id="editor-policy-violation-grandfathering"
          onChange={togglePolicyViolationGrandfatheringAllowed}
          isChecked={!!policyViolationGrandfatheringAllowed}
          disabled={readOnly || !isGrandfatheringSupported}
        >
          Allow this policy to be grandfathered
        </NxCheckbox>
      </NxFieldset>
      <NxDivider />
    </div>
  );
}
