export default function getPolicyThreatIndicatorLevel(policyThreatLevel) {
  if (policyThreatLevel >= 8) {
    return 'critical';
  }
  else if (policyThreatLevel >= 4) {
    return 'severe';
  }
  else if (policyThreatLevel >= 2) {
    return 'moderate';
  }
  else if (policyThreatLevel === 1) {
    return 'none';
  }
  else {
    return 'ignore';
  }
}
