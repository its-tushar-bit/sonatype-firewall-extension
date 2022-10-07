/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { isEmpty } from 'ramda';
import PropTypes from 'prop-types';
import { NxTextInput, NxFormSelect } from '@sonatype/react-shared-components';

const modifierTypes = [
  { name: 'days', modifier: 1 },
  { name: 'weeks', modifier: 7 },
  { name: 'months', modifier: 30 },
  { name: 'years', modifier: 365 },
];

export default function AgeInDaysInput({ id, onChange, ageInDays }) {
  const [modifier, setModifier] = useState(getInitialModifier(Number(ageInDays.value)));
  const age = formatDaysToAge(ageInDays.value);

  function getInitialModifier(days) {
    return days ? (days % 365 === 0 ? 365 : days % 30 === 0 ? 30 : days % 7 === 0 ? 7 : 1) : 365;
  }

  function formatDaysToAge(days) {
    return days ? parseInt(days) / modifier : days;
  }

  function parseAgeToDays(age, modifier) {
    return Number.isInteger(age) ? (parseInt(age) * modifier).toString() : '';
  }

  function onModifierChange(e) {
    const { value: newModifier } = e.currentTarget;

    setModifier(parseInt(newModifier));
    onChange(parseAgeToDays(age, newModifier));
  }

  function onAgeChange(value) {
    onChange(parseAgeToDays(isEmpty(value) ? '' : Number(value), modifier));
  }

  return (
    <>
      <NxTextInput
        id={id}
        className="constraint-editor__age-input constraint-editor__150-width"
        isPristine={ageInDays.isPristine}
        trimmedValue={ageInDays.trimmedValue}
        validationErrors={ageInDays.validationErrors}
        value={String(age)}
        onChange={onAgeChange}
        aria-required={true}
        placeholder="Age"
        validatable
      />
      <NxFormSelect
        aria-label="Age modifier"
        onChange={onModifierChange}
        value={modifier}
        className="constraint-editor__age-modifier constraint-editor__150-width"
      >
        {modifierTypes.map((type) => (
          <option key={type.modifier} value={type.modifier}>
            {type.name}
          </option>
        ))}
      </NxFormSelect>
    </>
  );
}

AgeInDaysInput.propTypes = {
  id: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  ageInDays: PropTypes.shape({
    value: PropTypes.string,
    trimmedValue: PropTypes.string,
    isPristine: PropTypes.bool,
  }),
};
