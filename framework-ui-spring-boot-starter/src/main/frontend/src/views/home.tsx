import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { BookOpen, FileText, BarChart3 } from "lucide-react";
import { api } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function HomePage() {
  const [counts, setCounts] = useState({ catalogs: 0, documents: 0, registers: 0 });

  useEffect(() => {
    Promise.all([api.getCatalogs(), api.getDocuments(), api.getRegisters()]).then(
      ([c, d, r]) => setCounts({ catalogs: c.length, documents: d.length, registers: r.length })
    );
  }, []);

  const cards = [
    { title: "Catalogs", count: counts.catalogs, icon: BookOpen, href: "#catalogs" },
    { title: "Documents", count: counts.documents, icon: FileText, href: "#documents" },
    { title: "Registers", count: counts.registers, icon: BarChart3, href: "#registers" },
  ];

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Dashboard</h1>
      <div className="grid gap-4 md:grid-cols-3">
        {cards.map((c) => (
          <Card key={c.title}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">{c.title}</CardTitle>
              <c.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{c.count}</div>
              <p className="text-xs text-muted-foreground">registered types</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
