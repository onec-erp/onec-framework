import { useEffect, useMemo, useState } from "react";
import { format } from "date-fns";
import { useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { DashboardWidgetMeta, EntityRecord } from "@/lib/types";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface ListWidgetProps {
  widget: DashboardWidgetMeta;
}

// Best-effort label for a row's headline — the authored titleField wins, then the
// usual document/catalog identity fields.
function headline(row: EntityRecord, titleField: string): string {
  const candidate = row[titleField] ?? row._number ?? row._code ?? row._description ?? row.name;
  return typeof candidate === "string" || typeof candidate === "number" ? String(candidate) : "";
}

// A secondary line: the first *_display reference label we find (client, property…).
function subtitle(row: EntityRecord): string {
  const preferred = ["client_display", "primary_client_display", "property_display", "customer_display"];
  for (const key of preferred) {
    const v = row[key];
    if (typeof v === "string" && v.trim()) return v;
  }
  for (const key of Object.keys(row)) {
    if (key.endsWith("_display") && typeof row[key] === "string" && (row[key] as string).trim()) {
      return row[key] as string;
    }
  }
  return "";
}

// A trailing money figure, if the row carries an obvious total.
function amount(row: EntityRecord): string {
  for (const key of ["total", "total_gross", "amount", "_sum"]) {
    const v = row[key];
    if (typeof v === "number") return `$${v.toFixed(2)}`;
  }
  return "";
}

function when(row: EntityRecord): string {
  const raw = row._date;
  if (typeof raw === "string" && raw) {
    try {
      return format(new Date(raw), "MMM d");
    } catch {
      // fall through
    }
  }
  return "";
}

export function ListWidget({ widget }: ListWidgetProps) {
  const [items, setItems] = useState<EntityRecord[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const name = toSnakeCase(widget.entityName);
    if (widget.entityType === "document") {
      api.listDocuments(name).then(setItems);
    } else if (widget.entityType === "catalog") {
      api.listCatalog(name).then(setItems);
    }
  }, [widget]);

  const rows = useMemo(() => {
    // Most-recent first when the entity is dated (documents); otherwise as served.
    const sorted = [...items].sort((a, b) => {
      const da = typeof a._date === "string" ? a._date : "";
      const db = typeof b._date === "string" ? b._date : "";
      return db.localeCompare(da);
    });
    return sorted.slice(0, widget.maxItems || 8);
  }, [items, widget.maxItems]);

  const open = (row: EntityRecord) => {
    const id = String(row._id ?? "");
    if (!id) return;
    const name = toSnakeCase(widget.entityName);
    navigate(`/${widget.entityType}s/${name}/${id}`);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-[13px] font-medium">{widget.title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <p className="py-6 text-center text-xs text-muted-foreground">No records yet.</p>
        ) : (
          <ul className="divide-y divide-border">
            {rows.map((row) => {
              const head = headline(row, widget.titleField || "_number");
              const sub = subtitle(row);
              const money = amount(row);
              const date = when(row);
              return (
                <li key={String(row._id)}>
                  <button
                    type="button"
                    onClick={() => open(row)}
                    className="flex w-full items-center justify-between gap-3 py-2 text-left transition-colors hover:bg-accent/40 -mx-2 px-2 rounded-md"
                  >
                    <div className="min-w-0">
                      <div className="truncate text-[13px] font-medium leading-tight">
                        {head || sub || "—"}
                      </div>
                      {head && sub && (
                        <div className="truncate text-[11px] text-muted-foreground">{sub}</div>
                      )}
                    </div>
                    <div className="flex shrink-0 flex-col items-end">
                      {money && <span className="text-[12px] font-medium tabular-nums">{money}</span>}
                      {date && <span className="text-[10px] text-muted-foreground tabular-nums">{date}</span>}
                    </div>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
