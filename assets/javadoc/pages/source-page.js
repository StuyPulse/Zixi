import { highlightCode } from "../shared/syntax-highlighting.js";
import { scrollBehavior, getSafeHashSelector } from "../shared/utils.js";
import { onHashNavigate } from "../shared/hash-navigation.js";

let animationEndEventListener = null;

const goToLineNumberByHash = (targetElement) => {
    const lineHashRegex = /^#line-\d+$/;

    const hash = window.location.hash;
    if (!lineHashRegex.test(hash)) return false;
    if (!targetElement) return false;

    if (targetElement) {
        const lineInViewObserver = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!(entry.target === targetElement)) continue;

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

                    const handleAnimationEnd = (animationEvent) => {
                        if (animationEvent.animationName !== "flashLine") return;

                        highlightDiv.classList.remove("flash");
                    }
                    highlightDiv.addEventListener("animationend", handleAnimationEnd, { once: true });
                    animationEndEventListener = handleAnimationEnd;

                    lineInViewObserver.disconnect();
                }
            }
        }, { threshold: 1.0 })

        lineInViewObserver.observe(targetElement);
        targetElement.scrollIntoView({ behavior: scrollBehavior(), block: "center" });
    }

    return true;
}

const pre = document.querySelector(".source-container > pre");
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
`

const codeBlock = document.querySelector(".code-wrapper > pre > code");
codeBlock.textContent = source;
highlightCode(codeBlock);

const highlightDiv = document.createElement("div");
highlightDiv.setAttribute("class", "line-highlight");
onHashNavigate(goToLineNumberByHash);

const applySyntaxHighlighting = async () => {
    const codeBlock = document.querySelector(".code-wrapper > pre > code");
    codeBlock.textContent = source;

    await highlightCode(codeBlock);
    codeBlock.appendChild(highlightDiv);

    const initialTarget = document.querySelector(getSafeHashSelector());

    if (initialTarget) {
        goToLineNumberByHash(initialTarget);
    }
};

applySyntaxHighlighting();

const backButton = document.querySelector(".back-button");
backButton.addEventListener("click", () => {
    const currentURL = new URL(window.location.href);
    currentURL.pathname = currentURL.pathname.replace("/src-html", "");
    currentURL.hash = "";
    window.location.href = currentURL.toString();
});
