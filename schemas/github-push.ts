import { z } from "zod";

const CommitSchema = z.object({
    id: z.string(),
    message: z.string(),
    url: z.string(),
    added: z.array(z.string()),
    removed: z.array(z.string()),
    modified: z.array(z.string()),
    tree_id: z.string().optional(),
    distinct: z.boolean().optional(),
    timestamp: z.string().optional(),
    author: z.object({
            name: z.string(),
            email: z.string(),
            date: z.string().optional(),
            username: z.string().optional(),
        }).optional(),
    committer: z.object({
        name: z.string(),
        email: z.string(),
        date: z.string().optional(),
        username: z.string().optional(),
    }).optional(),
});

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

    commits: z.array(CommitSchema),

    head_commit: CommitSchema.nullable(),
});

export type GithubPushPayload = z.infer<typeof GithubPushSchema>;
export { GithubPushSchema };