import { Outlet } from "react-router-dom";
import { AppSidebar } from "./app-sidebar";

export function AppShell() {
  return (
    <div className="flex h-screen overflow-hidden">
      <AppSidebar />
      <main className="flex-1 overflow-y-auto">
        <div className="container py-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
