import type { VercelRequest, VercelResponse } from '@vercel/node';
import { timingSafeEqual } from "node:crypto";

import { updateMainPush } from "../lib/update-main-push.js";

const handler = async (req: VercelRequest, res: VercelResponse) => {
    if (req.method !== "POST") {
        return res.status(405).json({ error: "Only POST requests are allowed" });
    }

    const password = process.env.MANUAL_RETRIGGER_PASSWORD;
    if (!password) {
        return res.status(500).json({ error: "Manual retrigger password not set" });
    }

    const givenPassword = req.headers["x-manual-retrigger-password"];

    if (typeof givenPassword !== "string") {
        return res.status(401).json({ error: "Unauthorized" });
    }

    const givenBuffer = Buffer.from(givenPassword);
    const correctBuffer = Buffer.from(password);

    if ((givenBuffer.length !== correctBuffer.length) || !timingSafeEqual(givenBuffer, correctBuffer)) {
        return res.status(401).json({ error: "Unauthorized" });
    }

    try {        
        await updateMainPush({
            message: "Manual retrigger",
            hash: "manual-retrigger",
            automatedMessage: "chore: [AUTOMATED] manually retrigger javadoc generation"
        });
    } catch (error: any) {
        return res.status(500).json({ error: error.message });
    }

    res.status(200).json({ message: "Manual retrigger completed" });
}

export default handler;