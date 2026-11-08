import { scrollBehavior, getElementFromHash } from "./utils.js";

const hashNavigationCallbacks: HashNavigationCallback[] = [];

const handleHashNavigation = () => {
    const hash = window.location.hash;

    if (hash === "#" || hash === "") {
        window.scrollTo({ top: 0, behavior: scrollBehavior() });
        return;
    }

    const element = getElementFromHash();
    if (!element) return;

    const handled = hashNavigationCallbacks.some(
        callback => callback(element)
    );

    if (!handled) {
        element.scrollIntoView({ behavior: scrollBehavior(), block: "start" });
    }
}

const initializeHashNavigation = () => {
    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
        anchor.addEventListener("click", (event) => {
            const href = anchor.getAttribute("href");

            event.preventDefault();

            if (window.location.hash !== href) {
                history.pushState(null, "", href);
            }

            handleHashNavigation();
        });
    });

    window.addEventListener("hashchange", handleHashNavigation);
};

const onHashNavigate = (callback: HashNavigationCallback) => {
    hashNavigationCallbacks.push(callback);
};

initializeHashNavigation();

type HashNavigationCallback = (element: HTMLElement) => boolean;

export { onHashNavigate };
export type { HashNavigationCallback };
