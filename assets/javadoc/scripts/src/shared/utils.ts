const scrollBehavior = () => {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth";
}

const isSourcePage = () => {
    return document.querySelector("body.source-page") !== null;
}

const isClassDeclarationPage = () => {
    return document.querySelector("body.class-declaration-page") !== null;
}

const getElementFromHash = (hash?: string | null) => {
    try {
        if (!hash) {
            hash = window.location.hash.slice(1);
        }
        if (!hash) return null;

        const selector = getSafeHashSelector(`#${hash}`);
        return selector ? document.querySelector(selector) as HTMLElement : null;
    } catch {
        return null;
    }
}

const getSafeHashSelector = (hash: string = window.location.hash) => {
    if (!hash) return null;

    let id = hash.slice(1);

    try {
        id = decodeURIComponent(id);
    } catch {
        return null;
    }

    id = `${CSS.escape(id)}`;
    if (!id.startsWith("#")) {
        id = `#${id}`;
    }
    return id;
};

export { scrollBehavior, isSourcePage, isClassDeclarationPage, getElementFromHash, getSafeHashSelector };
