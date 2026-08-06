import React, { useEffect, useState } from "react";
import { ExtensionSettings, RuntimeMessage, RuntimeResponse, DEFAULT_SETTINGS } from "../types";

export function Options() {
  const [settings, setSettings] = useState<ExtensionSettings>(DEFAULT_SETTINGS);
  const [savedAt, setSavedAt] = useState<number | null>(null);
  const [testResult, setTestResult] = useState<string>("");

  useEffect(() => {
    chrome.runtime
      .sendMessage<RuntimeMessage, RuntimeResponse>({ type: "GET_SETTINGS" })
      .then((r) => {
        if (r.ok && "settings" in r) setSettings(r.settings);
      });
  }, []);

  async function save() {
    await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
      type: "SET_SETTINGS",
      settings,
    });
    setSavedAt(Date.now());
  }

  async function testConnection() {
    setTestResult("Testing…");
    try {
      const res = await fetch(`${settings.iqServerUrl.replace(/\/$/, "")}/health`);
      if (res.ok) {
        const j = await res.json();
        setTestResult(`OK · ${j.service || "iq-server"}`);
      } else {
        setTestResult(`HTTP ${res.status}`);
      }
    } catch (e: any) {
      setTestResult(`Failed: ${e.message}`);
    }
  }

  function update<K extends keyof ExtensionSettings>(k: K, v: ExtensionSettings[K]) {
    setSettings({ ...settings, [k]: v });
  }

  return (
    <div className="max-w-2xl mx-auto p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-semibold flex items-center gap-2">
          <span className="w-3 h-3 rounded-full bg-sonatype-blue" />
          Sonatype Firewall — Settings
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Configure where the extension fetches verdicts from and which sites it activates on.
        </p>
      </header>

      <section className="bg-white rounded shadow-sm p-5 mb-4">
        <h2 className="font-semibold mb-3">Mock IQ Server</h2>
        <p className="text-xs text-gray-500 mb-3">
          The extension talks to a local mock that mimics IQ Server. Start it with{" "}
          <code className="bg-gray-100 px-1">npm run mock</code>.
        </p>

        <Field
          label="IQ Server URL"
          value={settings.iqServerUrl}
          onChange={(v) => update("iqServerUrl", v)}
          placeholder="http://localhost:8765"
        />
        <Field
          label="User Code"
          value={settings.userCode}
          onChange={(v) => update("userCode", v)}
          placeholder="demo-user"
        />
        <Field
          label="Pass Code"
          value={settings.passCode}
          onChange={(v) => update("passCode", v)}
          type="password"
          placeholder="demo-pass"
        />

        <div className="mt-3 flex items-center gap-3">
          <button
            onClick={testConnection}
            className="px-3 py-1.5 text-sm bg-gray-200 rounded hover:bg-gray-300"
          >
            Test connection
          </button>
          {testResult && <span className="text-sm text-gray-700">{testResult}</span>}
        </div>
      </section>

      <section className="bg-white rounded shadow-sm p-5 mb-4">
        <h2 className="font-semibold mb-3">Nexus Repository Proxy</h2>
        <Field
          label="Nexus base URL"
          value={settings.nexusProxyUrl}
          onChange={(v) => update("nexusProxyUrl", v)}
          placeholder="https://nexus.acme.com/repository"
        />
        <label className="flex items-center gap-2 mt-2">
          <input
            type="checkbox"
            checked={settings.rewriteInstallCommands}
            onChange={(e) => update("rewriteInstallCommands", e.target.checked)}
          />
          <span className="text-sm">
            Rewrite <code className="bg-gray-100 px-1">npm i</code> /{" "}
            <code className="bg-gray-100 px-1">pip install</code> snippets to use this proxy
          </span>
        </label>
      </section>

      <section className="bg-white rounded shadow-sm p-5 mb-4">
        <h2 className="font-semibold mb-3">Enabled Sites</h2>
        {(["npm", "pypi", "maven"] as const).map((site) => (
          <label key={site} className="flex items-center gap-2 mb-1">
            <input
              type="checkbox"
              checked={settings.enabledSites[site]}
              onChange={(e) =>
                update("enabledSites", { ...settings.enabledSites, [site]: e.target.checked })
              }
            />
            <span className="text-sm">{site}</span>
          </label>
        ))}
      </section>

      <div className="flex items-center gap-3">
        <button
          onClick={save}
          className="px-4 py-2 bg-sonatype-blue text-white rounded hover:bg-sonatype-dark"
        >
          Save settings
        </button>
        {savedAt && (
          <span className="text-sm text-gray-600">
            Saved at {new Date(savedAt).toLocaleTimeString()}
          </span>
        )}
      </div>
    </div>
  );
}

function Field(props: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <label className="block mb-2">
      <span className="text-sm font-medium">{props.label}</span>
      <input
        type={props.type || "text"}
        value={props.value}
        placeholder={props.placeholder}
        onChange={(e) => props.onChange(e.target.value)}
        className="mt-1 block w-full rounded border border-gray-300 px-3 py-1.5 text-sm font-mono"
      />
    </label>
  );
}
