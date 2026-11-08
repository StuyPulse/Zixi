import { getEffectiveTheme, onThemeChange, type EffectiveTheme } from "./theme.js";
import type { HLJSApi } from "highlight.js";

declare global {
    interface Window {
        hljs: HLJSApi;
    }
}

let hljsPromise: Promise<HLJSApi> | null = null;
let hljsCSS: HTMLLinkElement | null = null;

const setCSS = (effectiveTheme: EffectiveTheme) => {
    const theme = effectiveTheme === "light" ? "github" : "github-dark";
    if (!hljsCSS) {
        const link = document.createElement("link");
        link.rel = "stylesheet";
        link.setAttribute("href", `https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/${theme}.min.css`)
        
        document.head.appendChild(link);
        hljsCSS = link;
    } else {
        hljsCSS.setAttribute("href", `https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/${theme}.min.css`);
    }
}

const loadHighlightJS = () => {
    if (hljsPromise) return hljsPromise;

    hljsPromise = new Promise((resolve, reject) => {
        const script = document.createElement("script");

        script.src = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js";

        script.onload = () => resolve(window.hljs);
        script.onerror = reject;

        setCSS(getEffectiveTheme());

        document.head.appendChild(script);
    });

    return hljsPromise;
};

const highlightCode = async (element: HTMLElement) => {
    const hljs = await loadHighlightJS();

    hljs.highlightElement(element);
};

const highlightAll = async (selector: string = "pre code") => {
    const hljs = await loadHighlightJS();
    
    document.querySelectorAll<HTMLElement>(selector).forEach(element => {
        hljs.highlightElement(element);
    });
};

onThemeChange((_, effectiveTheme) => setCSS(effectiveTheme));

export { highlightCode, highlightAll };
