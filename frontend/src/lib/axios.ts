import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the access token to every outgoing request.
api.interceptors.request.use(
  (config) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("accessToken");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = typeof window !== "undefined" ? localStorage.getItem("refreshToken") : null;
  if (!refreshToken) return null;

  try {
    const response = await axios.post(`${API_URL}/api/auth/refresh`, { refreshToken });
    const { accessToken, refreshToken: newRefreshToken } = response.data;
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", newRefreshToken);
    return accessToken;
  } catch {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    return null;
  }
}

// On a 401, try exactly once to refresh the access token and replay the request.
// If that fails, clear the session so the app can redirect to /login.
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryableConfig | undefined;

    if (
      error.response?.status === 401 &&
      config &&
      !config._retry &&
      !config.url?.includes("/api/auth/")
    ) {
      config._retry = true;

      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }

      const newToken = await refreshPromise;

      if (newToken) {
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${newToken}`;
        return api(config);
      }

      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event("spendwise-session-expired"));
      }
    }

    return Promise.reject(error);
  }
);
