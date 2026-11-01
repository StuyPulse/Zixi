import { z } from "zod";

const GithubPushSchema = z.object({
  ref: z.string(),
  before: z.string(),
  after: z.string(),

  repository: z.object({
    id: z.number(),
    name: z.string(),
    full_name: z.string(),
  }),

  pusher: z.object({
    name: z.string(),
    email: z.string(),
  }),

  forced: z.boolean(),

  commits: z.array(
    z.object({
      id: z.string(),
      message: z.string(),
      url: z.string(),
      added: z.array(z.string()),
      removed: z.array(z.string()),
      modified: z.array(z.string()),
    })
  ),
});

export type GithubPushPayload = z.infer<typeof GithubPushSchema>;
export { GithubPushSchema };
