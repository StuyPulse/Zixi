const constructSvgRoot = (): SVGSVGElement => {
    const svgRoot = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svgRoot.setAttribute("viewBox", "0 0 24 24");
    svgRoot.setAttribute("width", "24");
    svgRoot.setAttribute("height", "24");
    svgRoot.setAttribute("fill", "none");
    svgRoot.setAttribute("stroke", "currentColor");
    return svgRoot;
}

const getSystemThemeIcon = (): SVGSVGElement => {
    const systemThemeIcon = constructSvgRoot();
    systemThemeIcon.innerHTML = `
        <rect width="20" height="14" x="2" y="3" rx="2"/>
        <line x1="8" x2="16" y1="21" y2="21"/>
        <line x1="12" x2="12" y1="17" y2="21"/>
    `

    return systemThemeIcon;
}

const getDarkThemeIcon = (): SVGSVGElement => {
    const darkThemeIcon = constructSvgRoot();
    darkThemeIcon.innerHTML = `
        <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
    `;

    return darkThemeIcon;
}

const getLightThemeIcon = (): SVGSVGElement => {
    const lightThemeIcon = constructSvgRoot();
    lightThemeIcon.innerHTML = `
        <circle cx="12" cy="12" r="4"/>
        <path d="M12 2v2"/>
        <path d="M12 20v2"/>
        <path d="m4.93 4.93 1.41 1.41"/>
        <path d="m17.66 17.66 1.41 1.41"/>
        <path d="M2 12h2"/>
        <path d="M20 12h2"/>
        <path d="m6.34 17.66-1.41 1.41"/>
        <path d="m19.07 4.93-1.41 1.41"/>
    `;

    return lightThemeIcon;
}

const getSvgFromName = (name: SVGName): SVGSVGElement => {
    const svg = themeIcons[name];
    if (!svg) {
        throw new Error(`Unknown theme name: ${name}`);
    }

    return svg();
}

const themeIcons = {
    system: getSystemThemeIcon,
    dark: getDarkThemeIcon,
    light: getLightThemeIcon,
}

type SVGName = keyof typeof themeIcons;

export { getSystemThemeIcon, getDarkThemeIcon, getLightThemeIcon, getSvgFromName };
export type { SVGName };
