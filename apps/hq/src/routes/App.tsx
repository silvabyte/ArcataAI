import {
  AppErrorOutlet,
  BoardIcon,
  BottomTabNav,
  classNames,
  FiyaIcon,
  Logo,
  type NavItem,
} from "@arcata/components";
import { getCurrentUser } from "@arcata/db";
import { ui } from "@arcata/envs";
import { t } from "@arcata/translate";
import { Dialog, Transition } from "@headlessui/react";
import {
  Bars3Icon,
  RocketLaunchIcon,
  UserCircleIcon,
  XMarkIcon,
} from "@heroicons/react/24/outline";
import { Fragment, useState } from "react";
import {
  Outlet,
  type RouteObject,
  redirect,
  useLocation,
  useNavigate,
} from "react-router-dom";
import HQ, { loader as hqLoader } from "./hq/HQ";
import { JobStreamPage, jobStreamLoader } from "./job-stream";
import { route as addJobByUrlRoute } from "./jobs/AddJobByUrl";

export const route: RouteObject = {
  path: "/",
  element: <App />,
  children: [
    {
      errorElement: <AppErrorOutlet />,
      children: [
        { index: true, element: <HQ />, loader: hqLoader },
        {
          path: "job-stream",
          element: <JobStreamPage />,
          loader: jobStreamLoader,
        },
        addJobByUrlRoute,
      ],
    },
  ],
  errorElement: <AppErrorOutlet />,
  loader: async () => {
    const user = await getCurrentUser();
    if (!user) {
      const loginUrl = ui.get("VITE_AUTH_BASE_URL");
      return redirect(loginUrl);
    }
    return { user };
  },
};

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();

  // Desktop sidebar navigation
  const navigation = [
    {
      name: t("nav.hq"),
      href: "/",
      icon: RocketLaunchIcon,
      current: location.pathname === "/",
    },
    {
      name: t("nav.jobList"),
      href: "/job-stream",
      icon: BoardIcon,
      current: location.pathname === "/job-stream",
    },
    // {
    //   name: t("nav.rankings"),
    //   href: "#",
    //   icon: FolderIcon,
    //   current: false,
    // },
    {
      name: t("nav.profile"),
      href: "#",
      icon: UserCircleIcon,
      current: false,
    },
    {
      name: t("nav.autoApplication"),
      href: "#",
      icon: FiyaIcon,
      current: false,
    },
  ];

  // Mobile bottom navigation - curated subset
  const mobileNavItems: NavItem[] = [
    {
      name: t("nav.trackerHQ"),
      href: "/",
      icon: RocketLaunchIcon,
      current: location.pathname === "/",
    },

    {
      name: t("nav.jobList"),
      href: "/job-stream",
      icon: BoardIcon,
      current: location.pathname === "/job-stream",
    },
    // {
    //   name: t("nav.rankings"),
    //   href: "#",
    //   icon: FolderIcon,
    //   current: false,
    // },
    {
      name: t("nav.autoApply"),
      href: "#",
      icon: FiyaIcon,
      current: false,
    },
    {
      name: t("nav.profile"),
      href: "#",
      icon: UserCircleIcon,
      current: false,
    },
  ];

  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleMobileNavigate = (href: string) => {
    if (href !== "#") {
      navigate(href);
    }
  };

  return (
    <div>
      <Transition.Root as={Fragment} show={sidebarOpen}>
        <Dialog
          as="div"
          className="relative z-50 lg:hidden"
          onClose={setSidebarOpen}
        >
          <Transition.Child
            as={Fragment}
            enter="transition-opacity ease-linear duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="transition-opacity ease-linear duration-300"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <div className="fixed inset-0 bg-gray-900/80" />
          </Transition.Child>

          <div className="fixed inset-0 flex">
            <Transition.Child
              as={Fragment}
              enter="transition ease-in-out duration-300 transform"
              enterFrom="-translate-x-full"
              enterTo="translate-x-0"
              leave="transition ease-in-out duration-300 transform"
              leaveFrom="translate-x-0"
              leaveTo="-translate-x-full"
            >
              <Dialog.Panel className="relative mr-16 flex w-full max-w-xs flex-1">
                <Transition.Child
                  as={Fragment}
                  enter="ease-in-out duration-300"
                  enterFrom="opacity-0"
                  enterTo="opacity-100"
                  leave="ease-in-out duration-300"
                  leaveFrom="opacity-100"
                  leaveTo="opacity-0"
                >
                  <div className="absolute top-0 left-full flex w-16 justify-center pt-5">
                    <button
                      className="-m-2.5 p-2.5"
                      onClick={() => setSidebarOpen(false)}
                      type="button"
                    >
                      <span className="sr-only">{t("sidebar.close")}</span>
                      <XMarkIcon
                        aria-hidden="true"
                        className="h-6 w-6 text-white"
                      />
                    </button>
                  </div>
                </Transition.Child>
                {/* Sidebar component, swap this element with another sidebar if you like */}
                <div className="flex grow flex-col gap-y-5 overflow-y-auto bg-black px-6 pb-2 ring-1 ring-white/10">
                  <div className="flex h-16 shrink-0 items-center">
                    <Logo />
                  </div>
                  <nav className="flex flex-1 flex-col">
                    <ul className="flex flex-1 flex-col gap-y-7">
                      <li>
                        <ul className="-mx-2 space-y-1">
                          {navigation.map((item) => (
                            <li key={item.name}>
                              <a
                                className={classNames(
                                  item.current
                                    ? "bg-gray-800 text-white"
                                    : "text-gray-400 hover:bg-gray-800 hover:text-white",
                                  "group flex gap-x-3 rounded-md p-2 font-semibold text-sm leading-6"
                                )}
                                href={item.href}
                              >
                                <item.icon
                                  aria-hidden="true"
                                  className="h-6 w-6 shrink-0"
                                />
                                {item.name}
                              </a>
                            </li>
                          ))}
                        </ul>
                      </li>
                    </ul>
                  </nav>
                </div>
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </Dialog>
      </Transition.Root>

      {/* Static sidebar for desktop */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-72 lg:flex-col">
        {/* Sidebar component, swap this element with another sidebar if you like */}
        <div className="flex grow flex-col gap-y-5 overflow-y-auto bg-black px-6">
          <div className="flex h-16 shrink-0 items-center">
            <Logo />
          </div>
          <nav className="flex flex-1 flex-col">
            <ul className="flex flex-1 flex-col gap-y-7">
              <li>
                <ul className="-mx-2 space-y-1">
                  {navigation.map((item) => (
                    <li key={item.name}>
                      <a
                        className={classNames(
                          item.current
                            ? "bg-gray-800 text-white"
                            : "text-gray-400 hover:bg-gray-800 hover:text-white",
                          "group flex gap-x-3 rounded-md p-2 font-semibold text-sm leading-6"
                        )}
                        href={item.href}
                      >
                        <item.icon
                          aria-hidden="true"
                          className="h-6 w-6 shrink-0"
                        />
                        {item.name}
                      </a>
                    </li>
                  ))}
                </ul>
              </li>
              <li className="-mx-6 mt-auto">
                <button
                  className="flex w-full items-center gap-x-3 px-6 py-3 font-semibold text-sm text-white leading-6 hover:bg-gray-800"
                  type="button"
                >
                  <UserCircleIcon className="h-6 w-6" />
                  <span className="sr-only">
                    {t("pages.profile.yourProfile")}
                  </span>
                  <span aria-hidden="true">
                    {t("pages.profile.yourProfile")}
                  </span>
                </button>
              </li>
            </ul>
          </nav>
        </div>
      </div>

      {/* Mobile header - hidden when using bottom nav */}
      <div className="sticky top-0 z-40 hidden items-center gap-x-6 bg-black px-4 py-4 shadow-sm sm:px-6">
        <button
          className="-m-2.5 p-2.5 text-gray-400 lg:hidden"
          onClick={() => setSidebarOpen(true)}
          type="button"
        >
          <span className="sr-only">{t("sidebar.open")}</span>
          <Bars3Icon aria-hidden="true" className="h-6 w-6" />
        </button>
        <div className="flex-1 font-semibold text-sm text-white leading-6">
          {t("dashboard.title")}
        </div>
        <button type="button">
          <span className="sr-only">{t("pages.profile.yourProfile")}</span>
          <UserCircleIcon className="h-6 w-6" />
        </button>
      </div>

      <main className="pb-16 lg:pb-0 lg:pl-72">
        <Outlet />
      </main>

      {/* Mobile bottom navigation */}
      <div className="lg:hidden">
        <BottomTabNav
          items={mobileNavItems}
          onNavigate={handleMobileNavigate}
        />
      </div>
    </div>
  );
}
