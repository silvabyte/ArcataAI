import ReactDOM from "react-dom/client";
import SessionLogin from "./routes/session/SessionLogin";
import "./index.css";
import { AppErrorOutlet } from "@arcata/components";
import { getSession } from "@arcata/db";
import { ui } from "@arcata/envs";
import { load } from "@arcata/translate";
import type { Session } from "@supabase/supabase-js";
import {
  createBrowserRouter,
  RouterProvider,
  redirect,
} from "react-router-dom";
import SessionAuthenticate from "./routes/session/SessionAuthenticate";
import SessionVerify from "./routes/session/SessionVerify";

function urlencodeJsonToken(jsonToken: Session) {
  return encodeURIComponent(JSON.stringify(jsonToken));
}

const redirectToApp = async () => {
  const token = await getSession();
  if (token) {
    const url = `${ui.get("VITE_HQ_BASE_URL")}/session?t=${urlencodeJsonToken(token)}`;
    return redirect(url);
  }
  return null;
};
const router = createBrowserRouter([
  {
    path: "/",
    element: <SessionLogin />,
    errorElement: <AppErrorOutlet />,
    loader: async () => await redirectToApp(),
  },
  {
    path: "/verify",
    element: <SessionVerify />,
    errorElement: <AppErrorOutlet />,
    loader: async () => await redirectToApp(),
  },
  {
    path: "/authenticate",
    element: <SessionAuthenticate />,
    errorElement: <AppErrorOutlet />,
    loader: async () => await redirectToApp(),
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
