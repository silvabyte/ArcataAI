import { t } from "@arcata/translate";
import { useNavigate, useRouteError } from "react-router-dom";
import { AppButton } from "../buttons/AppButton";
import { SupportLink } from "../support/SupportLink";

export function AppErrorOutlet() {
  const error = useRouteError() as Error & { statusText?: string };
  const nav = useNavigate();
  return (
    <main className="grid min-h-full place-items-center bg-white px-6 py-24 sm:py-32 lg:px-8">
      <div className="text-center">
        <p className="font-semibold text-base text-black">
          {t("common.errors.oops")}
        </p>
        <h1 className="mt-4 font-bold text-3xl text-gray-900 tracking-tight sm:text-5xl">
          {error.statusText || error.message}
        </h1>
        <p className="mt-6 text-base text-gray-600 leading-7">
          {t("common.errors.generic")}
        </p>
        <div className="mt-10 flex items-center justify-center gap-x-6">
          <div>
            <AppButton onClick={() => nav("/")}>{t("nav.goHome")}</AppButton>
          </div>
          <SupportLink classes="capitalize" />
        </div>
      </div>
    </main>
  );
}
