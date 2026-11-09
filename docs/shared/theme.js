import { getSvgFromName } from "./svgs.js";
import { isSourcePage } from "./utils.js";
let themeState = "system";
const themeChangeCallbacks = [];
const themes = ["system", "dark", "light"];
const systemThemeQuery = window.matchMedia("(prefers-color-scheme: light)");
const interactionQuery = window.matchMedia("(prefers-reduced-motion: reduce), (hover: none), (max-width: 800px)");
const reducedMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
const narrowViewportQuery = window.matchMedia("(max-width: 800px)");
let interactionMode = interactionQuery.matches ? "click" : "hover";
let isOpen = false;
const themeContainer = (() => {
    if (isSourcePage())
        return null;
    const topNavbar = document.querySelector("#navbar-top");
    if (!topNavbar)
        return null;
    const wrapper = document.createElement("div");
    wrapper.id = "theme-toggle-menu";
    wrapper.setAttribute("class", "theme-toggle-wrapper");
    wrapper.setAttribute("role", "group");
    wrapper.setAttribute("aria-label", "Choose color theme");
    wrapper.classList.toggle("interaction-click", interactionMode === "click");
    wrapper.classList.toggle("no-motion", reducedMotionQuery.matches);
    wrapper.classList.toggle("is-vertical", narrowViewportQuery.matches);
    topNavbar.appendChild(wrapper);
    themes.forEach((theme) => {
        const button = document.createElement("button");
        button.type = "button";
        button.setAttribute("class", `theme-button theme-button-${theme}`);
        button.setAttribute("aria-label", `${theme[0].toUpperCase()}${theme.slice(1)} theme`);
        button.setAttribute("title", `Use ${theme} theme`);
        button.setAttribute("data-theme-target", theme);
        button.appendChild(getSvgFromName(theme));
        button.addEventListener("click", (event) => {
            if (button.classList.contains("is-active") && !isOpen) {
                event.preventDefault();
                if (interactionMode === "click")
                    toggleMenu();
                else
                    openMenu();
                return;
            }
            setTheme(theme);
            closeMenu();
            button.focus();
        });
        wrapper.appendChild(button);
    });
    wrapper.addEventListener("keydown", (event) => {
        const buttons = Array.from(wrapper.querySelectorAll(".theme-button"));
        const currentIndex = buttons.indexOf(document.activeElement);
        if (currentIndex === -1)
            return;
        if (event.key === "Escape") {
            event.preventDefault();
            closeMenu();
            wrapper.querySelector(".theme-button.is-active")?.focus();
            return;
        }
        let nextIndex;
        if (event.key === "ArrowRight" || event.key === "ArrowDown") {
            nextIndex = (currentIndex + 1) % buttons.length;
        }
        else if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
            nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
        }
        if (nextIndex !== undefined) {
            event.preventDefault();
            if (!isOpen)
                openMenu();
            buttons[nextIndex].focus();
        }
    });
    wrapper.addEventListener("mouseenter", () => {
        if (interactionMode === "hover")
            openMenu();
    });
    wrapper.addEventListener("mouseleave", () => {
        if (interactionMode === "hover")
            closeMenu();
    });
    wrapper.addEventListener("focusin", () => {
        if (interactionMode === "hover" && document.activeElement.matches(":focus-visible")) {
            openMenu();
        }
    });
    wrapper.addEventListener("focusout", (event) => {
        if (interactionMode === "hover" && !wrapper.contains(event.relatedTarget))
            closeMenu();
    });
    document.addEventListener("pointerdown", (event) => {
        if (isOpen && !wrapper.contains(event.target))
            closeMenu();
    });
    return wrapper;
})();
positionVerticalMenu();
const updateMenuAccessibility = () => {
    if (!themeContainer)
        return;
    themeContainer.querySelectorAll(".theme-button").forEach((button) => {
        const active = button.classList.contains("is-active");
        button.setAttribute("aria-pressed", String(active));
        button.setAttribute("aria-hidden", String(!isOpen && !active));
        button.setAttribute("tabindex", isOpen || active ? "0" : "-1");
        if (active) {
            button.setAttribute("aria-expanded", String(isOpen));
            button.setAttribute("aria-controls", "theme-toggle-menu");
        }
        else {
            button.removeAttribute("aria-expanded");
            button.removeAttribute("aria-controls");
        }
    });
};
function openMenu() {
    if (!themeContainer || isOpen)
        return;
    isOpen = true;
    themeContainer.classList.add("is-open");
    positionVerticalMenu();
    updateMenuAccessibility();
}
function closeMenu() {
    if (!themeContainer || !isOpen)
        return;
    isOpen = false;
    themeContainer.classList.remove("is-open");
    updateMenuAccessibility();
}
function toggleMenu() {
    if (isOpen)
        closeMenu();
    else
        openMenu();
}
function positionVerticalMenu() {
    if (!themeContainer || !narrowViewportQuery.matches)
        return;
    const bounds = themeContainer.getBoundingClientRect();
    themeContainer.style.setProperty("--theme-menu-top", `${bounds.top + bounds.height / 2}px`);
    themeContainer.style.setProperty("--theme-menu-left", `${bounds.left}px`);
}
window.addEventListener("scroll", positionVerticalMenu, true);
window.addEventListener("resize", positionVerticalMenu);
interactionQuery.addEventListener("change", (event) => {
    const nextMode = event.matches ? "click" : "hover";
    if (nextMode === interactionMode)
        return;
    interactionMode = nextMode;
    themeContainer?.classList.toggle("interaction-click", interactionMode === "click");
    const focusedButton = themeContainer?.contains(document.activeElement) ? document.activeElement : null;
    closeMenu();
    if (focusedButton && themeContainer?.contains(focusedButton)) {
        themeContainer.querySelector(".theme-button.is-active")?.focus();
    }
});
reducedMotionQuery.addEventListener("change", (event) => {
    themeContainer?.classList.toggle("no-motion", event.matches);
});
narrowViewportQuery.addEventListener("change", (event) => {
    themeContainer?.classList.toggle("is-vertical", event.matches);
    positionVerticalMenu();
});
const handleThemeChangeEvent = () => {
    if (themeState === "system")
        setTheme("system");
};
const setTheme = (theme) => {
    if (!themes.includes(theme)) {
        console.warn(`Invalid theme: ${theme}. Valid themes are: ${themes.join(", ")}`);
        return;
    }
    themeState = theme;
    let effectiveTheme = theme === "system"
        ? (systemThemeQuery.matches ? "light" : "dark")
        : theme;
    if (theme === "system") {
        effectiveTheme = systemThemeQuery.matches ? "light" : "dark";
        systemThemeQuery.addEventListener("change", handleThemeChangeEvent);
    }
    else {
        systemThemeQuery.removeEventListener("change", handleThemeChangeEvent);
    }
    if (themeContainer) {
        themeContainer.dataset.activeTheme = themeState;
        themeContainer.querySelectorAll(".theme-button").forEach((button) => {
            button.classList.toggle("is-active", button.getAttribute("data-theme-target") === themeState);
        });
        updateMenuAccessibility();
    }
    themeChangeCallbacks.forEach((callback) => callback(themeState, effectiveTheme));
    localStorage.setItem("theme", themeState);
    document.documentElement.setAttribute("data-theme", effectiveTheme);
};
(() => {
    let storedTheme = localStorage.getItem("theme");
    if (!storedTheme || !themes.includes(storedTheme)) {
        storedTheme = "system";
        localStorage.setItem("theme", storedTheme);
    }
    setTheme(storedTheme);
    updateMenuAccessibility();
})();
const onThemeChange = (callback) => {
    themeChangeCallbacks.push(callback);
};
const getTheme = () => themeState;
const getEffectiveTheme = () => {
    if (themeState === "system") {
        return systemThemeQuery.matches ? "light" : "dark";
    }
    return themeState;
};
export { getTheme, getEffectiveTheme, onThemeChange };
