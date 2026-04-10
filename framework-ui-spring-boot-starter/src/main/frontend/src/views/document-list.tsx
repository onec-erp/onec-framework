import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { Plus, Trash2, Eye } from "lucide-react";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { DocumentMeta, EntityRecord } from "@/lib/types";
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

export function DocumentListView() {
  const { name } = useParams<{ name: string }>();
  const [meta, setMeta] = useState<DocumentMeta | null>(null);
  const [items, setItems] = useState<EntityRecord[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);

  const load = () => {
    if (!name) return;
    api.listDocuments(name).then(setItems);
  };

  useEffect(() => {
    if (!name) return;
    api.getDocuments().then((all) => {
      const found = all.find((d) => toSnakeCase(d.name) === name);
      setMeta(found ?? null);
    });
    load();
  }, [name]);

  const handleCreate = async (data: EntityRecord) => {
    if (!name) return;
    await api.createDocument(name, data);
    setDialogOpen(false);
    load();
  };

  const handleDelete = async (id: string) => {
    if (!name) return;
    await api.deleteDocument(name, id);
    load();
  };

  if (!meta) return <div>Loading...</div>;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold">{meta.name}</h1>
          <p className="text-muted-foreground text-sm">Document</p>
        </div>
        <Button onClick={() => setDialogOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New
        </Button>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Number</TableHead>
              <TableHead>Date</TableHead>
              <TableHead>Posted</TableHead>
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
                  colSpan={4 + meta.attributes.length}
                  className="text-center text-muted-foreground py-8"
                >
                  No records
                </TableCell>
              </TableRow>
            )}
            {items.map((item) => (
              <TableRow key={item._id as string}>
                <TableCell className="font-mono">{item._number as string}</TableCell>
                <TableCell>
                  {item._date
                    ? new Date(item._date as string).toLocaleString()
                    : "—"}
                </TableCell>
                <TableCell>
                  <Badge variant={item._posted ? "default" : "secondary"}>
                    {item._posted ? "Posted" : "Draft"}
                  </Badge>
                </TableCell>
                {meta.attributes.map((a) => (
                  <TableCell key={a.fieldName}>
                    {String(item[a.columnName] ?? "")}
                  </TableCell>
                ))}
                <TableCell>
                  <div className="flex gap-1">
                    <Button size="icon" variant="ghost" asChild>
                      <Link to={`/documents/${name}/${item._id}`}>
                        <Eye className="h-4 w-4" />
                      </Link>
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
            <DialogTitle>New {meta.name}</DialogTitle>
            <DialogDescription>Fill in the fields to create a new document.</DialogDescription>
          </DialogHeader>
          <EntityForm
            baseFields={[
              { label: "Number", key: "number" },
              { label: "Date", key: "date", type: "datetime-local" },
            ]}
            attributes={meta.attributes}
            onSubmit={handleCreate}
            onCancel={() => setDialogOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}
