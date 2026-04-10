import { useEffect, useMemo, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { Plus, Trash2, Eye, FileText } from "lucide-react";
import { api } from "@/lib/api";
import { toSnakeCase, displayValue } from "@/lib/utils";
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
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { EntityForm } from "@/components/entity-form";
import { PageHeader } from "@/components/page-header";
import { EmptyState } from "@/components/empty-state";
import { SkeletonTable } from "@/components/skeleton-table";
import { Skeleton } from "@/components/ui/skeleton";
import { DeleteDialog } from "@/components/delete-dialog";

export function DocumentListView() {
  const { name } = useParams<{ name: string }>();
  const [meta, setMeta] = useState<DocumentMeta | null>(null);
  const [items, setItems] = useState<EntityRecord[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

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

  const handleCreate = async (data: EntityRecord, andPost?: boolean) => {
    if (!name) return;
    const created = await api.createDocument(name, data);
    if (andPost && created?._id) {
      await api.postDocument(name, created._id as string);
    }
    setDialogOpen(false);
    load();
  };

  const handleDelete = async () => {
    if (!name || !deleteTarget) return;
    await api.deleteDocument(name, deleteTarget);
    setDeleteTarget(null);
    load();
  };

  const listAttrs = useMemo(
    () => meta?.attributes.filter((a) => a.visibleInList !== false).sort((a, b) => (a.order ?? 0) - (b.order ?? 0)) ?? [],
    [meta]
  );

  if (!meta) {
    return (
      <div className="animate-in-page">
        <div className="mb-6">
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-24" />
        </div>
        <SkeletonTable columns={5} rows={5} />
      </div>
    );
  }

  return (
    <div className="animate-in-page">
      <PageHeader
        title={meta.name}
        subtitle="Document"
        breadcrumbs={[{ label: "Documents" }, { label: meta.name }]}
        actions={
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4 mr-2" />
            New
          </Button>
        }
      />

      {items.length === 0 ? (
        <EmptyState
          icon={FileText}
          title="No documents yet"
          description={`Create your first ${meta.name} document to get started.`}
          action={
            <Button onClick={() => setDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Create {meta.name}
            </Button>
          }
        />
      ) : (
        <div className="rounded-lg border overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Number</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Posted</TableHead>
                {listAttrs.map((a) => (
                  <TableHead key={a.fieldName}>{a.displayName}</TableHead>
                ))}
                <TableHead className="w-[100px]">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item._id as string}>
                  <TableCell className="font-mono">{item._number as string}</TableCell>
                  <TableCell>
                    {item._date
                      ? new Date(item._date as string).toLocaleString()
                      : "—"}
                  </TableCell>
                  <TableCell>
                    <Badge variant={item._posted ? "success" : "secondary"}>
                      {item._posted ? "Posted" : "Draft"}
                    </Badge>
                  </TableCell>
                  {listAttrs.map((a) => (
                    <TableCell key={a.fieldName}>
                      {displayValue(a, item[a.columnName], item)}
                    </TableCell>
                  ))}
                  <TableCell>
                    <TooltipProvider>
                      <div className="flex gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button size="icon" variant="ghost" asChild>
                              <Link to={`/documents/${name}/${item._id}`}>
                                <Eye className="h-4 w-4" />
                              </Link>
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>View</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              size="icon"
                              variant="ghost"
                              onClick={() => setDeleteTarget(item._id as string)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Delete</TooltipContent>
                        </Tooltip>
                      </div>
                    </TooltipProvider>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className={meta.tabularSections.length > 0 ? "max-w-4xl max-h-[90vh] overflow-y-auto" : undefined}>
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
            tabularSections={meta.tabularSections}
            onSubmit={handleCreate}
            showSaveAndPost
            onCancel={() => setDialogOpen(false)}
          />
        </DialogContent>
      </Dialog>

      <DeleteDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null); }}
        onConfirm={handleDelete}
        title={`Delete ${meta.name}`}
        description="This will mark the document for deletion. Are you sure?"
      />
    </div>
  );
}
