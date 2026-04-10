import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "@/lib/api";
import { toSnakeCase } from "@/lib/utils";
import type { RegisterMeta, EntityRecord } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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

export function RegisterReportView() {
  const { name } = useParams<{ name: string }>();
  const [meta, setMeta] = useState<RegisterMeta | null>(null);
  const [movements, setMovements] = useState<EntityRecord[]>([]);
  const [balances, setBalances] = useState<EntityRecord[]>([]);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");

  useEffect(() => {
    if (!name) return;
    api.getRegisters().then((all) => {
      const found = all.find((r) => toSnakeCase(r.name) === name);
      setMeta(found ?? null);
    });
  }, [name]);

  useEffect(() => {
    if (!name || !meta) return;
    api.getMovements(name).then(setMovements);
    if (meta.type === "BALANCE") {
      api.getBalance(name).then(setBalances);
    }
  }, [name, meta]);

  const loadTurnover = () => {
    if (!name || !from || !to) return;
    api.getTurnover(name, from, to).then(setBalances);
  };

  if (!meta) return <div>Loading...</div>;

  const allColumns = [...meta.dimensions, ...meta.resources];

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <h1 className="text-3xl font-bold">{meta.name}</h1>
        <Badge variant="outline">{meta.type}</Badge>
      </div>

      <Tabs defaultValue={meta.type === "BALANCE" ? "balance" : "movements"}>
        <TabsList>
          {meta.type === "BALANCE" && (
            <TabsTrigger value="balance">Balance</TabsTrigger>
          )}
          <TabsTrigger value="movements">Movements</TabsTrigger>
          <TabsTrigger value="turnover">Turnover</TabsTrigger>
        </TabsList>

        {meta.type === "BALANCE" && (
          <TabsContent value="balance">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Current Balances</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        {allColumns.map((c) => (
                          <TableHead key={c.fieldName}>{c.displayName}</TableHead>
                        ))}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {balances.length === 0 && (
                        <TableRow>
                          <TableCell
                            colSpan={allColumns.length}
                            className="text-center text-muted-foreground py-8"
                          >
                            No data
                          </TableCell>
                        </TableRow>
                      )}
                      {balances.map((row, i) => (
                        <TableRow key={i}>
                          {allColumns.map((c) => (
                            <TableCell key={c.fieldName}>
                              {String(row[c.columnName] ?? "")}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        )}

        <TabsContent value="movements">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Movement Records</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Period</TableHead>
                      <TableHead>Type</TableHead>
                      {allColumns.map((c) => (
                        <TableHead key={c.fieldName}>{c.displayName}</TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {movements.length === 0 && (
                      <TableRow>
                        <TableCell
                          colSpan={2 + allColumns.length}
                          className="text-center text-muted-foreground py-8"
                        >
                          No movements
                        </TableCell>
                      </TableRow>
                    )}
                    {movements.map((row, i) => (
                      <TableRow key={i}>
                        <TableCell>
                          {row._period
                            ? new Date(row._period as string).toLocaleString()
                            : "—"}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              row._movement_type === "RECEIPT" ? "default" : "destructive"
                            }
                          >
                            {row._movement_type as string}
                          </Badge>
                        </TableCell>
                        {allColumns.map((c) => (
                          <TableCell key={c.fieldName}>
                            {String(row[c.columnName] ?? "")}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="turnover">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Turnover Report</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-end gap-4 mb-4">
                <div className="grid gap-2">
                  <Label>From</Label>
                  <Input
                    type="datetime-local"
                    value={from}
                    onChange={(e) => setFrom(e.target.value)}
                  />
                </div>
                <div className="grid gap-2">
                  <Label>To</Label>
                  <Input
                    type="datetime-local"
                    value={to}
                    onChange={(e) => setTo(e.target.value)}
                  />
                </div>
                <Button onClick={loadTurnover}>Calculate</Button>
              </div>

              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      {allColumns.map((c) => (
                        <TableHead key={c.fieldName}>{c.displayName}</TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {balances.length === 0 && (
                      <TableRow>
                        <TableCell
                          colSpan={allColumns.length}
                          className="text-center text-muted-foreground py-8"
                        >
                          Select a period and click Calculate
                        </TableCell>
                      </TableRow>
                    )}
                    {balances.map((row, i) => (
                      <TableRow key={i}>
                        {allColumns.map((c) => (
                          <TableCell key={c.fieldName}>
                            {String(row[c.columnName] ?? "")}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
