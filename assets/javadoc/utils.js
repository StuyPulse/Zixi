const scrollBehavior = () => {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth";
}

const isSourcePage = () => {
    return document.querySelector("body.source-page") !== null;
}

const isClassDeclarationPage = () => {
    return document.querySelector("body.class-declaration-page") !== null;
}

const getElementFromHash = (hash = null) => {
    try {
        if (!hash) {
            hash = window.location.hash.slice(1);
        }
        if (!hash) return null;

        const decodedHash = decodeURIComponent(hash);
        return document.getElementById(decodedHash);
    } catch {
        return null;
    }
};

export { scrollBehavior, isSourcePage, isClassDeclarationPage, getElementFromHash };