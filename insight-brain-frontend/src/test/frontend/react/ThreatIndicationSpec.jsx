import ThreatIndication from '../../../main/frontend/react/ThreatIndication';
import * as enzymeUtils from '../enzymeUtils';

describe('ThreatIndication', function() {
  const getShallowComponent = enzymeUtils.getShallowComponent(ThreatIndication, { policyThreatLevel: 0 });

  it('renders a span with the iq-threat-indication class', function() {
    expect(getShallowComponent()).toMatchSelector('span.iq-threat-indication');
  });

  it('adds a class for the appropriate threat level indication', function() {
    expect(getShallowComponent()).toMatchSelector('.ignore');
    expect(getShallowComponent({ policyThreatLevel: 1 })).toMatchSelector('.none');
    expect(getShallowComponent({ policyThreatLevel: 3 })).toMatchSelector('.moderate');
    expect(getShallowComponent({ policyThreatLevel: 5 })).toMatchSelector('.severe');
    expect(getShallowComponent({ policyThreatLevel: 9 })).toMatchSelector('.critical');
  });
});
