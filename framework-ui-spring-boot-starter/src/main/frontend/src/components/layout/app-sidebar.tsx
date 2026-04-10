import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  BookOpen,
  FileText,
  BarChart3,
  FolderOpen,
  ChevronDown,
  ChevronRight,
  Home,
  Moon,
  Sun,
} from "lucide-react";
import { api } from "@/lib/api";
import { cn, toSnakeCase } from "@/lib/utils";
import { useTheme } from "@/providers/theme-provider";

interface NavItem {
  name: string;
  href: string;
}

interface NavSection {
  title: string;
  icon: React.ElementType;
  order: number;
  items: NavItem[];
}

export function AppSidebar() {
  const [sections, setSections] = useState<NavSection[]>([]);
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const location = useLocation();
  const { theme, setTheme } = useTheme();

  useEffect(() => {
    Promise.all([api.getCatalogs(), api.getDocuments(), api.getRegisters()]).then(
      ([catalogs, documents, registers]) => {
        const sectionMap = new Map<string, { order: number; items: NavItem[] }>();

        const add = (
          entity: { name: string; section?: string; sectionOrder?: number },
          prefix: string,
          fallbackSection: string
        ) => {
          const sectionName = entity.section ?? fallbackSection;
          const order = entity.sectionOrder ?? 999;
          const href = `/${prefix}/${toSnakeCase(entity.name)}`;

          if (!sectionMap.has(sectionName)) {
            sectionMap.set(sectionName, { order, items: [] });
          }
          const sec = sectionMap.get(sectionName)!;
          sec.order = Math.min(sec.order, order);
          sec.items.push({ name: entity.name, href });
        };

        catalogs.forEach((c) => add(c, "catalogs", "Catalogs"));
        documents.forEach((d) => add(d, "documents", "Documents"));
        registers.forEach((r) => add(r, "registers", "Registers"));

        const fallbackIcons: Record<string, React.ElementType> = {
          Catalogs: BookOpen,
          Documents: FileText,
          Registers: BarChart3,
        };

        const navSections: NavSection[] = [...sectionMap.entries()]
          .map(([title, { order, items }]) => ({
            title,
            icon: fallbackIcons[title] ?? FolderOpen,
            order,
            items: items.sort((a, b) => a.name.localeCompare(b.name)),
          }))
          .sort((a, b) => a.order - b.order || a.title.localeCompare(b.title));

        setSections(navSections);
      }
    );
  }, []);

  const toggle = (title: string) =>
    setCollapsed((prev) => ({ ...prev, [title]: !prev[title] }));

  return (
    <aside className="flex h-full w-56 flex-col border-r border-border bg-background text-foreground">
      <div className="flex h-12 items-center px-4">
        <span className="text-sm font-semibold tracking-tight">OneC</span>
      </div>

      <nav className="flex-1 overflow-y-auto px-2 pb-2">
        <Link
          to="/"
          className={cn(
            "flex items-center gap-2 rounded-md px-2.5 py-1.5 text-[13px] transition-colors",
            location.pathname === "/"
              ? "bg-accent text-accent-foreground font-medium"
              : "text-muted-foreground hover:text-foreground hover:bg-accent"
          )}
        >
          <Home className="h-3.5 w-3.5" />
          Home
        </Link>

        <div className="mt-4 space-y-3">
          {sections.map((section) => (
            <div key={section.title}>
              <button
                onClick={() => toggle(section.title)}
                className="flex w-full items-center gap-1.5 px-2.5 py-1 text-[11px] font-medium uppercase tracking-wider text-muted-foreground/60 hover:text-muted-foreground transition-colors"
              >
                {section.title}
                {collapsed[section.title] ? (
                  <ChevronRight className="ml-auto h-3 w-3" />
                ) : (
                  <ChevronDown className="ml-auto h-3 w-3" />
                )}
              </button>

              {!collapsed[section.title] && (
                <div className="mt-0.5 space-y-px">
                  {section.items.map((item) => (
                    <Link
                      key={item.href}
                      to={item.href}
                      className={cn(
                        "flex items-center rounded-md px-2.5 py-1.5 text-[13px] transition-colors",
                        location.pathname === item.href
                          ? "bg-accent text-accent-foreground font-medium"
                          : "text-muted-foreground hover:text-foreground hover:bg-accent"
                      )}
                    >
                      {item.name}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </nav>

      <div className="border-t border-border px-2 py-2">
        <button
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="flex w-full items-center gap-2 rounded-md px-2.5 py-1.5 text-[13px] text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
        >
          {theme === "dark" ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
          {theme === "dark" ? "Light" : "Dark"}
        </button>
      </div>
    </aside>
  );
}
