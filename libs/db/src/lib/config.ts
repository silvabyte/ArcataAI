import { ui } from "@arcata/envs";
import joi from "joi";

const schema = joi.object({
  supabaseUrl: joi.string().required(),
  supabaseKey: joi.string().required(),
});

const config = {
  supabaseUrl: (ui.get("VITE_SUPABASE_URL") ||
    "https://xjgiioanrwfejqyjhefb.supabase.co") as string,
  supabaseKey: ui.get("VITE_SUPABASE_KEY") as string,
};

const { error } = schema.validate(config);

if (error) {
  if (error?.details[0]?.message) {
    throw new Error(error.details[0].message);
  }
  throw error;
}

export default config;
