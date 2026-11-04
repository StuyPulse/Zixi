import { Octokit } from "@octokit/rest";

interface MainPushData {
    message: string;
    hash: string;
    updatedAt: string;
}

interface UpdateMainPushOptions {
    message: string;
    hash: string;
    automatedMessage?: string;
}

const updateMainPush = async ({ message, hash, automatedMessage }: UpdateMainPushOptions) => {
    const octokit = new Octokit({
        auth: process.env.GITHUB_TOKEN,
    });

    const owner = "StuyPulse";
    const repo = "Zixi";
    const path = ".automation/main-push.json";
    const branch = "docs";

    let sha: string | undefined;

    try {
        const { data } = await octokit.repos.getContent({
            owner,
            repo,
            path,
            ref: branch,
        });

        if (!Array.isArray(data) && data.type === "file") {
            sha = data.sha;
        }
    } catch (error: any) {
        // File does not exist
    }

    const updatedData: MainPushData = {
        message,
        hash,
        updatedAt: new Date().toISOString(),
    };

    const response = await octokit.repos.createOrUpdateFileContents({
        owner,
        repo,
        path,
        message: automatedMessage ?? `chore: [AUTOMATED] update main push info for commit ${hash.substring(0, 7)}`,
        content: Buffer.from(
            JSON.stringify(updatedData, null, 2)
        ).toString("base64"),
        sha,
        branch,
    });

    return response.data.content?.sha;
};

export type { MainPushData, UpdateMainPushOptions };
export { updateMainPush };