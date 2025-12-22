import { Alert, AppButton, type ButtonState, Logo } from "@arcata/components";
import { signInWithEmail } from "@arcata/db";
import { t } from "@arcata/translate";
import type { AuthError } from "@supabase/supabase-js";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Login() {
  const nav = useNavigate();
  //TODO: use form action loader instead of this
  const [buttonState, setButtonState] = useState<ButtonState>("");
  const [error, setError] = useState<Error | AuthError | null>(null);
  const onSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    setButtonState("loading");
    const form = document.getElementById("session-submit") as HTMLFormElement;
    const body = new FormData(form);
    const email = body.get("email") as string;
    const { error: signInError } = await signInWithEmail(email);
    if (signInError) {
      setError(signInError);
      //TODO: handle error
      // throw new Response(error.message, { status: 400 });
      //maybe have session error page that takes error msg in url
    }

    return nav("/verify");
  };

  return (
    <div className="flex min-h-full flex-1 flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <Logo sizeFactor={2} />
        </div>
        <h2 className="mt-6 text-center font-bold text-2xl text-white leading-9 tracking-tight">
          {t("session.title")}
        </h2>
      </div>

      <div className="mt-10 sm:mx-auto sm:w-full sm:max-w-[480px]">
        <div className="bg-white px-6 py-12 shadow sm:rounded-lg sm:px-12">
          {error ? <Alert title={error.message} type="danger" /> : null}
          <form
            className="space-y-6"
            id="session-submit"
            method="POST"
            onSubmit={onSubmit}
          >
            <div>
              <label
                className="block font-medium text-gray-900 text-sm leading-6"
                htmlFor="email"
              >
                {t("session.inputs.email.label")}
              </label>
              <div className="mt-2">
                <input
                  autoComplete="email"
                  className="block w-full rounded-md border-0 px-2 py-1.5 text-gray-900 shadow-sm ring-1 ring-gray-900 ring-inset placeholder:text-gray-400 focus:ring-2 focus:ring-gray-900 focus:ring-inset sm:text-sm sm:leading-6"
                  id="email"
                  name="email"
                  required
                  type="email"
                />
              </div>
            </div>

            <div>
              <AppButton buttonState={buttonState} type="submit">
                {t("session.signin.loginLink")}
              </AppButton>
            </div>
          </form>

          {/* could add the "or continue with section" */}
        </div>
      </div>
    </div>
  );
}
