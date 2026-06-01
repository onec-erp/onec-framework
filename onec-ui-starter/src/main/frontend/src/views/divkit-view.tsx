import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { DivKit, type DivKitProps } from "@divkitframework/react";
import { useAuth } from "@/providers/auth-provider";
import { useTheme } from "@/providers/theme-provider";
import "@divkitframework/divkit/dist/client.css";

/**
 * The whole authenticated app: a DivKit canvas. It fetches the server-emitted
 * card for the current route from /api/divkit/* — which already contains the
 * chrome (top bar + nav) plus the surface content, themed and sized for the
 * client — and renders it. The client only routes: onec:// action URLs (a
 * non-builtin protocol, so DivKit hands them to onCustomAction) become
 * navigation, persona switches, and sign-out. viewport + theme are sent to the
 * server so layout and colors are chosen server-side — the same hooks a Flutter
 * client would use.
 */
const MOBILE_BREAKPOINT = 768;

export function DivKitView() {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const [profile, setProfile] = useState<string | null>(null);
  const [mobile, setMobile] = useState(() => window.innerWidth < MOBILE_BREAKPOINT);
  const [card, setCard] = useState<DivKitProps["json"] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const resolvedTheme = useMemo<"light" | "dark">(() => {
    if (theme === "dark" || theme === "light") return theme;
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }, [theme]);

  useEffect(() => {
    const onResize = () => setMobile(window.innerWidth < MOBILE_BREAKPOINT);
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const endpoint = useMemo(() => {
    const path = location.pathname;
    const isHome = path === "/" || path === "";
    const base = isHome ? "/api/divkit/app" : `/api/divkit${path}`;
    const qs = new URLSearchParams();
    if (mobile) qs.set("viewport", "mobile");
    qs.set("theme", resolvedTheme);
    if (isHome && profile) qs.set("profile", profile);
    return `${base}?${qs.toString()}`;
  }, [location.pathname, profile, mobile, resolvedTheme]);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    fetch(endpoint, { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((json) => {
        if (!cancelled) setCard(json as DivKitProps["json"]);
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      });
    return () => {
      cancelled = true;
    };
  }, [endpoint]);

  const onCustomAction = useCallback(
    (action: { url?: string }) => {
      const url = action?.url;
      if (!url || !url.startsWith("onec://")) return;
      const rest = url.slice("onec://".length); // "logout" | "theme/toggle" | "app?profile=x" | "documents/foo/id"
      if (rest === "logout") {
        logout().finally(() => navigate("/login"));
        return;
      }
      if (rest === "theme/toggle") {
        setTheme(resolvedTheme === "dark" ? "light" : "dark");
        return;
      }
      if (rest.startsWith("app")) {
        const q = rest.indexOf("?");
        const params = new URLSearchParams(q >= 0 ? rest.slice(q + 1) : "");
        setProfile(params.get("profile"));
        if (location.pathname !== "/") navigate("/");
        return;
      }
      navigate("/" + rest);
    },
    [navigate, location.pathname, logout, setTheme, resolvedTheme]
  );

  const pageBg = resolvedTheme === "dark" ? "#0A0A0A" : "#FFFFFF";

  if (error) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center text-sm text-destructive"
           style={{ background: pageBg }}>
        Failed to load: {error}
      </div>
    );
  }
  if (!card) {
    return (
      <div className="flex min-h-screen w-full items-center justify-center text-sm text-muted-foreground"
           style={{ background: pageBg }}>
        Loading…
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full overflow-x-hidden" style={{ background: pageBg }}>
      <DivKit
        id={`onec:${resolvedTheme}:${mobile ? "m" : "d"}:${location.pathname}`}
        json={card}
        theme={resolvedTheme}
        onCustomAction={onCustomAction as NonNullable<DivKitProps["onCustomAction"]>}
      />
    </div>
  );
}
