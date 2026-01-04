import { Button, Input, Label, useNotification } from "@arcata/components";
import {
  deleteAccount,
  getUserProfile,
  signOut,
  updateUserProfile,
} from "@arcata/db";
import { ui } from "@arcata/envs";
import { t } from "@arcata/translate";
import { Dialog, Listbox, Switch, Transition } from "@headlessui/react";
import {
  BellIcon,
  CheckIcon,
  ChevronUpDownIcon,
  CommandLineIcon,
  ExclamationTriangleIcon,
  MapIcon,
  UserCircleIcon,
} from "@heroicons/react/24/outline";
import type { User } from "@supabase/supabase-js";
import { Fragment, useEffect, useRef, useState } from "react";
import { Link, useRouteLoaderData } from "react-router-dom";

const TIMEZONES = [
  { id: "UTC", name: "UTC" },
  { id: "America/New_York", name: "Eastern Time (US & Canada)" },
  { id: "America/Chicago", name: "Central Time (US & Canada)" },
  { id: "America/Denver", name: "Mountain Time (US & Canada)" },
  { id: "America/Los_Angeles", name: "Pacific Time (US & Canada)" },
  { id: "Europe/London", name: "London" },
  { id: "Europe/Paris", name: "Paris" },
  { id: "Europe/Berlin", name: "Berlin" },
  { id: "Asia/Tokyo", name: "Tokyo" },
  { id: "Asia/Shanghai", name: "Shanghai" },
  { id: "Australia/Sydney", name: "Sydney" },
];

type TimezoneOption = (typeof TIMEZONES)[number];

function classNames(...classes: string[]) {
  return classes.filter(Boolean).join(" ");
}

function SettingsHeader() {
  return (
    <header className="flex items-center justify-between border-gray-200 border-b bg-white px-4 py-3 lg:px-6 lg:py-4">
      <div className="flex min-w-0 flex-1 items-center gap-x-4">
        <h1 className="truncate font-bold text-gray-900 text-xl">
          {t("pages.account.title")}
        </h1>
      </div>
    </header>
  );
}

function AccountTab({
  user,
  onLogout,
  onDelete,
  notify,
}: {
  user: User;
  onLogout: () => void;
  onDelete: () => void;
  notify: (msg: string, type: "success" | "error") => void;
}) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [selectedTimezone, setSelectedTimezone] = useState<TimezoneOption>(
    TIMEZONES[0]
  );
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    async function loadProfile() {
      if (user?.email) {
        setEmail(user.email);
      }
      const { data: profile } = await getUserProfile();
      if (profile) {
        setFirstName(profile.first_name || "");
        setLastName(profile.last_name || "");
        setUsername(profile.username || "");
        const userTimezone = TIMEZONES.find((tz) => tz.id === profile.timezone);
        if (userTimezone) {
          setSelectedTimezone(userTimezone);
        }
      }
    }
    loadProfile();
  }, [user]);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      const { error } = await updateUserProfile({
        firstName,
        lastName,
        username,
        timezone: selectedTimezone.id,
      });
      if (error) {
        notify(t("pages.account.notifications.saveError"), "error");
      } else {
        notify(t("pages.account.notifications.saveSuccess"), "success");
      }
    } catch {
      notify(t("pages.account.notifications.saveError"), "error");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-10">
      <section>
        <div className="mb-6">
          <h2 className="font-semibold text-gray-900 text-lg">
            Personal Information
          </h2>
          <p className="mt-1 text-gray-500 text-sm">
            {t("pages.account.personalInfo.description")}
          </p>
        </div>

        <form className="space-y-6" onSubmit={handleSaveProfile}>
          <div className="flex items-center gap-x-6">
            <div className="flex h-20 w-20 items-center justify-center rounded-lg bg-gray-100 ring-1 ring-gray-200">
              {user?.user_metadata?.avatar_url ? (
                <img
                  alt=""
                  className="h-20 w-20 rounded-lg object-cover"
                  height={80}
                  src={user.user_metadata.avatar_url}
                  width={80}
                />
              ) : (
                <UserCircleIcon className="h-12 w-12 text-gray-400" />
              )}
            </div>
            <div>
              <Button type="button" variant="outline">
                {t("pages.account.personalInfo.changeAvatar")}
              </Button>
              <p className="mt-2 text-gray-500 text-xs">
                JPG, GIF or PNG. 1MB max.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <div>
              <Label htmlFor="firstName">
                {t("pages.account.personalInfo.firstName")}
              </Label>
              <Input
                className="mt-2"
                id="firstName"
                onChange={(e) => setFirstName(e.target.value)}
                value={firstName}
              />
            </div>
            <div>
              <Label htmlFor="lastName">
                {t("pages.account.personalInfo.lastName")}
              </Label>
              <Input
                className="mt-2"
                id="lastName"
                onChange={(e) => setLastName(e.target.value)}
                value={lastName}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="email">
              {t("pages.account.personalInfo.email")}
            </Label>
            <Input
              className="mt-2 bg-gray-50 text-gray-500"
              disabled
              id="email"
              value={email}
            />
          </div>

          <div>
            <Label htmlFor="username">
              {t("pages.account.personalInfo.username")}
            </Label>
            <div className="mt-2 flex rounded-md shadow-sm">
              <span className="inline-flex items-center rounded-l-md border border-gray-300 border-r-0 bg-gray-50 px-3 text-gray-500 sm:text-sm">
                arcata.ai/
              </span>
              <Input
                className="rounded-l-none"
                id="username"
                onChange={(e) => setUsername(e.target.value)}
                value={username}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="timezone">
              {t("pages.account.personalInfo.timezone")}
            </Label>
            <Listbox onChange={setSelectedTimezone} value={selectedTimezone}>
              <div className="relative mt-2">
                <Listbox.Button className="relative w-full cursor-default rounded-md border border-gray-300 bg-white py-2 pr-10 pl-3 text-left shadow-sm focus:border-[#273655] focus:outline-none focus:ring-1 focus:ring-[#273655] sm:text-sm">
                  <span className="block truncate">
                    {selectedTimezone.name}
                  </span>
                  <span className="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-2">
                    <ChevronUpDownIcon
                      aria-hidden="true"
                      className="h-5 w-5 text-gray-400"
                    />
                  </span>
                </Listbox.Button>
                <Transition
                  as={Fragment}
                  leave="transition ease-in duration-100"
                  leaveFrom="opacity-100"
                  leaveTo="opacity-0"
                >
                  <Listbox.Options className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black/5 focus:outline-none sm:text-sm">
                    {TIMEZONES.map((timezone) => (
                      <Listbox.Option
                        className={({ active }) =>
                          classNames(
                            active
                              ? "bg-[#273655] text-white"
                              : "text-gray-900",
                            "relative cursor-default select-none py-2 pr-9 pl-3"
                          )
                        }
                        key={timezone.id}
                        value={timezone}
                      >
                        {({ selected, active }) => (
                          <>
                            <span
                              className={classNames(
                                selected ? "font-semibold" : "font-normal",
                                "block truncate"
                              )}
                            >
                              {timezone.name}
                            </span>
                            {selected ? (
                              <span
                                className={classNames(
                                  active ? "text-white" : "text-[#273655]",
                                  "absolute inset-y-0 right-0 flex items-center pr-4"
                                )}
                              >
                                <CheckIcon
                                  aria-hidden="true"
                                  className="h-5 w-5"
                                />
                              </span>
                            ) : null}
                          </>
                        )}
                      </Listbox.Option>
                    ))}
                  </Listbox.Options>
                </Transition>
              </div>
            </Listbox>
          </div>

          <div className="flex justify-end pt-4">
            <Button
              className="bg-[#273655] text-white hover:bg-[#111826]"
              disabled={isSaving}
              type="submit"
            >
              {isSaving ? "Saving..." : "Save Changes"}
            </Button>
          </div>
        </form>
      </section>

      <div className="border-gray-200 border-t pt-10">
        <h3 className="font-semibold text-base text-gray-900">
          Account Actions
        </h3>
        <div className="mt-6 flex flex-col gap-4 sm:flex-row">
          <Button onClick={onLogout} variant="outline">
            {t("pages.account.logout.button")}
          </Button>
          <Button
            className="border-red-200 text-red-600 hover:bg-red-50"
            onClick={onDelete}
            variant="outline"
          >
            {t("pages.account.deleteAccount.button")}
          </Button>
        </div>
      </div>
    </div>
  );
}

function NotificationsTab() {
  const { notify } = useNotification();
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [jobDigests, setJobDigests] = useState(false);

  const handleSave = () => {
    // TODO: Persist to DB
    notify(t("pages.account.notifications.saveSuccess"), "success");
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="font-semibold text-gray-900 text-lg">Notifications</h2>
        <p className="mt-1 text-gray-500 text-sm">
          Manage how and when we contact you.
        </p>
      </div>

      <div className="space-y-4">
        <Switch.Group as="div" className="flex items-center justify-between">
          <span className="flex grow flex-col">
            <Switch.Label
              as="span"
              className="font-medium text-gray-900 text-sm"
            >
              Application Updates
            </Switch.Label>
            <Switch.Description as="span" className="text-gray-500 text-sm">
              Get notified when the status of a tracked job changes.
            </Switch.Description>
          </span>
          <Switch
            checked={emailAlerts}
            className={classNames(
              emailAlerts ? "bg-[#273655]" : "bg-gray-200",
              "relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-[#273655] focus:ring-offset-2"
            )}
            onChange={setEmailAlerts}
          >
            <span
              aria-hidden="true"
              className={classNames(
                emailAlerts ? "translate-x-5" : "translate-x-0",
                "pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
              )}
            />
          </Switch>
        </Switch.Group>

        <Switch.Group as="div" className="flex items-center justify-between">
          <span className="flex grow flex-col">
            <Switch.Label
              as="span"
              className="font-medium text-gray-900 text-sm"
            >
              Weekly Digest
            </Switch.Label>
            <Switch.Description as="span" className="text-gray-500 text-sm">
              Receive a weekly summary of new jobs matching your profile.
            </Switch.Description>
          </span>
          <Switch
            checked={jobDigests}
            className={classNames(
              jobDigests ? "bg-[#273655]" : "bg-gray-200",
              "relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-[#273655] focus:ring-offset-2"
            )}
            onChange={setJobDigests}
          >
            <span
              aria-hidden="true"
              className={classNames(
                jobDigests ? "translate-x-5" : "translate-x-0",
                "pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
              )}
            />
          </Switch>
        </Switch.Group>
      </div>

      <div className="flex justify-end pt-4">
        <Button
          className="bg-[#273655] text-white hover:bg-[#111826]"
          onClick={handleSave}
          type="button"
        >
          Save Preferences
        </Button>
      </div>
    </div>
  );
}

function IntegrationsTab() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="font-semibold text-gray-900 text-lg">Integrations</h2>
        <p className="mt-1 text-gray-500 text-sm">
          Connect with third-party tools to supercharge your workflow.
        </p>
      </div>

      {/* Roadmap Callout */}
      <div className="rounded-md bg-blue-50 p-4">
        <div className="flex">
          <div className="shrink-0">
            <MapIcon aria-hidden="true" className="h-5 w-5 text-blue-400" />
          </div>
          <div className="ml-3 flex-1 md:flex md:justify-between">
            <p className="text-blue-700 text-sm">
              These features are currently in development. Check our{" "}
              <Link
                className="whitespace-nowrap font-medium text-blue-700 hover:text-blue-600"
                to="/roadmap"
              >
                Product Roadmap
                <span aria-hidden="true"> &rarr;</span>
              </Link>
            </p>
          </div>
        </div>
      </div>

      <ul className="divide-y divide-gray-100 rounded-md border border-gray-200">
        <li className="flex items-center justify-between gap-x-6 px-4 py-5 sm:px-6">
          <div className="min-w-0">
            <div className="flex items-start gap-x-3">
              <p className="font-semibold text-gray-900 text-sm leading-6">
                Google / Gmail
              </p>
              <p className="rounded-md bg-gray-50 px-1.5 py-0.5 font-medium text-gray-600 text-xs ring-1 ring-gray-500/10 ring-inset">
                Planned
              </p>
            </div>
            <div className="mt-1 flex items-center gap-x-2 text-xs leading-5">
              <p className="text-gray-500">
                Sync email conversations with recruiters automatically.
              </p>
            </div>
          </div>
          <div className="flex flex-none items-center gap-x-4">
            <Button disabled variant="outline">
              Connect
            </Button>
          </div>
        </li>
        <li className="flex items-center justify-between gap-x-6 px-4 py-5 sm:px-6">
          <div className="min-w-0">
            <div className="flex items-start gap-x-3">
              <p className="font-semibold text-gray-900 text-sm leading-6">
                Chrome Extension
              </p>
              <p className="rounded-md bg-gray-50 px-1.5 py-0.5 font-medium text-gray-600 text-xs ring-1 ring-gray-500/10 ring-inset">
                Planned
              </p>
            </div>
            <div className="mt-1 flex items-center gap-x-2 text-xs leading-5">
              <p className="text-gray-500">
                Clip jobs from LinkedIn, Indeed, and more.
              </p>
            </div>
          </div>
          <div className="flex flex-none items-center gap-x-4">
            <Button disabled variant="outline">
              Install
            </Button>
          </div>
        </li>
      </ul>
    </div>
  );
}

export default function AccountSettings() {
  const { user } = useRouteLoaderData("root") as { user: User };
  const { notify } = useNotification();
  const [currentTab, setCurrentTab] = useState("account");

  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  const tabs = [
    { id: "account", name: "Account", icon: UserCircleIcon },
    { id: "notifications", name: "Notifications", icon: BellIcon },
    { id: "integrations", name: "Integrations", icon: CommandLineIcon },
  ];

  const handleLogout = async () => {
    try {
      const { error } = await signOut();
      if (error) {
        notify(t("pages.account.notifications.logoutError"), "error");
      } else {
        const loginUrl = ui.get("VITE_AUTH_BASE_URL");
        window.location.href = loginUrl;
      }
    } catch {
      notify(t("pages.account.notifications.logoutError"), "error");
    }
  };

  const handleConfirmDelete = async () => {
    setIsDeleting(true);
    try {
      const { error } = await deleteAccount();
      if (error) {
        notify(t("pages.account.notifications.deleteError"), "error");
        setIsDeleting(false);
      } else {
        const loginUrl = ui.get("VITE_AUTH_BASE_URL");
        window.location.href = loginUrl;
      }
    } catch {
      notify(t("pages.account.notifications.deleteError"), "error");
      setIsDeleting(false);
    }
  };

  return (
    <div className="flex h-full flex-col overflow-hidden bg-gray-50">
      <SettingsHeader />

      <main className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div className="lg:grid lg:grid-cols-12 lg:gap-x-8">
            {/* Sidebar Navigation */}
            <aside className="py-6 lg:col-span-3">
              <nav className="space-y-1">
                {tabs.map((tab) => (
                  <button
                    className={classNames(
                      currentTab === tab.id
                        ? "bg-white text-[#273655] shadow-sm"
                        : "text-gray-700 hover:bg-white hover:text-[#273655] hover:shadow-sm",
                      "group flex w-full items-center rounded-md px-3 py-2 font-medium text-sm transition-all"
                    )}
                    key={tab.id}
                    onClick={() => setCurrentTab(tab.id)}
                    type="button"
                  >
                    <tab.icon
                      aria-hidden="true"
                      className={classNames(
                        currentTab === tab.id
                          ? "text-[#273655]"
                          : "text-gray-400 group-hover:text-[#273655]",
                        "mr-3 -ml-1 h-6 w-6 shrink-0"
                      )}
                    />
                    <span className="truncate">{tab.name}</span>
                  </button>
                ))}
              </nav>
            </aside>

            {/* Main Content */}
            <div className="lg:col-span-9">
              <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm sm:p-8">
                {currentTab === "account" && (
                  <AccountTab
                    notify={notify}
                    onDelete={() => setIsDeleteDialogOpen(true)}
                    onLogout={handleLogout}
                    user={user}
                  />
                )}
                {currentTab === "notifications" && <NotificationsTab />}
                {currentTab === "integrations" && <IntegrationsTab />}
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Delete Confirmation Modal */}
      <Transition.Root as={Fragment} show={isDeleteDialogOpen}>
        <Dialog
          as="div"
          className="relative z-50"
          initialFocus={cancelButtonRef}
          onClose={setIsDeleteDialogOpen}
        >
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="ease-in duration-200"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <div className="fixed inset-0 bg-gray-500/75 transition-opacity" />
          </Transition.Child>

          <div className="fixed inset-0 z-10 w-screen overflow-y-auto">
            <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
              <Transition.Child
                as={Fragment}
                enter="ease-out duration-300"
                enterFrom="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                enterTo="opacity-100 translate-y-0 sm:scale-100"
                leave="ease-in duration-200"
                leaveFrom="opacity-100 translate-y-0 sm:scale-100"
                leaveTo="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
              >
                <Dialog.Panel className="relative transform overflow-hidden rounded-lg bg-white px-4 pt-5 pb-4 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
                  <div className="sm:flex sm:items-start">
                    <div className="mx-auto flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10">
                      <ExclamationTriangleIcon
                        aria-hidden="true"
                        className="h-6 w-6 text-red-600"
                      />
                    </div>
                    <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                      <Dialog.Title
                        as="h3"
                        className="font-semibold text-base text-gray-900"
                      >
                        {t("pages.account.deleteDialog.title")}
                      </Dialog.Title>
                      <div className="mt-2">
                        <p className="text-gray-500 text-sm">
                          {t("pages.account.deleteDialog.description")}
                        </p>
                      </div>
                    </div>
                  </div>
                  <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                    <Button
                      className="inline-flex w-full justify-center bg-red-600 text-white hover:bg-red-500 sm:ml-3 sm:w-auto"
                      disabled={isDeleting}
                      onClick={handleConfirmDelete}
                      type="button"
                    >
                      {isDeleting
                        ? t("pages.account.deleteDialog.deleting")
                        : t("pages.account.deleteDialog.confirm")}
                    </Button>
                    <Button
                      className="mt-3 inline-flex w-full justify-center sm:mt-0 sm:w-auto"
                      disabled={isDeleting}
                      onClick={() => setIsDeleteDialogOpen(false)}
                      ref={cancelButtonRef}
                      type="button"
                      variant="outline"
                    >
                      {t("pages.account.deleteDialog.cancel")}
                    </Button>
                  </div>
                </Dialog.Panel>
              </Transition.Child>
            </div>
          </div>
        </Dialog>
      </Transition.Root>
    </div>
  );
}
