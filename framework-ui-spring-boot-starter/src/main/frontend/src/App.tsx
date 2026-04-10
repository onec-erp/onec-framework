import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Toaster } from "sonner";
import { ThemeProvider } from "@/providers/theme-provider";
import { AppShell } from "@/components/layout/app-shell";
import { HomePage } from "@/views/home";
import { CatalogListView } from "@/views/catalog-list";
import { DocumentListView } from "@/views/document-list";
import { DocumentDetailView } from "@/views/document-detail";
import { RegisterReportView } from "@/views/register-report";

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter basename="/ui">
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<HomePage />} />
            <Route path="catalogs/:name" element={<CatalogListView />} />
            <Route path="documents/:name" element={<DocumentListView />} />
            <Route path="documents/:name/:id" element={<DocumentDetailView />} />
            <Route path="registers/:name" element={<RegisterReportView />} />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster richColors position="bottom-right" />
    </ThemeProvider>
  );
}
