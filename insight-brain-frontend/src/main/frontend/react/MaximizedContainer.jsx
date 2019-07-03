import React from 'react';
import $ from 'jquery';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

import { maximizeHeightServiceInstance } from '../util/AngularCommon';

export default class MaximizedContainer extends React.Component {
  constructor(props) {
    super(props);

    this.ref = React.createRef();
  }

  componentDidMount() {
    this.teardownMaximizeHeightListener = maximizeHeightServiceInstance.maximizeHeight($(this.ref.current));
  }

  componentWillUnmount() {
    if (this.teardownMaximizeHeightListener) {
      this.teardownMaximizeHeightListener();
    }
  }

  render() {
    return (
      <div { ...this.props } className={classnames(this.props.className, 'maximized-container')} ref={this.ref}/>
    );
  }
}

MaximizedContainer.propTypes = {
  className: PropTypes.string
};
