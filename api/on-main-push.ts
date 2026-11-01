import { createHmac, timingSafeEqual } from "node:crypto";
import { GithubPushSchema } from "../schemas/github-push";

const POST = async (request: Request) => {
    const secret = process.env.GITHUB_WEBHOOK_SECRET;

    if (!secret) {
        return Response.json({ error: "App not configured properly" }, { status: 500 });
    }

    const body = await request.text();
    const signature = request.headers.get("x-hub-signature-256");

    if (!signature) {
        return Response.json({ error: "Missing signature" }, { status: 401 });
    }

    const expected = "sha256=" + createHmac("sha256", secret).update(body).digest("hex");

    const givenBuffer = Buffer.from(signature);
    const correctBuffer = Buffer.from(expected);

    if (givenBuffer.length !== correctBuffer.length || !timingSafeEqual(givenBuffer, correctBuffer)) {
        return Response.json({ error: "Invalid signature" }, { status: 401 });
    }

    const parsed = GithubPushSchema.safeParse(JSON.parse(body));

    if (!parsed.success) {
        return Response.json({ error: "Invalid payload" }, { status: 400 });
    }

    const payload = parsed.data;
    if (payload.ref !== "refs/head/main") {
        return Response.json({ message: "Only main pushes are detected" });
    }

    console.log("Main branch pushed to ", "Commit", payload.after);

    return Response.json({ success: true });
}

export { POST };