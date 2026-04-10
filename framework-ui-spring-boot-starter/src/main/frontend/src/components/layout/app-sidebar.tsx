import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  BookOpen,
  FileText,
  BarChart3,
  ChevronDown,
  ChevronRight,
} from "lucide-react";
import { api } from "@/lib/api";
import type { CatalogMeta, DocumentMeta, RegisterMeta } from "@/lib/types";
import { cn, toSnakeCase } from "@/lib/utils";
import { Separator } from "@/components/ui/separator";

interface NavSection {
  title: string;
  icon: React.ElementType;
  items: { name: string; href: string }[];
}

export function AppSidebar() {
  const [sections, setSections] = useState<NavSection[]>([]);
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const location = useLocation();

  useEffect(() => {
    Promise.all([api.getCatalogs(), api.getDocuments(), api.getRegisters()]).then(
      ([catalogs, documents, registers]) => {
        const navSections: NavSection[] = [];

        if (catalogs.length > 0) {
          navSections.push({
            title: "Catalogs",
            icon: BookOpen,
            items: catalogs.map((c) => ({
              name: c.name,
              href: `/catalogs/${toSnakeCase(c.name)}`,
            })),
          });
        }

        if (documents.length > 0) {
          navSections.push({
            title: "Documents",
            icon: FileText,
            items: documents.map((d) => ({
              name: d.name,
              href: `/documents/${toSnakeCase(d.name)}`,
            })),
          });
        }

        if (registers.length > 0) {
          navSections.push({
            title: "Registers",
            icon: BarChart3,
            items: registers.map((r) => ({
              name: r.name,
              href: `/registers/${toSnakeCase(r.name)}`,
            })),
          });
        }

        setSections(navSections);
      }
    );
  }, []);

  const toggle = (title: string) =>
    setCollapsed((prev) => ({ ...prev, [title]: !prev[title] }));

  return (
    <aside className="flex h-full w-64 flex-col border-r bg-sidebar text-sidebar-foreground">
      <div className="flex h-14 items-center gap-2 border-b px-4">
        <span className="text-lg font-semibold">OneC</span>
      </div>

      <nav className="flex-1 overflow-y-auto p-2">
        <Link
          to="/"
          className={cn(
            "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-sidebar-accent",
            location.pathname === "/" && "bg-sidebar-accent text-sidebar-accent-foreground"
          )}
        >
          Home
        </Link>

        <Separator className="my-2" />

        {sections.map((section) => (
          <div key={section.title} className="mb-1">
            <button
              onClick={() => toggle(section.title)}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-sidebar-foreground/70 hover:bg-sidebar-accent transition-colors"
            >
              <section.icon className="h-4 w-4" />
              {section.title}
              {collapsed[section.title] ? (
                <ChevronRight className="ml-auto h-4 w-4" />
              ) : (
                <ChevronDown className="ml-auto h-4 w-4" />
              )}
            </button>

            {!collapsed[section.title] && (
              <div className="ml-4 space-y-0.5">
                {section.items.map((item) => (
                  <Link
                    key={item.href}
                    to={item.href}
                    className={cn(
                      "flex items-center rounded-md px-3 py-1.5 text-sm transition-colors hover:bg-sidebar-accent",
                      location.pathname === item.href &&
                        "bg-sidebar-accent text-sidebar-accent-foreground font-medium"
                    )}
                  >
                    {item.name}
                  </Link>
                ))}
              </div>
            )}
          </div>
        ))}
      </nav>
    </aside>
  );
}
