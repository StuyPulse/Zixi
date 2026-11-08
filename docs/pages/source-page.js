import { highlightCode } from "../shared/syntax-highlighting.js";
import { scrollBehavior, getSafeHashSelector } from "../shared/utils.js";
import { onHashNavigate } from "../shared/hash-navigation.js";
const goToLineNumberByHash = (targetElement) => {
    const lineHashRegex = /^#line-\d+$/;
    const hash = window.location.hash;
    if (!lineHashRegex.test(hash))
        return false;
    if (!targetElement)
        return false;
    if (targetElement) {
        const lineInViewObserver = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!(entry.target === targetElement))
                    continue;
                if (entry.isIntersecting && entry.intersectionRatio >= 1.0) {
                    const lineHeight = (() => {
                        const computed = window.getComputedStyle(highlightDiv);
                        const lineHeight = computed.getPropertyValue("--line-height").trim();
                        return parseFloat(lineHeight);
                    })();
                    const lineNumber = parseInt(entry.target.id.replace("line-", ""));
                    const translateY = (lineNumber - 1) * lineHeight;
                    highlightDiv.style.setProperty("transform", `translateY(${translateY}em)`);
                    highlightDiv.classList.add("flash");
                    const handleAnimationEnd = (event) => {
                        const animationEvent = event;
                        if (animationEvent.animationName !== "flashLine")
                            return;
                        highlightDiv.classList.remove("flash");
                        highlightDiv.removeEventListener("animationend", handleAnimationEnd);
                    };
                    highlightDiv.addEventListener("animationend", handleAnimationEnd);
                    lineInViewObserver.disconnect();
                }
            }
        }, { threshold: 1.0 });
        lineInViewObserver.observe(targetElement);
        targetElement.scrollIntoView({ behavior: scrollBehavior(), block: "center" });
    }
    return true;
};
const pre = document.querySelector(".source-container > pre");
if (!pre) {
    throw new Error("Source code element was not found.");
}
const lines = pre.querySelectorAll(":scope > [id*=\"line-\"]");
const source = Array.from(lines).map(line => line.textContent).join("\n");
const lineNumbers = Array.from(lines).map((_, i) => `<span id="line-${i + 1}">${i + 1}</span>`).join("\n");
pre.outerHTML =
    `
<button class="back-button">BACK</button>
<div class="code-wrapper">
    <div class="line-numbers">
        ${lineNumbers}
    </div>

    <pre><code class="language-java" hlfs></code></pre>
</div>
`;
const codeBlock = document.querySelector(".code-wrapper > pre > code");
if (!codeBlock) {
    throw new Error("Code block element was not found.");
}
codeBlock.textContent = source;
highlightCode(codeBlock);
const highlightDiv = document.createElement("div");
highlightDiv.setAttribute("class", "line-highlight");
onHashNavigate(goToLineNumberByHash);
const applySyntaxHighlighting = async () => {
    const codeBlock = document.querySelector(".code-wrapper > pre > code");
    if (!codeBlock) {
        throw new Error("Code block element was not found.");
    }
    codeBlock.textContent = source;
    await highlightCode(codeBlock);
    codeBlock.appendChild(highlightDiv);
    const selector = getSafeHashSelector();
    const initialTarget = selector ? document.querySelector(selector) : null;
    if (initialTarget) {
        goToLineNumberByHash(initialTarget);
    }
};
applySyntaxHighlighting();
const backButton = document.querySelector(".back-button");
if (!backButton) {
    throw new Error("Back button element was not found.");
}
backButton.addEventListener("click", () => {
    const currentURL = new URL(window.location.href);
    currentURL.pathname = currentURL.pathname.replace("/src-html", "");
    currentURL.hash = "";
    window.location.href = currentURL.toString();
});
