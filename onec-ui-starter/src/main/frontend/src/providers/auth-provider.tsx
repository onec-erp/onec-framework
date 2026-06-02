import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { ApiError, api } from "@/lib/api";
import type { AuthUser } from "@/lib/types";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const currentUser = await api.getCurrentUser();
      setUser(currentUser.authenticated ? currentUser : null);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setUser(null);
      } else {
        setUser(null);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Keep the server session alive while a tab is open, and bounce to login the moment it
  // isn't. The app's only standing connection is the SSE stream, which is dispatched once
  // and so never bumps the session's last-accessed time — without this a parked tab silently
  // loses its session after the idle timeout and the next action fails with no explanation.
  // A lightweight /api/auth/me poll both refreshes the session and, on a 401, clears the user.
  const HEARTBEAT_MS = 4 * 60 * 1000;
  useEffect(() => {
    if (!user) return;
    const beat = () => {
      if (document.visibilityState === "visible") refresh();
    };
    const interval = window.setInterval(beat, HEARTBEAT_MS);
    // Revalidate immediately on regaining focus (e.g. waking from sleep) so a session that
    // expired while the tab was hidden surfaces as the login screen, not a broken panel.
    document.addEventListener("visibilitychange", beat);
    return () => {
      window.clearInterval(interval);
      document.removeEventListener("visibilitychange", beat);
    };
  }, [user, refresh]);

  const login = useCallback(async (username: string, password: string) => {
    try {
      const currentUser = await api.login(username, password);
      setUser(currentUser);
    } catch (err) {
      setUser(null);
      throw err;
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, logout, refresh }),
    [user, loading, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
