/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

const getText = (reachable) => {
  if (reachable == null) {
    return '-';
  }

  if (reachable) {
    return 'Reachable';
  }

  return 'Not Reachable';
};

export default function Reachability({ reachable }) {
  return (
    <span
      className={classnames('iq-reachability', {
        'iq-reachability__reachable': reachable,
      })}
    >
      {getText(reachable)}
    </span>
  );
}

Reachability.propTypes = {
  reachable: PropTypes.bool,
};
