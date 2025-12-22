import ReactDOM from "react-dom/client";
import "./index.css";
import { AppErrorOutlet } from "@arcata/components";
import { getCurrentUser, setSession } from "@arcata/db";
import { ui } from "@arcata/envs";
import { load } from "@arcata/translate";
import {
  createBrowserRouter,
  RouterProvider,
  redirect,
} from "react-router-dom";
import { route as appRoute } from "./routes/App";

const router = createBrowserRouter([
  appRoute,
  {
    path: "/session",
    element: null,
    errorElement: <AppErrorOutlet />,
    loader: async () => {
      const search = new URLSearchParams(document.location.search);
      const loginUrl = ui.get("VITE_AUTH_BASE_URL");
      try {
        const token = search.get("t");
        if (!token) {
          return redirect(loginUrl);
        }
        const sessionData = JSON.parse(decodeURIComponent(token));
        const { error } = await setSession(sessionData);
        if (error) {
          return redirect(loginUrl);
        }
        const user = await getCurrentUser();
        if (user) {
          return redirect("/");
        }
        return redirect(loginUrl);
      } catch {
        return redirect(loginUrl);
      }
    },
  },
]);

const loadApp = async () => {
  try {
    await load({
      loadPath: "/locales/{{lng}}.json",
      crossDomain: false,
    });
  } catch (error) {
    //i18next will fallback to 'en' on an error
    console.log(error);
  }

  const rootElement = document.getElementById("root");
  if (!rootElement) {
    throw new Error("Root element not found");
  }
  ReactDOM.createRoot(rootElement).render(<RouterProvider router={router} />);
};

loadApp();
