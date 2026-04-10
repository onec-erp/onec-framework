import { useState } from "react";
import type { AttributeMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";

interface EntityFormProps {
  attributes: AttributeMeta[];
  baseFields?: { label: string; key: string; type?: string }[];
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
  initial = {},
  onSubmit,
  onCancel,
}: EntityFormProps) {
  const [data, setData] = useState<EntityRecord>({ ...initial });

  const set = (key: string, value: unknown) =>
    setData((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {baseFields.map((f) => (
        <div key={f.key} className="grid gap-2">
          <Label htmlFor={f.key}>{f.label}</Label>
          <Input
            id={f.key}
            type={f.type || "text"}
            value={(data[f.key] as string) ?? ""}
            onChange={(e) => set(f.key, e.target.value)}
          />
        </div>
      ))}

      {attributes.map((attr) => {
        const type = fieldType(attr);

        if (type === "checkbox") {
          return (
            <div key={attr.fieldName} className="flex items-center gap-2">
              <Checkbox
                id={attr.fieldName}
                checked={!!data[attr.fieldName]}
                onCheckedChange={(v) => set(attr.fieldName, v)}
              />
              <Label htmlFor={attr.fieldName}>{attr.displayName}</Label>
            </div>
          );
        }

        return (
          <div key={attr.fieldName} className="grid gap-2">
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
      })}

      <div className="flex justify-end gap-2 pt-4">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit">Save</Button>
      </div>
    </form>
  );
}
