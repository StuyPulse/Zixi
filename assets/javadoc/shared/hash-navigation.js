
import { scrollBehavior, getElementFromHash } from "./utils.js";

const hashNavigationCallbacks = [];

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
                history.pushState(null, null, href);
            }

            handleHashNavigation();
        });
    });

    window.addEventListener("hashchange", handleHashNavigation);
};

const onHashNavigate = (callback) => {
    hashNavigationCallbacks.push(callback);
};

initializeHashNavigation();

export { onHashNavigate };
