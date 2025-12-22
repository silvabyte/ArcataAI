import { AppButton, PopoverButton } from "@arcata/components";
import { t } from "@arcata/translate";
import { Popover, Transition } from "@headlessui/react";
import joi from "joi";
import { Fragment, type ReactNode, useEffect } from "react";
import {
  type ActionFunctionArgs,
  type RouteObject,
  redirect,
  useFetcher,
  useNavigation,
} from "react-router-dom";

const schema = joi.object({
  jobUrl: joi.string().uri().required(),
});

const path = "/v1/job-by-url";

export const route: RouteObject = {
  path,
  action,
  loader: () => redirect("/"),
};

export async function action({ request }: ActionFunctionArgs) {
  const formData = await request.formData();
  const updates = Object.fromEntries(formData);
  const { error } = schema.validate(updates);
  if (error?.details[0]?.message) {
    return { success: false, error: error?.details[0]?.message };
  }

  //TODO: add parse job on backend using ai

  return { success: true, error: null, job: null };
}

type AddJobPopoverProps = {
  actuator: ReactNode;
};

type PanelContentProps = {
  close: () => void;
  success: boolean;
  error: string | null;
  nav: ReturnType<typeof useNavigation>;
  fetcher: ReturnType<typeof useFetcher>;
};

function PanelContent({
  close,
  success,
  error,
  nav,
  fetcher,
}: PanelContentProps) {
  useEffect(() => {
    if (success) {
      close();
    }
  }, [success, close]);

  return (
    <div className="w-screen max-w-sm flex-auto rounded-3xl bg-white p-4 text-sm leading-6 shadow-lg ring-1 ring-gray-900/5">
      <fetcher.Form
        action={path}
        className="space-y-6"
        id="add-job-url-submit"
        method="POST"
      >
        <div>
          <label
            className="block font-medium text-gray-900 text-sm leading-6"
            htmlFor="jobUrl"
          >
            {t("pages.hq.inputs.jobUrl.label")}
          </label>
          <div className="mt-2 h-14">
            <input
              className="block w-full rounded-md border-0 px-2 py-1.5 text-gray-900 shadow-sm ring-1 ring-gray-900 ring-inset placeholder:text-gray-400 focus:ring-2 focus:ring-gray-900 focus:ring-inset sm:text-sm sm:leading-6"
              id="job-url-input"
              name="jobUrl"
              placeholder="https://boards.greenhouse.io/arcata/jobs/4305870005"
              required
              type="text"
            />
            {error ? (
              <p className="mt-2 text-red-600 text-sm" id="job-url-error">
                {error}
              </p>
            ) : null}
          </div>
        </div>
        <div className="flex justify-end">
          <div>
            <AppButton
              className="py-1"
              onClick={() => close()}
              theme="link"
              type="button"
            >
              {t("common.actions.cancel")}
            </AppButton>
          </div>
          <div>
            <AppButton
              buttonState={nav.state}
              className="py-1"
              theme="primary_outline"
              type="submit"
            >
              {t("common.actions.add")}
            </AppButton>
          </div>
        </div>
      </fetcher.Form>
    </div>
  );
}

export function AddJobPopover({ actuator }: AddJobPopoverProps) {
  const nav = useNavigation();
  const fetcher = useFetcher();
  const { success, error, job } = fetcher.data || {
    success: false,
    error: null,
    job: null,
  };
  console.log(job);

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
        <Popover.Panel className="absolute left-3/4 z-10 mt-5 flex w-screen max-w-max -translate-x-full px-4">
          {({ close }) => (
            <PanelContent
              close={close}
              error={error}
              fetcher={fetcher}
              nav={nav}
              success={success}
            />
          )}
        </Popover.Panel>
      </Transition>
    </Popover>
  );
}
