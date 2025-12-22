/* eslint-disable @typescript-eslint/no-explicit-any */
import db from "./client";

export type ResourceName =
  | "companies"
  | "jobs"
  | "profiles"
  | "job_profiles"
  | "application_statuses"
  | "job_applications"
  | "conversations"
  | "job_stream"
  | "application_answers";

type GenerateResourceApiArg = {
  table: ResourceName;
  id_field: string;
};

type EqArg<T = Record<string, unknown>> = {
  key: keyof T;
  value: T[keyof T];
};
type IsArg<T = Record<string, unknown>> = {
  key: keyof T;
  value: T[keyof T];
};

type NeqArg<T = Record<string, unknown>> = {
  key: keyof T;
  value: T[keyof T];
};

type OrderArg<T = Record<string, unknown>> = {
  key: keyof T;
  meta?: Record<string, unknown>;
};

type RangeArg = {
  start: number;
  end: number;
};

type ListArgs<T = Record<string, unknown>> = {
  select?: string;
  order?: OrderArg<T>;
  range?: RangeArg;
  limit?: number;
  eq?: EqArg<T>;
  is?: IsArg<T>;
  neq?: NeqArg<T>;
  withCount?: boolean;
};

type GetArgs = {
  id?: string | number;
  select?: string;
};

type GetByArgs<T = Record<string, unknown>> = {
  field?: keyof T;
  fieldValue?: T[keyof T];
  select?: string;
};

export default function generateResourceApi({
  table,
  id_field,
}: GenerateResourceApiArg) {
  function applyFilters<T>(
    // biome-ignore lint/suspicious/noExplicitAny: PostgrestQueryBuilder type is too complex
    query: any,
    filters: Pick<
      ListArgs<T>,
      "eq" | "is" | "neq" | "order" | "range" | "limit"
    >
  ) {
    let result = query;

    if (filters.eq) {
      result = result.eq(filters.eq.key as string, filters.eq.value);
    }
    if (filters.is) {
      result = result.is(filters.is.key as string, filters.is.value);
    }
    if (filters.neq) {
      result = result.neq(filters.neq.key as string, filters.neq.value);
    }
    if (filters.order) {
      result = result.order(filters.order.key as string, filters.order.meta);
    }
    if (filters.range) {
      result = result.range(filters.range.start, filters.range.end);
    }
    if (filters.limit) {
      result = result.limit(filters.limit);
    }

    return result;
  }

  async function list<T = Record<string, unknown>>({
    select = "*",
    order,
    range,
    limit,
    is,
    eq,
    neq,
    withCount,
  }: ListArgs = {}) {
    const baseQuery = withCount
      ? db.from(table).select(select, { count: "exact", head: true })
      : db.from(table).select(select);

    const query = applyFilters(baseQuery, { eq, is, neq, order, range, limit });

    const { data, error, count } = await query;
    if (error) {
      throw error;
    }

    return withCount ? ({ count } as T) : (data as T[]);
  }

  async function get<T = Record<string, unknown>>({
    id,
    select = "*",
  }: GetArgs = {}) {
    const { data, error } = await db
      .from(table)
      .select(select)
      .eq(id_field, id as string)
      .limit(1)
      .single();
    if (error) {
      throw error;
    }
    return data as T;
  }

  async function getBy<T = Record<string, unknown>>({
    field,
    fieldValue,
    select = "*",
  }: GetByArgs = {}) {
    const { data, error } = await db
      .from(table)
      .select(select)
      .eq(field as string, fieldValue as string)
      .limit(1)
      .single();
    if (error) {
      throw error;
    }
    return data as T;
  }

  async function create<T = Record<string, unknown>>(
    resource: Record<string, unknown>
  ) {
    const { data, error } = await db.from(table).insert([resource]).select();
    if (error) {
      throw error;
    }
    return data as T;
  }

  async function createMany<T = Record<string, unknown>>(
    resources: Record<string, unknown>[]
  ) {
    const { data, error } = await db
      .from(table)
      .insert([...resources])
      .select();
    if (error) {
      throw error;
    }
    return data as T;
  }

  async function update<T = Record<string, unknown>>(
    id: string | number,
    partialResource: Partial<Record<string, unknown>>
  ) {
    const { data, error } = await db
      .from(table)
      .update(partialResource)
      .eq(id_field, id)
      .select();
    if (error) {
      throw error;
    }
    return data as T;
  }

  async function remove(id: string | number) {
    const { error } = await db.from(table).delete().eq(id_field, id);
    if (error) {
      throw error;
    }
  }

  const api = {
    list,
    get,
    getBy,
    create,
    createMany,
    update,
    remove,
  };
  return api;
}
