import i18next from "./i18n";

type Config = {
  loadPath: string;
  crossDomain: boolean;
};

export const load = async (config: Config) => {
  const { loadPath, crossDomain } = config;
  await i18next.init({
    fallbackLng: "en",
    load: "languageOnly", // en vs en-US
    // React already does escaping
    interpolation: {
      escapeValue: false,
    },
    debug: false,
    backend: {
      loadPath,
      crossDomain,
    },
  });
  return i18next;
};

export const t = (key: string, options?: Record<string, unknown>) =>
  i18next.t(key, options) as string;
