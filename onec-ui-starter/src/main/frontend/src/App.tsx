import type { ReactNode } from "react";
import { BrowserRouter, Navigate, Routes, Route, useLocation } from "react-router-dom";
import { Toaster } from "sonner";
import { ThemeProvider } from "@/providers/theme-provider";
import { AuthProvider, useAuth } from "@/providers/auth-provider";
import { LoginView } from "@/views/login";
import { PortfolioPage } from "@/views/portfolio";
import { DivKitView } from "@/views/divkit-view";

function ProtectedApp() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Loading workspace...
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  // The entire authenticated app is server-driven DivKit (chrome + content).
  return <DivKitView />;
}

function WorkspaceProviders({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <AuthProvider>
        {children}
        <Toaster richColors position="bottom-right" />
      </AuthProvider>
    </ThemeProvider>
  );
}

export default function App() {
  return (
    <BrowserRouter basename="/">
      <Routes>
        <Route path="/portfolio" element={<PortfolioPage />} />
        <Route path="/login" element={<WorkspaceProviders><LoginView /></WorkspaceProviders>} />
        <Route path="*" element={<WorkspaceProviders><ProtectedApp /></WorkspaceProviders>} />
      </Routes>
    </BrowserRouter>
  );
}
