import generateResourceApi, { type ResourceName } from "./generateResourceApi";

const resources: { table: ResourceName; id_field: string }[] = [
  { table: "companies", id_field: "company_id" },
  { table: "jobs", id_field: "job_id" },
  { table: "profiles", id_field: "id" },
  { table: "job_profiles", id_field: "job_profile_id" },
  { table: "application_statuses", id_field: "status_id" },
  { table: "job_applications", id_field: "application_id" },
  { table: "conversations", id_field: "conversation_id" },
  { table: "job_stream", id_field: "stream_id" },
  { table: "application_answers", id_field: "answer_id" },
];

type DB = {
  [key in ResourceName]: ReturnType<typeof generateResourceApi>;
};

export const db: DB = {
  companies: undefined as unknown as ReturnType<typeof generateResourceApi>,
  jobs: undefined as unknown as ReturnType<typeof generateResourceApi>,
  profiles: undefined as unknown as ReturnType<typeof generateResourceApi>,
  job_profiles: undefined as unknown as ReturnType<typeof generateResourceApi>,
  application_statuses: undefined as unknown as ReturnType<
    typeof generateResourceApi
  >,
  job_applications: undefined as unknown as ReturnType<
    typeof generateResourceApi
  >,
  conversations: undefined as unknown as ReturnType<typeof generateResourceApi>,
  job_stream: undefined as unknown as ReturnType<typeof generateResourceApi>,
  application_answers: undefined as unknown as ReturnType<
    typeof generateResourceApi
  >,
};

for (const resource of resources) {
  db[resource.table] = generateResourceApi(resource);
}
