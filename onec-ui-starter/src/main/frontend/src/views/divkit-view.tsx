import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { DivKit, type DivKitProps } from "@divkitframework/react";
import { useAuth } from "@/providers/auth-provider";
import "@divkitframework/divkit/dist/client.css";

/**
 * The whole authenticated app: a full-screen DivKit canvas. It fetches the
 * server-emitted card for the current route from /api/divkit/* — which already
 * contains the responsive chrome (top bar, sidebar on desktop / bottom nav on
 * mobile) plus the surface content — and renders it. The client only routes:
 * onec:// action URLs (a non-builtin protocol, so DivKit hands them to
 * onCustomAction) become navigation, persona switches, and sign-out.
 *
 * Viewport is sent to the server (?viewport=mobile) so the layout is chosen
 * server-side — the same mechanism a Flutter client would use.
 */
const MOBILE_BREAKPOINT = 768;

export function DivKitView() {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [profile, setProfile] = useState<string | null>(null);
  const [mobile, setMobile] = useState(() => window.innerWidth < MOBILE_BREAKPOINT);
  const [card, setCard] = useState<DivKitProps["json"] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const onResize = () => setMobile(window.innerWidth < MOBILE_BREAKPOINT);
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  const endpoint = useMemo(() => {
    const p = location.pathname;
    const isHome = p === "/" || p === "";
    const base = isHome ? "/api/divkit/app" : `/api/divkit${p}`;
    const qs = new URLSearchParams();
    if (mobile) qs.set("viewport", "mobile");
    if (isHome && profile) qs.set("profile", profile);
    const q = qs.toString();
    return q ? `${base}?${q}` : base;
  }, [location.pathname, profile, mobile]);

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
      const rest = url.slice("onec://".length); // "logout" | "app?profile=x" | "documents/foo/id"
      if (rest === "logout") {
        logout().finally(() => navigate("/login"));
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
    [navigate, location.pathname, logout]
  );

  if (error) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-background text-sm text-destructive">
        Failed to load: {error}
      </div>
    );
  }
  if (!card) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Loading…
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden">
      <DivKit
        id={`onec:${mobile ? "m" : "d"}:${location.pathname}`}
        json={card}
        onCustomAction={onCustomAction as NonNullable<DivKitProps["onCustomAction"]>}
      />
    </div>
  );
}
