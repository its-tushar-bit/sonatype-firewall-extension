/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

export default class PollingContainer extends React.Component {
  componentDidMount() {
    this.props.pollingAction();
    this.dataPolling = setInterval(() => this.props.pollingAction(), this.props.interval);
  }

  componentWillUnmount() {
    clearInterval(this.dataPolling);
  }

  render() {
    return null;
  }
}

PollingContainer.propTypes = {
  pollingAction: PropTypes.func.isRequired,
  interval: PropTypes.number
};

PollingContainer.defaultProps = {
  interval: 2000
};
