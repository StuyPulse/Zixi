import { getSvgFromName } from "./svgs.js";
import { isSourcePage } from "./utils.js";

let themeState: Theme = "system";

const themeChangeCallbacks: Array<ThemeChangeCallback> = [];

const themes: Theme[] = ["system", "dark", "light"];
const themeContainer = (() => {
    if (isSourcePage()) return null;

    const topNavbar = document.querySelector<HTMLElement>("#navbar-top");
    if (!topNavbar) return null;
    
    const themeToggleWrapper = document.createElement("div");
    themeToggleWrapper.setAttribute("role", "menu");
    themeToggleWrapper.setAttribute("class", "theme-toggle-wrapper");
    topNavbar.appendChild(themeToggleWrapper);
    
    themes.forEach(theme => {
        const button = document.createElement("button");
        button.setAttribute("class", `theme-button theme-button-${theme}`);
        button.setAttribute("aria-label", `Switch to ${theme} theme`);
        button.setAttribute("title", `Switch to ${theme} theme`);
        button.setAttribute("data-theme-target", theme);
        button.appendChild(getSvgFromName(theme));
        
        button.addEventListener("click", (e) => {
            if (!themeToggleWrapper.classList.contains("is-hover") && button.classList.contains("is-active")) {
                e.preventDefault();
                openMenu();
                return;
            }
            
            setTheme(theme);
            closeMenu();
            button.focus();
        });
        themeToggleWrapper.appendChild(button);
    });

    themeToggleWrapper.addEventListener("keydown", (event: KeyboardEvent) => {
        const buttons = Array.from(themeToggleWrapper.querySelectorAll<HTMLButtonElement>(".theme-button"));
        const currentIndex = buttons.indexOf(document.activeElement as HTMLButtonElement);

        if (currentIndex === -1) return;

        let nextIndex;
        if (event.key === "ArrowRight" || event.key === "ArrowDown") {
            event.preventDefault();
            nextIndex = (currentIndex + 1) % buttons.length;
        } else if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
            event.preventDefault();
            nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
        } else if (event.key === "Escape") {
            event.preventDefault();
            closeMenu();
            const activeButton = themeToggleWrapper.querySelector<HTMLButtonElement>(".theme-button.is-active");
            if (activeButton) {
                activeButton.focus();
            }
        }

        if (nextIndex !== undefined) {
            buttons[currentIndex].setAttribute("tabindex", "-1");
            buttons[nextIndex].setAttribute("tabindex", "0");
            buttons[nextIndex].focus();
        }
    });

    const openMenu = () => {
        themeToggleWrapper.classList.add("is-hover");
        toggleMenuAccessibility(true);
    };

    const closeMenu = () => {
        themeToggleWrapper.classList.remove("is-hover");
        toggleMenuAccessibility(false);
    };

    themeToggleWrapper.addEventListener("mouseenter", openMenu);

    themeToggleWrapper.addEventListener("mouseleave", closeMenu);

    themeToggleWrapper.addEventListener("focusin", () => {
        if (!(document.activeElement as HTMLElement).matches(":focus-visible")) return;

        openMenu();
    });

    themeToggleWrapper.addEventListener("focusout", (event: FocusEvent) => {
        if (!themeToggleWrapper.contains(event.relatedTarget as Node)) {
            closeMenu();
        }
    });

    return themeToggleWrapper;
})();

const toggleMenuAccessibility = (isOpen: boolean) => {
    if (!themeContainer) return;

    const buttons = themeContainer.querySelectorAll<HTMLButtonElement>(".theme-button");
    buttons.forEach((button) => {
        const isActive = button.classList.contains("is-active");

        if (isOpen) {
            button.setAttribute("role", "menuitemradio");
            button.removeAttribute("aria-haspopup");
            button.removeAttribute("aria-expanded");
            button.removeAttribute("aria-hidden");
            button.setAttribute("aria-checked", isActive ? "true" : "false");

            if (isActive) {
                button.setAttribute("tabindex", "0");
            } else {
                if (document.activeElement !== button) {
                    button.setAttribute("tabindex", "-1");
                }
            }
        } else {
           if (isActive) {
                button.setAttribute("role", "button");
                button.setAttribute("tabindex", "0");
                button.setAttribute("aria-haspopup", "true");
                button.setAttribute("aria-expanded", "false");
                button.setAttribute("aria-hidden", "true");
           } else {
                button.removeAttribute("role");
                button.setAttribute("tabindex", "-1");
                button.removeAttribute("aria-haspopup");
                button.removeAttribute("aria-expanded");
                button.setAttribute("aria-hidden", "true");
           }
        }
    });
};

const handleThemeChangeEvent = (event: MediaQueryListEvent) => {
    if (themeState === "system") {
        setTheme("system");
    }
}

const setTheme = (theme: Theme) => {
    if (!themes.includes(theme)) {
        console.warn(`Invalid theme: ${theme}. Valid themes are: ${themes.join(", ")}`);
        return;
    }

    themeState = theme;
    if (theme === "system") {
        const isLightMode = window.matchMedia("(prefers-color-scheme: light)").matches;
        theme = isLightMode ? "light" : "dark";
        window.matchMedia("(prefers-color-scheme: light)").addEventListener("change", handleThemeChangeEvent);
    } else {
        window.matchMedia("(prefers-color-scheme: light)").removeEventListener("change", handleThemeChangeEvent);
    }

    if (themeContainer) {
        const buttons = themeContainer.querySelectorAll(".theme-button");
        buttons.forEach((button) => {
            if (button.getAttribute("data-theme-target") === themeState) {
                button.classList.add("is-active");
            } else {
                button.classList.remove("is-active");
            }
        });
    
        const isExpanded = themeContainer.classList.contains("is-hover") || themeContainer.contains(document.activeElement);
        toggleMenuAccessibility(isExpanded);
    }

    themeChangeCallbacks.forEach((callback) => callback(themeState, getEffectiveTheme()));
    localStorage.setItem("theme", themeState);
    document.documentElement.setAttribute("data-theme", theme);
}

(() => {
    let storedTheme = localStorage.getItem("theme");
    if (!storedTheme || !themes.includes(storedTheme as Theme)) {
        storedTheme = "system";
        localStorage.setItem("theme", storedTheme);
    }

    setTheme(storedTheme as Theme);
    toggleMenuAccessibility(false);
})();

const onThemeChange = (callback: ThemeChangeCallback) => {
    themeChangeCallbacks.push(callback);
}

const getTheme = () => {
    return themeState;
}

const getEffectiveTheme = () => {
    if (themeState === "system") {
        const isLightMode = window.matchMedia("(prefers-color-scheme: light)").matches;
        return isLightMode ? "light" : "dark";
    }
    return themeState;
}

type EffectiveTheme = "dark" | "light";
type Theme = "system" | EffectiveTheme;
type ThemeChangeCallback = (theme: Theme, effectiveTheme: EffectiveTheme) => void

export { getTheme, getEffectiveTheme, onThemeChange };
export type { Theme, EffectiveTheme };
