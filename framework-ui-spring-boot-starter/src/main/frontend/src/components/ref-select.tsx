import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { EntityRecord } from "@/lib/types";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface RefSelectProps {
  catalogName: string;
  value?: string;
  onChange: (id: string) => void;
}

export function RefSelect({ catalogName, value, onChange }: RefSelectProps) {
  const [items, setItems] = useState<EntityRecord[]>([]);

  useEffect(() => {
    api.listCatalog(toSnakeCase(catalogName)).then(setItems);
  }, [catalogName]);

  return (
    <Select value={value ?? ""} onValueChange={onChange}>
      <SelectTrigger>
        <SelectValue placeholder={`Select ${catalogName}...`} />
      </SelectTrigger>
      <SelectContent>
        {items.map((item) => (
          <SelectItem key={item._id as string} value={item._id as string}>
            {item._description
              ? `${item._code ?? ""} — ${item._description}`.trim()
              : (item._code as string) ?? (item._id as string)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
