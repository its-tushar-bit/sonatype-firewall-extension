async function p(t){const n={type:"GET_VERDICT",purl:t},s=await chrome.runtime.sendMessage(n);return!s.ok||!("verdict"in s)?null:s.verdict}async function c(){const t=await chrome.runtime.sendMessage({type:"GET_SETTINGS"});return t.ok&&"settings"in t?t.settings:null}function r(t){switch(t){case"block":return{bg:"#D92D20",fg:"#fff",label:"BLOCKED BY FIREWALL"};case"quarantine":return{bg:"#F79009",fg:"#1a1a1a",label:"QUARANTINED"};case"warn":return{bg:"#F79009",fg:"#1a1a1a",label:"WARN"};case"allow":return{bg:"#12B76A",fg:"#fff",label:"ALLOWED"}}}function d(t,n){const s=t.querySelector("#sonatype-firewall-badge");s&&s.remove();const l=r(n.policy.verdict),e=document.createElement("div");e.id="sonatype-firewall-badge",e.style.cssText=`
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    margin: 12px 0;
    border-radius: 6px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    font-size: 13px;
    font-weight: 600;
    background: ${l.bg};
    color: ${l.fg};
    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
  `;const a=document.createElement("span");a.style.cssText=`
    width: 8px; height: 8px; border-radius: 50%;
    background: ${l.fg}; opacity: 0.85;
  `,e.appendChild(a);const o=document.createElement("span");if(o.textContent=`Sonatype Firewall: ${l.label}`,e.appendChild(o),n.policy.policyName){const i=document.createElement("span");i.style.cssText="opacity: 0.85; font-weight: 400;",i.textContent=`· ${n.policy.policyName} @ ${n.policy.stage}`,e.appendChild(i)}if(n.component.goldenVersion){const i=document.createElement("span");i.style.cssText=`
      margin-left: 8px; padding: 3px 8px;
      background: rgba(255,255,255,0.2); border-radius: 4px;
      font-weight: 500;
    `,i.textContent=`★ Golden Version: ${n.component.goldenVersion.version}`,e.appendChild(i)}return t.prepend(e),e}function g(t,n,s){document.querySelectorAll(t).forEach(e=>{const a=e.textContent||"";let o=a;s==="npm"&&/^\s*npm\s+i(nstall)?\s+/.test(a)?o=a.replace(/^(\s*npm\s+i(nstall)?\s+)/,`$1--registry=${n}/npm-proxy/ `):s==="pypi"&&/^\s*pip\s+install\s+/.test(a)&&(o=a.replace(/^(\s*pip\s+install\s+)/,`$1-i ${n}/pypi-proxy/simple `)),o!==a&&(e.textContent=o,e.title="Rewritten by Sonatype Firewall extension to use your Nexus proxy",e.style.outline="1px dashed #1F65BF")})}export{g as a,c as g,d as i,p as r};
