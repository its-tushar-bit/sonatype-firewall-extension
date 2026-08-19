/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { isNil } from 'ramda';
import { faTrashAlt, faPen } from '@fortawesome/free-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon, NxList } from '@sonatype/react-shared-components';
import { conditionString } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import ConditionIsNotSupportedError from './ConditionIsNotSupportedError';

export default function ReadOnlyConstraint({
  constraint,
  constraintIdx,
  cannotBeRemoved,
  readOnly,
  conditionTypesMap,
  updateEditConstraintId,
  deleteConstraint,
}) {
  const constraintSubheader =
    constraint.conditions.length > 1
      ? `${constraint.operator === 'OR' ? 'any' : 'all'} of the following are true:`
      : 'the following is true:';

  return (
    <NxList.Item key={constraint.id} className="constraint-editor__item" data-testid="read-only-constraint">
      <NxList.Text>{constraint.name.value}</NxList.Text>
      <NxList.Actions>
        <NxButton
          variant="icon-only"
          title={readOnly ? '' : 'Edit constraint'}
          aria-label="Edit constraint"
          type="button"
          disabled={readOnly}
          className="constraint-editor__edit-btn"
          onClick={() => updateEditConstraintId(constraint.id)}
        >
          <NxFontAwesomeIcon icon={faPen} />
        </NxButton>
        <NxButton
          variant="icon-only"
          title={cannotBeRemoved ? '' : 'Delete constraint'}
          aria-label="Delete constraint"
          type="button"
          disabled={cannotBeRemoved}
          className="constraint-editor__delete-btn"
          onClick={() => deleteConstraint(constraintIdx)}
        >
          <NxFontAwesomeIcon icon={faTrashAlt} />
        </NxButton>
      </NxList.Actions>
      <NxList.Subtext>
        <ConditionIsNotSupportedError constraint={constraint} conditionTypesMap={conditionTypesMap} />
        is in violation if {constraintSubheader}
        <NxList bulleted>
          {!isNil(conditionTypesMap) &&
            constraint.conditions.map((condition) => (
              <NxList.Item key={condition.conditionTypeId}>{conditionString(condition, conditionTypesMap)}</NxList.Item>
            ))}
        </NxList>
      </NxList.Subtext>
    </NxList.Item>
  );
}

ReadOnlyConstraint.propTypes = {
  constraint: PropTypes.object,
  constraintIdx: PropTypes.number,
  cannotBeRemoved: PropTypes.bool,
  readOnly: PropTypes.bool,
  conditionTypesMap: PropTypes.object,
  updateEditConstraintId: PropTypes.func,
  deleteConstraint: PropTypes.func,
};
