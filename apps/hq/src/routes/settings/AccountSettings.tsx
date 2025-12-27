import { Button, Input, Label } from "@arcata/components";
import { t } from "@arcata/translate";
import { Listbox, Transition } from "@headlessui/react";
import {
  CheckIcon,
  ChevronUpDownIcon,
  UserCircleIcon,
} from "@heroicons/react/24/outline";
import { Fragment, useState } from "react";

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

export default function AccountSettings() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [selectedTimezone, setSelectedTimezone] = useState<TimezoneOption>(
    TIMEZONES[0]
  );

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement save profile logic
    console.log("Save profile", {
      firstName,
      lastName,
      email,
      username,
      timezone: selectedTimezone.id,
    });
  };

  const handleLogout = () => {
    // TODO: Implement logout logic
    console.log("Logout");
  };

  const handleDeleteAccount = () => {
    // TODO: Implement delete account logic with confirmation
    console.log("Delete account");
  };

  return (
    <div className="mx-auto max-w-2xl px-6 py-8">
      {/* Page Header */}
      <p className="text-gray-500 text-sm">{t("pages.account.subtitle")}</p>

      {/* Personal Information Section */}
      <section className="mt-8">
        <p className="text-gray-500 text-sm">
          {t("pages.account.personalInfo.description")}
        </p>

        <form className="mt-6 space-y-5" onSubmit={handleSaveProfile}>
          {/* Avatar */}
          <div className="flex items-center gap-x-6">
            <div className="flex h-20 w-20 items-center justify-center rounded-lg bg-gray-100">
              <UserCircleIcon className="h-16 w-16 text-gray-400" />
            </div>
            <div>
              <Button type="button" variant="outline">
                {t("pages.account.personalInfo.changeAvatar")}
              </Button>
              <p className="mt-2 text-gray-400 text-xs">
                {t("pages.account.personalInfo.avatarHint")}
              </p>
            </div>
          </div>

          {/* Name Fields - Side by Side */}
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
            <div>
              <Label htmlFor="firstName">
                {t("pages.account.personalInfo.firstName")}
              </Label>
              <Input
                className="mt-1.5"
                id="firstName"
                onChange={(e) => setFirstName(e.target.value)}
                placeholder={t(
                  "pages.account.personalInfo.firstNamePlaceholder"
                )}
                type="text"
                value={firstName}
              />
            </div>
            <div>
              <Label htmlFor="lastName">
                {t("pages.account.personalInfo.lastName")}
              </Label>
              <Input
                className="mt-1.5"
                id="lastName"
                onChange={(e) => setLastName(e.target.value)}
                placeholder={t(
                  "pages.account.personalInfo.lastNamePlaceholder"
                )}
                type="text"
                value={lastName}
              />
            </div>
          </div>

          {/* Email */}
          <div>
            <Label htmlFor="email">
              {t("pages.account.personalInfo.email")}
            </Label>
            <Input
              className="mt-1.5"
              id="email"
              onChange={(e) => setEmail(e.target.value)}
              placeholder={t("pages.account.personalInfo.emailPlaceholder")}
              type="email"
              value={email}
            />
          </div>

          {/* Username */}
          <div>
            <Label htmlFor="username">
              {t("pages.account.personalInfo.username")}
            </Label>
            <div className="mt-1.5 flex rounded-md">
              <span className="inline-flex items-center rounded-l-md border border-gray-200 border-r-0 bg-gray-50 px-3 text-gray-500 text-sm">
                arcata.ai/
              </span>
              <Input
                className="rounded-l-none"
                id="username"
                onChange={(e) => setUsername(e.target.value)}
                placeholder={t(
                  "pages.account.personalInfo.usernamePlaceholder"
                )}
                type="text"
                value={username}
              />
            </div>
          </div>

          {/* Timezone */}
          <div>
            <Label htmlFor="timezone">
              {t("pages.account.personalInfo.timezone")}
            </Label>
            <Listbox onChange={setSelectedTimezone} value={selectedTimezone}>
              <div className="relative mt-1.5">
                <Listbox.Button className="relative w-full cursor-default rounded-md border border-gray-200 bg-white py-2 pr-10 pl-3 text-left text-gray-900 text-sm focus:border-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-900">
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
                  <Listbox.Options className="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-sm shadow-lg ring-1 ring-black/5 focus:outline-none">
                    {TIMEZONES.map((timezone) => (
                      <Listbox.Option
                        className={({ active }) =>
                          classNames(
                            active ? "bg-gray-900 text-white" : "text-gray-900",
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
                                selected ? "font-medium" : "font-normal",
                                "block truncate"
                              )}
                            >
                              {timezone.name}
                            </span>
                            {selected ? (
                              <span
                                className={classNames(
                                  active ? "text-white" : "text-gray-900",
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

          {/* Save Button */}
          <div className="flex pt-2">
            <Button
              className="bg-[#273655] text-white hover:bg-[#111826]"
              type="submit"
            >
              {t("pages.account.personalInfo.save")}
            </Button>
          </div>
        </form>
      </section>

      {/* Divider */}
      <div className="my-10 border-gray-100 border-t" />

      {/* Log Out Section */}
      <section>
        <p className="text-gray-500 text-sm">
          {t("pages.account.logout.description")}
        </p>
        <div className="mt-4">
          <Button onClick={handleLogout} type="button" variant="outline">
            {t("pages.account.logout.button")}
          </Button>
        </div>
      </section>

      {/* Divider */}
      <div className="my-10 border-gray-100 border-t" />

      {/* Delete Account Section */}
      <section>
        <h2 className="font-semibold text-red-600 text-sm">
          {t("pages.account.deleteAccount.title")}
        </h2>
        <p className="mt-1 text-gray-500 text-sm">
          {t("pages.account.deleteAccount.description")}
        </p>
        <div className="mt-4">
          <Button
            className="border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700"
            onClick={handleDeleteAccount}
            type="button"
            variant="outline"
          >
            {t("pages.account.deleteAccount.button")}
          </Button>
        </div>
      </section>
    </div>
  );
}
