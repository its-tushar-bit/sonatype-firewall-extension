import 'angular';
import 'angular-route';
import 'angular-ui-bootstrap/ui-bootstrap-tpls';

import './cip-index';

import './proprietary.matchers.modal/proprietary.matchers.module';
import './proprietary.matchers.modal/proprietary.matchers.service';
import './proprietary.matchers.modal/proprietary.matchers.modal.controller';
import './proprietary.matchers.modal/proprietary.matchers.modal';

import './cip-loader';
import './cip-component-util';
import './ci-label-tab';
import './ci-policy-violations-tab';
import './ci.policy.violations.service';
import './ci-license-tab';
import './ci-vulnerability-tab';
import './ci-version-graph';

import Base64 from '../lib/Base64';
window.Base64 = Base64;
