import { BrowserRouter, Navigate, Routes, Route, useLocation } from "react-router-dom";
import { Toaster } from "sonner";
import { ThemeProvider } from "@/providers/theme-provider";
import { WidgetRegistryProvider } from "@/providers/widget-registry";
import { AuthProvider, useAuth } from "@/providers/auth-provider";
import { AppShell } from "@/components/layout/app-shell";
import { HomePage } from "@/views/home";
import { LoginView } from "@/views/login";
import { CatalogListView } from "@/views/catalog-list";
import { DocumentListView } from "@/views/document-list";
import { DocumentDetailView } from "@/views/document-detail";
import { RegisterReportView } from "@/views/register-report";
import { builtInDashboardWidgets } from "@/views/home";

function ProtectedApp() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Loading workspace...
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <AppShell />;
}

export default function App() {
  return (
    <ThemeProvider>
      <WidgetRegistryProvider builtInDashboardWidgets={builtInDashboardWidgets}>
        <AuthProvider>
          <BrowserRouter basename="/ui">
            <Routes>
              <Route path="login" element={<LoginView />} />
              <Route element={<ProtectedApp />}>
                <Route index element={<HomePage />} />
                <Route path="catalogs/:name" element={<CatalogListView />} />
                <Route path="documents/:name" element={<DocumentListView />} />
                <Route path="documents/:name/:id" element={<DocumentDetailView />} />
                <Route path="registers/:name" element={<RegisterReportView />} />
              </Route>
            </Routes>
          </BrowserRouter>
          <Toaster richColors position="bottom-right" />
        </AuthProvider>
      </WidgetRegistryProvider>
    </ThemeProvider>
  );
}
