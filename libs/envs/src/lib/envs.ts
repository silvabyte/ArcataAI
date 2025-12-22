export function envs(): string {
  return "envs";
}
export const server = {
  get: (key: string) => process.env[key],
};

export const ui = {
  get: (key: string) => import.meta.env[key],
};
