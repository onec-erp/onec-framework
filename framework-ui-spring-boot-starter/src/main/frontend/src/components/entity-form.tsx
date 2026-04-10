import { useMemo, useState } from "react";
import type { AttributeMeta, TabularSectionMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { RefSelect } from "@/components/ref-select";
import { DatePicker } from "@/components/date-picker";
import { TabularSectionEditor } from "@/components/tabular-section-editor";
import { useWidgetRegistry } from "@/providers/widget-registry";

interface EntityFormProps {
  attributes: AttributeMeta[];
  baseFields?: { label: string; key: string; type?: string }[];
  tabularSections?: TabularSectionMeta[];
  initial?: EntityRecord;
  onSubmit: (data: EntityRecord) => void;
  onCancel: () => void;
}

function fieldType(attr: AttributeMeta): string {
  if (attr.isRef) return "text";
  switch (attr.javaType) {
    case "BigDecimal":
    case "int":
    case "Integer":
    case "long":
    case "Long":
    case "double":
    case "Double":
      return "number";
    case "boolean":
    case "Boolean":
      return "checkbox";
    case "LocalDate":
      return "date";
    case "LocalDateTime":
      return "datetime-local";
    default:
      return "text";
  }
}

export function EntityForm({
  attributes,
  baseFields = [],
  tabularSections = [],
  initial = {},
  onSubmit,
  onCancel,
}: EntityFormProps) {
  const { fieldRenderers } = useWidgetRegistry();
  const [data, setData] = useState<EntityRecord>({ ...initial });

  const set = (key: string, value: unknown) =>
    setData((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(data);
  };

  const formAttrs = useMemo(() => {
    const filtered = attributes.filter((a) => a.visibleInForm !== false);
    filtered.sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    return filtered;
  }, [attributes]);

  const groups = useMemo(() => {
    const map = new Map<string, AttributeMeta[]>();
    for (const attr of formAttrs) {
      const g = attr.group || "";
      if (!map.has(g)) map.set(g, []);
      map.get(g)!.push(attr);
    }
    return map;
  }, [formAttrs]);

  const widthClass = (hint: string) => {
    if (hint === "full") return "md:col-span-2";
    return "";
  };

  const renderField = (attr: AttributeMeta) => {
    const type = fieldType(attr);
    const wc = widthClass(attr.widthHint);

    const CustomRenderer = fieldRenderers.get(attr.javaType);
    if (CustomRenderer) {
      return (
        <div key={attr.fieldName} className={`grid gap-2 ${wc}`}>
          <Label htmlFor={attr.fieldName}>
            {attr.displayName}
            {attr.required && <span className="text-destructive ml-1">*</span>}
          </Label>
          <CustomRenderer
            attr={attr}
            value={data[attr.fieldName]}
            onChange={(v) => set(attr.fieldName, v)}
          />
        </div>
      );
    }

    if (attr.isRef && attr.refTarget) {
      return (
        <div key={attr.fieldName} className={`grid gap-2 ${wc}`}>
          <Label htmlFor={attr.fieldName}>
            {attr.displayName}
            {attr.required && <span className="text-destructive ml-1">*</span>}
          </Label>
          <RefSelect
            catalogName={attr.refTarget}
            value={data[attr.fieldName] as string}
            onChange={(id) => set(attr.fieldName, id)}
          />
        </div>
      );
    }

    if (type === "checkbox") {
      return (
        <div key={attr.fieldName} className={`flex items-center gap-2 ${wc}`}>
          <Checkbox
            id={attr.fieldName}
            checked={!!data[attr.fieldName]}
            onCheckedChange={(v) => set(attr.fieldName, v)}
          />
          <Label htmlFor={attr.fieldName}>{attr.displayName}</Label>
        </div>
      );
    }

    if (type === "date" || type === "datetime-local") {
      return (
        <div key={attr.fieldName} className={`grid gap-2 ${wc}`}>
          <Label htmlFor={attr.fieldName}>
            {attr.displayName}
            {attr.required && <span className="text-destructive ml-1">*</span>}
          </Label>
          <DatePicker
            value={(data[attr.fieldName] as string) ?? ""}
            onChange={(val) => set(attr.fieldName, val)}
            includeTime={type === "datetime-local"}
          />
        </div>
      );
    }

    return (
      <div key={attr.fieldName} className={`grid gap-2 ${wc}`}>
        <Label htmlFor={attr.fieldName}>
          {attr.displayName}
          {attr.required && <span className="text-destructive ml-1">*</span>}
        </Label>
        <Input
          id={attr.fieldName}
          type={type}
          step={type === "number" && attr.scale > 0 ? Math.pow(10, -attr.scale).toString() : undefined}
          maxLength={attr.length > 0 ? attr.length : undefined}
          required={attr.required}
          value={(data[attr.fieldName] as string) ?? ""}
          onChange={(e) =>
            set(
              attr.fieldName,
              type === "number" ? parseFloat(e.target.value) || "" : e.target.value
            )
          }
        />
      </div>
    );
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {baseFields.map((f) => (
        <div key={f.key} className="grid gap-2">
          <Label htmlFor={f.key}>{f.label}</Label>
          {f.type === "date" || f.type === "datetime-local" ? (
            <DatePicker
              value={(data[f.key] as string) ?? ""}
              onChange={(val) => set(f.key, val)}
              includeTime={f.type === "datetime-local"}
            />
          ) : (
            <Input
              id={f.key}
              type={f.type || "text"}
              value={(data[f.key] as string) ?? ""}
              onChange={(e) => set(f.key, e.target.value)}
            />
          )}
        </div>
      ))}

      {[...groups.entries()].map(([groupName, groupAttrs]) => (
        <div key={groupName || "__default"} className="space-y-4">
          {groupName && (
            <div className="pt-4 border-b border-border pb-2">
              <h3 className="text-[13px] font-medium text-muted-foreground">
                {groupName}
              </h3>
            </div>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {groupAttrs.map((attr) => renderField(attr))}
          </div>
        </div>
      ))}

      {tabularSections.length > 0 && (
        <Tabs defaultValue={tabularSections[0].name} className="pt-2">
          <TabsList>
            {tabularSections.map((ts) => (
              <TabsTrigger key={ts.name} value={ts.name}>
                {ts.name}
              </TabsTrigger>
            ))}
          </TabsList>
          {tabularSections.map((ts) => (
            <TabsContent key={ts.name} value={ts.name}>
              <TabularSectionEditor
                section={ts}
                rows={(data[ts.name] as EntityRecord[]) ?? []}
                onChange={(rows) => set(ts.name, rows)}
              />
            </TabsContent>
          ))}
        </Tabs>
      )}

      <div className="flex justify-end gap-2 pt-4 border-t">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit">Save</Button>
      </div>
    </form>
  );
}
