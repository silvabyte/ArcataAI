import { Logo } from "@arcata/components";
import { t } from "@arcata/translate";

export default function SessionVerify() {
  return (
    <div className="flex min-h-full flex-1 flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="flex justify-center">
          <Logo sizeFactor={2} />
        </div>
        <h2 className="mt-6 text-center font-bold text-2xl text-white leading-9 tracking-tight">
          {t("session.verify.emailSent")}
        </h2>
      </div>

      <div className="mt-10 sm:mx-auto sm:w-full sm:max-w-[480px]">
        <div className="bg-white px-6 py-12 shadow sm:rounded-lg sm:px-12">
          <p className="mb-4">{t("session.verify.checkInbox")}</p>
          <p>{t("session.verify.spam")}</p>
        </div>
      </div>
    </div>
  );
}
