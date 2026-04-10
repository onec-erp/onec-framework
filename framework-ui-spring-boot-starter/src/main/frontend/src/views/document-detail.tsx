import { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { DocumentMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import { PageHeader } from "@/components/page-header";

export function DocumentDetailView() {
  const { name, id } = useParams<{ name: string; id: string }>();
  const navigate = useNavigate();
  const [meta, setMeta] = useState<DocumentMeta | null>(null);
  const [doc, setDoc] = useState<EntityRecord | null>(null);

  useEffect(() => {
    if (!name || !id) return;
    api.getDocuments().then((all) => {
      setMeta(all.find((d) => toSnakeCase(d.name) === name) ?? null);
    });
    api.getDocument(name, id).then(setDoc);
  }, [name, id]);

  const detailAttrs = useMemo(
    () => meta?.attributes.filter((a) => a.visibleInDetail !== false).sort((a, b) => (a.order ?? 0) - (b.order ?? 0)) ?? [],
    [meta]
  );

  const handlePost = async () => {
    if (!name || !id) return;
    await api.postDocument(name, id);
    api.getDocument(name, id).then(setDoc);
  };

  const handleUnpost = async () => {
    if (!name || !id) return;
    await api.unpostDocument(name, id);
    api.getDocument(name, id).then(setDoc);
  };

  if (!meta || !doc) {
    return (
      <div className="animate-in-page">
        <div className="mb-6">
          <Skeleton className="h-4 w-64 mb-3" />
          <Skeleton className="h-8 w-48 mb-2" />
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-4 w-16" />
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="space-y-2">
                  <Skeleton className="h-3 w-16" />
                  <Skeleton className="h-4 w-32" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="animate-in-page">
      <PageHeader
        title={`${meta.name} #${doc._number as string}`}
        breadcrumbs={[
          { label: "Documents" },
          { label: meta.name, href: `/documents/${name}` },
          { label: `#${doc._number as string}` },
        ]}
        badge={
          <Badge variant={doc._posted ? "success" : "secondary"}>
            {doc._posted ? "Posted" : "Draft"}
          </Badge>
        }
        actions={
          doc._posted ? (
            <Button variant="outline" size="sm" onClick={handleUnpost}>
              Unpost
            </Button>
          ) : (
            <Button size="sm" onClick={handlePost}>
              Post
            </Button>
          )
        }
      />

      <Card className="mb-6">
        <CardHeader className="pb-4">
          <CardTitle className="text-[13px] font-medium text-muted-foreground">
            Details
          </CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-4 text-sm">
            <div className="space-y-1">
              <dt className="text-xs text-muted-foreground">Number</dt>
              <dd className="font-mono">{doc._number as string}</dd>
            </div>
            <div className="space-y-1">
              <dt className="text-xs text-muted-foreground">Date</dt>
              <dd>{doc._date ? new Date(doc._date as string).toLocaleString() : "—"}</dd>
            </div>
            {detailAttrs.map((a) => (
              <div key={a.fieldName} className="space-y-1">
                <dt className="text-xs text-muted-foreground">{a.displayName}</dt>
                <dd>{String(doc[a.columnName] ?? "—")}</dd>
              </div>
            ))}
          </dl>
        </CardContent>
      </Card>

      {meta.tabularSections.length > 0 && (
        <div className="mt-8">
          <Tabs defaultValue={meta.tabularSections[0].name}>
            <TabsList>
              {meta.tabularSections.map((ts) => (
                <TabsTrigger key={ts.name} value={ts.name}>
                  {ts.name}
                </TabsTrigger>
              ))}
            </TabsList>

            {meta.tabularSections.map((ts) => {
              const rows = (doc[ts.name] as EntityRecord[]) ?? [];
              return (
                <TabsContent key={ts.name} value={ts.name}>
                  <div className="rounded-lg border overflow-hidden">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>#</TableHead>
                          {ts.attributes.map((a) => (
                            <TableHead key={a.fieldName}>{a.displayName}</TableHead>
                          ))}
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {rows.length === 0 && (
                          <TableRow>
                            <TableCell
                              colSpan={1 + ts.attributes.length}
                              className="text-center text-muted-foreground py-8"
                            >
                              No rows
                            </TableCell>
                          </TableRow>
                        )}
                        {rows.map((row, i) => (
                          <TableRow key={i}>
                            <TableCell>{(row._line_number as number) ?? i + 1}</TableCell>
                            {ts.attributes.map((a) => (
                              <TableCell key={a.fieldName}>
                                {String(row[a.columnName] ?? "")}
                              </TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </TabsContent>
              );
            })}
          </Tabs>
        </div>
      )}
    </div>
  );
}
