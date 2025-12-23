import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useState,
} from "react";
import { Notification, type NotificationVariant } from "./Notification";

type NotificationState = {
  show: boolean;
  title: string;
  message?: string;
  variant: NotificationVariant;
};

type NotificationContextValue = {
  notify: (
    title: string,
    variant: NotificationVariant,
    message?: string,
    autoDismiss?: number
  ) => void;
  dismiss: () => void;
};

const NotificationContext = createContext<NotificationContextValue | null>(
  null
);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<NotificationState>({
    show: false,
    title: "",
    variant: "success",
  });

  const dismiss = useCallback(() => {
    setState((prev) => ({ ...prev, show: false }));
  }, []);

  const notify = useCallback(
    (
      title: string,
      variant: NotificationVariant,
      message?: string,
      autoDismiss?: number
    ) => {
      setState({ show: true, title, message, variant });

      if (autoDismiss && variant !== "loading") {
        setTimeout(dismiss, autoDismiss);
      }
    },
    [dismiss]
  );

  return (
    <NotificationContext.Provider value={{ notify, dismiss }}>
      {children}
      <Notification
        message={state.message}
        onClose={dismiss}
        show={state.show}
        title={state.title}
        variant={state.variant}
      />
    </NotificationContext.Provider>
  );
}

export function useNotification(): NotificationContextValue {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error(
      "useNotification must be used within a NotificationProvider"
    );
  }
  return context;
}
