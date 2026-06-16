import { afterEach, describe, expect, it, vi } from "vitest";

// base-path.ts reads window.__onecBasePath once at module load, so each case sets the global and
// re-imports the module fresh (resetModules clears the cached evaluation).
async function load(value: string | undefined) {
  if (value === undefined) {
    delete (window as { __onecBasePath?: string }).__onecBasePath;
  } else {
    (window as { __onecBasePath?: string }).__onecBasePath = value;
  }
  vi.resetModules();
  return import("@/lib/base-path");
}

describe("base-path", () => {
  afterEach(() => {
    delete (window as { __onecBasePath?: string }).__onecBasePath;
  });

  it("defaults to the web root when the global is unset (e.g. tests)", async () => {
    const m = await load(undefined);
    expect(m.BASE_PATH).toBe("/");
    expect(m.withBasePath("/catalogs/x")).toBe("/catalogs/x");
    expect(m.stripBasePath("/catalogs/x")).toBe("/catalogs/x");
  });

  it("treats the unreplaced placeholder as the web root (Vite dev, no server templating)", async () => {
    const m = await load("__ONEC_BASE_PATH__");
    expect(m.BASE_PATH).toBe("/");
    expect(m.withBasePath("/catalogs/x")).toBe("/catalogs/x");
  });

  it("honors an injected prefix: prefixes links and strips it back off raw pathnames", async () => {
    const m = await load("/ui");
    expect(m.BASE_PATH).toBe("/ui");
    expect(m.withBasePath("/catalogs/x")).toBe("/ui/catalogs/x");
    expect(m.stripBasePath("/ui/catalogs/x")).toBe("/catalogs/x");
    // The mount root itself maps back to the router root.
    expect(m.stripBasePath("/ui")).toBe("/");
    // A path that doesn't carry the prefix is left untouched (defensive).
    expect(m.stripBasePath("/catalogs/x")).toBe("/catalogs/x");
  });

  it("normalizes a trailing slash and a missing leading slash", async () => {
    expect((await load("/ui/")).BASE_PATH).toBe("/ui");
    expect((await load("ui")).BASE_PATH).toBe("/ui");
    expect((await load("/app/ui/")).BASE_PATH).toBe("/app/ui");
  });
});
