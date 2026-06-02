import { useMemo, useState } from "react";
import { toast } from "sonner";
import { Check, X } from "lucide-react";
import type { AttributeMeta, EntityRecord } from "@/lib/types";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { RefSelect } from "@/components/ref-select";
import { DatePicker } from "@/components/date-picker";

// Matches the DivKit action pills (Edit/Delete/New): a compact dark pill, icon + label,
// rounded-lg, text-sm/medium, with the same vertical/horizontal rhythm.
const actionBtn =
  "inline-flex items-center gap-1.5 rounded-lg bg-secondary px-3.5 py-2 text-sm font-medium transition-colors hover:bg-accent disabled:opacity-50";

// The portable form descriptor the server emits as the onec-form custom component.
export type FormDescriptor = {
  kind: "documents" | "catalogs";
  name: string;
  id: string | null;
  title: string;
  submitLabel: string;
  meta: {
    name: string;
    autoNumber?: boolean;
    attributes: AttributeMeta[];
  };
  initial: EntityRecord | null;
};

// One editable field: either a catalog system field (code/description) or an attribute.
type Field =
  | { kind: "system"; key: string; label: string; column: string }
  | { kind: "attr"; key: string; label: string; attr: AttributeMeta };

function isNumeric(javaType: string): boolean {
  return ["BigDecimal", "Integer", "Long", "Double", "Float", "Short", "int", "long", "double"].includes(
    javaType
  );
}

// Fire an onec:// action / pane-close through the host (divkit-view) — same routing the
// DivKit surfaces use, so a form opened in an island behaves like any other navigation.
function dispatchAction(url: string) {
  window.dispatchEvent(new CustomEvent("onec:action", { detail: url }));
}
function dispatchClose(path: string) {
  window.dispatchEvent(new CustomEvent("onec:closepath", { detail: path }));
}

export function EntityFormWidget({ form }: { form: FormDescriptor }) {
  const { kind, name, id, meta, initial } = form;
  const isEdit = id != null;
  const formPath = `/${kind}/${name}/${isEdit ? `${id}/edit` : "new"}`;

  // Build the ordered field list: catalogs lead with code (unless auto-numbered) +
  // description; both then list the visible-in-form attributes by their order hint.
  const fields = useMemo<Field[]>(() => {
    const out: Field[] = [];
    if (kind === "catalogs") {
      if (!meta.autoNumber) {
        out.push({ kind: "system", key: "code", label: "Code", column: "_code" });
      }
      out.push({ kind: "system", key: "description", label: "Description", column: "_description" });
    }
    const attrs = meta.attributes
      .filter((a) => a.visibleInForm !== false)
      .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
    for (const attr of attrs) {
      out.push({ kind: "attr", key: attr.fieldName, label: attr.displayName, attr });
    }
    return out;
  }, [kind, meta]);

  const [data, setData] = useState<EntityRecord>(() => {
    const seed: EntityRecord = {};
    if (!initial) return seed;
    for (const f of fields) {
      const col = f.kind === "system" ? f.column : f.attr.columnName;
      if (initial[col] != null) seed[f.key] = initial[col];
    }
    return seed;
  });
  const [saving, setSaving] = useState(false);

  const set = (key: string, value: unknown) => setData((prev) => ({ ...prev, [key]: value }));

  const save = async () => {
    setSaving(true);
    try {
      const payload = { ...data };
      let saved: EntityRecord;
      if (kind === "documents") {
        saved = isEdit ? await api.updateDocument(name, id!, payload) : await api.createDocument(name, payload);
      } else {
        saved = isEdit
          ? await api.updateCatalogItem(name, id!, payload)
          : await api.createCatalogItem(name, payload);
      }
      const savedId = String(isEdit ? id : saved._id);
      // Open the saved record and close the form pane (the list refreshes over SSE).
      dispatchAction(`onec://${kind}/${name}/${savedId}`);
      dispatchClose(formPath);
    } catch (e) {
      toast.error(`Couldn't save: ${e instanceof Error ? e.message : String(e)}`);
      setSaving(false);
    }
  };

  const cancel = () => {
    if (isEdit) dispatchAction(`onec://${kind}/${name}/${id}`);
    dispatchClose(formPath);
  };

  return (
    <div className="mx-auto w-full max-w-2xl">
      <h1 className="mb-5 text-xl font-semibold text-foreground">{form.title}</h1>
      <div className="space-y-4 rounded-2xl border border-border bg-card p-5">
        {fields.map((f) => (
          <FormFieldRow key={f.key} field={f} value={data[f.key]} onChange={(v) => set(f.key, v)} />
        ))}
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <button
          type="button"
          className={cn(actionBtn, "text-muted-foreground hover:text-foreground")}
          onClick={cancel}
          disabled={saving}
        >
          <X className="size-4" aria-hidden="true" />
          Cancel
        </button>
        <button
          type="button"
          className={cn(actionBtn, "text-foreground")}
          onClick={save}
          disabled={saving}
        >
          <Check className="size-4" aria-hidden="true" />
          {saving ? "Saving…" : form.submitLabel}
        </button>
      </div>
    </div>
  );
}

function FormFieldRow({
  field,
  value,
  onChange,
}: {
  field: Field;
  value: unknown;
  onChange: (value: unknown) => void;
}) {
  const required = field.kind === "attr" && field.attr.required;
  const control =
    field.kind === "system" ? (
      <Input value={(value as string) ?? ""} onChange={(e) => onChange(e.target.value)} />
    ) : (
      <AttrControl attr={field.attr} value={value} onChange={onChange} />
    );

  // Checkboxes own their label inline.
  if (field.kind === "attr" && /^(boolean|Boolean)$/.test(field.attr.javaType)) {
    return (
      <div className="flex items-center gap-2">
        <Checkbox
          id={field.key}
          checked={!!value}
          onCheckedChange={(v) => onChange(v === true)}
        />
        <Label htmlFor={field.key}>{field.label}</Label>
      </div>
    );
  }

  return (
    <div className="grid gap-1.5">
      <Label htmlFor={field.key}>
        {field.label}
        {required ? <span className="ml-1 text-destructive">*</span> : null}
      </Label>
      {control}
    </div>
  );
}

function AttrControl({
  attr,
  value,
  onChange,
}: {
  attr: AttributeMeta;
  value: unknown;
  onChange: (value: unknown) => void;
}) {
  if (attr.isRef && attr.refTarget) {
    return (
      <RefSelect catalogName={attr.refTarget} value={value as string | undefined} onChange={onChange} />
    );
  }

  if (attr.isEnum && attr.enumValues) {
    return (
      <Select value={(value as string) ?? ""} onValueChange={onChange}>
        <SelectTrigger>
          <SelectValue placeholder={`Select ${attr.displayName}…`} />
        </SelectTrigger>
        <SelectContent>
          {attr.enumValues.map((ev) => (
            <SelectItem key={ev.id} value={ev.id}>
              {ev.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    );
  }

  if (attr.javaType === "LocalDate" || attr.javaType === "LocalDateTime") {
    return (
      <DatePicker
        value={(value as string) ?? ""}
        onChange={onChange}
        includeTime={attr.javaType === "LocalDateTime"}
      />
    );
  }

  const numeric = isNumeric(attr.javaType);
  return (
    <Input
      type={numeric ? "number" : "text"}
      step={numeric && attr.scale > 0 ? Math.pow(10, -attr.scale).toString() : undefined}
      maxLength={attr.length > 0 && !numeric ? attr.length : undefined}
      value={(value as string) ?? ""}
      onChange={(e) => onChange(numeric ? (e.target.value === "" ? "" : Number(e.target.value)) : e.target.value)}
    />
  );
}
