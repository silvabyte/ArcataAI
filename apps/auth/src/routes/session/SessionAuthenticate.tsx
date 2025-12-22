import { Alert, Logo, SupportLink } from "@arcata/components";
import { verifyOTP } from "@arcata/db";
import { t } from "@arcata/translate";
import type { EmailOtpType } from "@supabase/supabase-js";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export default function SessionAuthenticate() {
  //TODO: use loader for route instead
  const [error, setError] = useState("");
  const nav = useNavigate();
  useEffect(() => {
    const params = new URLSearchParams(document.location.search);
    const token_hash = params.get("token_hash") as string;
    const type = params.get("type") as EmailOtpType;
    verifyOTP(token_hash, type).then(({ error: verificationError }) => {
      if (verificationError) {
        setError(verificationError.message);
      } else {
        nav("/");
      }
    });
  }, [nav]);

  return error ? (
    <div className="flex min-h-full flex-1 flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <Logo sizeFactor={2} />
        </div>
        <h2 className="mt-6 text-center font-bold text-2xl text-white leading-9 tracking-tight">
          {t("session.authenticate.oops")}
        </h2>
      </div>

      <div className="mt-10 sm:mx-auto sm:w-full sm:max-w-[480px]">
        <div className="bg-white px-6 py-12 shadow sm:rounded-lg sm:px-12">
          <Alert title={error} type="danger" />
          <p>
            {t("session.authenticate.please")}{" "}
            <Link to="/">
              <span className="italic underline">
                {t("session.authenticate.signingIn")}
              </span>{" "}
            </Link>
            {t("session.authenticate.again")} <SupportLink />
          </p>
        </div>
      </div>
    </div>
  ) : null;
}
