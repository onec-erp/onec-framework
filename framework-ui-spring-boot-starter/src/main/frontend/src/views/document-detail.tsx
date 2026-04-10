import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { DocumentMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";

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

  if (!meta || !doc) return <div>Loading...</div>;

  return (
    <div>
      <Button variant="ghost" className="mb-4" onClick={() => navigate(-1)}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        Back
      </Button>

      <div className="flex items-center gap-3 mb-6">
        <h1 className="text-3xl font-bold">
          {meta.name} #{doc._number as string}
        </h1>
        <Badge variant={doc._posted ? "default" : "secondary"}>
          {doc._posted ? "Posted" : "Draft"}
        </Badge>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-lg">Details</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="text-muted-foreground">Number</dt>
              <dd className="font-mono">{doc._number as string}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Date</dt>
              <dd>{doc._date ? new Date(doc._date as string).toLocaleString() : "—"}</dd>
            </div>
            {meta.attributes.map((a) => (
              <div key={a.fieldName}>
                <dt className="text-muted-foreground">{a.displayName}</dt>
                <dd>{String(doc[a.columnName] ?? "—")}</dd>
              </div>
            ))}
          </dl>
        </CardContent>
      </Card>

      {meta.tabularSections.length > 0 && (
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
                <div className="rounded-md border">
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
      )}
    </div>
  );
}
