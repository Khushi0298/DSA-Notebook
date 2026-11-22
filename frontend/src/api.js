const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const getToken = () => localStorage.getItem("dsa-token");
export const getEmail = () => localStorage.getItem("dsa-email");

export function setToken(token, email) {
  if (token) {
    localStorage.setItem("dsa-token", token);
    localStorage.setItem("dsa-email", email);
  } else {
    localStorage.removeItem("dsa-token");
    localStorage.removeItem("dsa-email");
  }
}

export async function api(path, { method = "GET", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  const token = getToken();
  if (token) headers.Authorization = "Bearer " + token;

  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  } catch {
    throw new Error("Cannot reach the server. On the free plan it may be waking up, so wait a minute and try again.");
  }
  if (res.status === 401 && token) {
    setToken(null); // session expired
    window.location.reload();
  }
  if (!res.ok) {
    let message = "Something went wrong";
    try { message = (await res.json()).message || message; } catch { /* no body */ }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}
