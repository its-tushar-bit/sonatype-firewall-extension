import React from 'react';
import * as PropTypes from 'prop-types';
import { NxBackButton } from '@sonatype/react-shared-components';

export default function BackButton({ stateName, text, $state }) {
  const state = $state.get(stateName);

  return <NxBackButton href={$state.href(state)} text={text} targetPageTitle={state.data.title} />;
}

BackButton.propTypes = {
  stateName: PropTypes.string.isRequired,
  text: PropTypes.string,
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired
  }).isRequired
};
