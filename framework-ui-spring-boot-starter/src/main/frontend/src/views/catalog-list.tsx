import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Plus, Trash2, Pencil } from "lucide-react";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { CatalogMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { EntityForm } from "@/components/entity-form";

export function CatalogListView() {
  const { name } = useParams<{ name: string }>();
  const [meta, setMeta] = useState<CatalogMeta | null>(null);
  const [items, setItems] = useState<EntityRecord[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<EntityRecord | null>(null);

  const load = () => {
    if (!name) return;
    api.listCatalog(name).then(setItems);
  };

  useEffect(() => {
    if (!name) return;
    api.getCatalogs().then((all) => {
      const found = all.find((c) => toSnakeCase(c.name) === name);
      setMeta(found ?? null);
    });
    load();
  }, [name]);

  const handleSave = async (data: EntityRecord) => {
    if (!name) return;
    if (editing && editing._id) {
      await api.updateCatalogItem(name, editing._id as string, data);
    } else {
      await api.createCatalogItem(name, data);
    }
    setDialogOpen(false);
    setEditing(null);
    load();
  };

  const handleDelete = async (id: string) => {
    if (!name) return;
    await api.deleteCatalogItem(name, id);
    load();
  };

  if (!meta) return <div>Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">{meta.name}</h1>
          <p className="text-muted-foreground text-sm">Catalog</p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setDialogOpen(true);
          }}
        >
          <Plus className="h-4 w-4 mr-2" />
          New
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Description</TableHead>
              {meta.attributes.map((a) => (
                <TableHead key={a.fieldName}>{a.displayName}</TableHead>
              ))}
              <TableHead className="w-[100px]">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={3 + meta.attributes.length}
                  className="text-center text-muted-foreground py-8"
                >
                  No records
                </TableCell>
              </TableRow>
            )}
            {items.map((item) => (
              <TableRow key={item._id as string}>
                <TableCell className="font-mono">{item._code as string}</TableCell>
                <TableCell>{item._description as string}</TableCell>
                {meta.attributes.map((a) => (
                  <TableCell key={a.fieldName}>
                    {String(item[a.columnName] ?? "")}
                  </TableCell>
                ))}
                <TableCell>
                  <div className="flex gap-1">
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => {
                        setEditing(item);
                        setDialogOpen(true);
                      }}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => handleDelete(item._id as string)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "Edit" : "New"} {meta.name}</DialogTitle>
            <DialogDescription>
              {editing ? "Update the record fields below." : "Fill in the fields to create a new record."}
            </DialogDescription>
          </DialogHeader>
          <EntityForm
            baseFields={[
              { label: "Code", key: "code" },
              { label: "Description", key: "description" },
            ]}
            attributes={meta.attributes}
            initial={
              editing
                ? {
                    code: editing._code,
                    description: editing._description,
                    ...Object.fromEntries(
                      meta.attributes.map((a) => [a.fieldName, editing[a.columnName]])
                    ),
                  }
                : {}
            }
            onSubmit={handleSave}
            onCancel={() => setDialogOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
