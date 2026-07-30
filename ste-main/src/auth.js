const DEVICE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
const TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

async function jsonFetch(url, options = {}) {
  const response = await fetch(url, options);
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.error_description || body.errorMessage || body.message || `Request failed (${response.status})`);
  }
  return body;
}

async function beginMicrosoftLogin(clientId) {
  if (!clientId) throw new Error("Add your Microsoft Azure application ID in Settings first.");
  const body = new URLSearchParams({
    client_id: clientId,
    scope: "XboxLive.signin offline_access"
  });
  return jsonFetch(DEVICE_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body
  });
}

async function pollMicrosoftLogin(clientId, device, onStatus) {
  const started = Date.now();
  const timeout = (device.expires_in || 900) * 1000;
  let interval = (device.interval || 5) * 1000;

  while (Date.now() - started < timeout) {
    await new Promise((resolve) => setTimeout(resolve, interval));
    const body = new URLSearchParams({
      client_id: clientId,
      grant_type: "urn:ietf:params:oauth:grant-type:device_code",
      device_code: device.device_code
    });
    const response = await fetch(TOKEN_URL, {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body
    });
    const token = await response.json();
    if (response.ok) return completeXboxLogin(token);
    if (token.error === "authorization_pending") {
      onStatus?.("Waiting for approval…");
      continue;
    }
    if (token.error === "slow_down") {
      interval += 5000;
      continue;
    }
    throw new Error(token.error_description || "Microsoft sign-in failed.");
  }
  throw new Error("The Microsoft sign-in code expired.");
}

async function completeXboxLogin(msToken) {
  const xbl = await jsonFetch("https://user.auth.xboxlive.com/user/authenticate", {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify({
      Properties: {
        AuthMethod: "RPS",
        SiteName: "user.auth.xboxlive.com",
        RpsTicket: `d=${msToken.access_token}`
      },
      RelyingParty: "http://auth.xboxlive.com",
      TokenType: "JWT"
    })
  });
  const userHash = xbl.DisplayClaims.xui[0].uhs;
  const xsts = await jsonFetch("https://xsts.auth.xboxlive.com/xsts/authorize", {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify({
      Properties: { SandboxId: "RETAIL", UserTokens: [xbl.Token] },
      RelyingParty: "rp://api.minecraftservices.com/",
      TokenType: "JWT"
    })
  });
  const mc = await jsonFetch("https://api.minecraftservices.com/authentication/login_with_xbox", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ identityToken: `XBL3.0 x=${userHash};${xsts.Token}` })
  });
  await jsonFetch("https://api.minecraftservices.com/entitlements/mcstore", {
    headers: { authorization: `Bearer ${mc.access_token}` }
  });
  const profile = await jsonFetch("https://api.minecraftservices.com/minecraft/profile", {
    headers: { authorization: `Bearer ${mc.access_token}` }
  });
  return {
    type: "microsoft",
    username: profile.name,
    uuid: profile.id,
    accessToken: mc.access_token,
    refreshToken: msToken.refresh_token,
    addedAt: new Date().toISOString()
  };
}

module.exports = { beginMicrosoftLogin, pollMicrosoftLogin };
