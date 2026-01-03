import { AppButton, PopoverButton } from "@arcata/components";
import { t } from "@arcata/translate";
import { Popover, Transition } from "@headlessui/react";
import joi from "joi";
import { Fragment, type ReactNode, useState } from "react";

const schema = joi.object({
  jobUrl: joi.string().uri().required(),
});

type AddJobPopoverProps = {
  actuator: ReactNode;
  onSubmit: (url: string) => void;
  disabled?: boolean;
};

export function AddJobPopover({
  actuator,
  onSubmit,
  disabled,
}: AddJobPopoverProps) {
  const [url, setUrl] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (close: () => void) => {
    const { error: validationError } = schema.validate({ jobUrl: url });
    if (validationError?.details[0]?.message) {
      setError(validationError.details[0].message);
      return;
    }

    setError(null);
    onSubmit(url);
    setUrl(""); // Reset for next time
    close();
  };

  return (
    <Popover className="relative">
      <PopoverButton
        className="inline-flex items-center gap-x-1 font-semibold text-gray-900 text-sm leading-6"
        theme="brand"
      >
        {actuator}
      </PopoverButton>

      <Transition
        as={Fragment}
        enter="transition ease-out duration-200"
        enterFrom="opacity-0 translate-y-1"
        enterTo="opacity-100 translate-y-0"
        leave="transition ease-in duration-150"
        leaveFrom="opacity-100 translate-y-0"
        leaveTo="opacity-0 translate-y-1"
      >
        <Popover.Panel className="fixed top-16 right-4 z-50 w-[min(24rem,calc(100vw-2.5rem))] px-4 sm:top-20 sm:right-6 sm:px-0">
          {({ close }) => (
            <div className="w-screen max-w-sm flex-auto rounded-3xl bg-white p-4 text-sm leading-6 shadow-lg ring-1 ring-gray-900/5">
              <div className="space-y-6">
                <div>
                  <label
                    className="block font-medium text-gray-900 text-sm leading-6"
                    htmlFor="job-url-input"
                  >
                    {t("pages.hq.inputs.jobUrl.label")}
                  </label>
                  <div className="mt-2 h-14">
                    <input
                      className="block w-full rounded-md border-0 px-2 py-1.5 text-gray-900 shadow-sm ring-1 ring-gray-900 ring-inset placeholder:text-gray-400 focus:ring-2 focus:ring-gray-900 focus:ring-inset disabled:cursor-not-allowed disabled:bg-gray-100 sm:text-sm sm:leading-6"
                      disabled={disabled}
                      id="job-url-input"
                      name="jobUrl"
                      onChange={(e) => setUrl(e.target.value)}
                      placeholder="https://boards.greenhouse.io/company/jobs/12345"
                      required
                      type="text"
                      value={url}
                    />
                    {error ? (
                      <p
                        className="mt-2 text-red-600 text-sm"
                        id="job-url-error"
                      >
                        {error}
                      </p>
                    ) : null}
                  </div>
                </div>
                <div className="flex justify-end">
                  <AppButton
                    className="py-1"
                    disabled={disabled}
                    onClick={() => close()}
                    theme="link"
                    type="button"
                  >
                    {t("common.actions.cancel")}
                  </AppButton>
                  <AppButton
                    className="py-1"
                    disabled={disabled}
                    onClick={() => handleSubmit(close)}
                    theme="primary_outline"
                    type="button"
                  >
                    {t("common.actions.add")}
                  </AppButton>
                </div>
              </div>
            </div>
          )}
        </Popover.Panel>
      </Transition>
    </Popover>
  );
}
