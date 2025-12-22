import i18next from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import XHR from "i18next-xhr-backend";

i18next.use(XHR).use(LanguageDetector);

export default i18next;
