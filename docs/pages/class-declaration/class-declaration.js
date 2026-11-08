import { scrollBehavior, getSafeHashSelector } from "../../shared/utils.js";
import { onHashNavigate } from "../../shared/hash-navigation.js";
let targetElementInView = null;
const sectionInViewObserver = new IntersectionObserver((entries) => {
    for (const entry of entries) {
        if (entry.target !== targetElementInView)
            continue;
        if (!entry.isIntersecting || entry.intersectionRatio < 0.99)
            continue;
        entry.target.classList.add("flash");
        sectionInViewObserver.unobserve(entry.target);
        const handleAnimationEnd = (event) => {
            const animationEvent = event;
            if (animationEvent.animationName !== "flashBorder")
                return;
            entry.target.classList.remove("flash");
            targetElementInView = null;
        };
        entry.target.addEventListener("animationend", handleAnimationEnd, {
            once: true
        });
    }
}, { threshold: 0.99 });
const handleSectionNavigation = (targetElement) => {
    if (!targetElement.matches("section.detail"))
        return false;
    if (targetElementInView) {
        targetElementInView.classList.remove("flash");
        sectionInViewObserver.unobserve(targetElementInView);
        targetElementInView = null;
    }
    targetElementInView = targetElement;
    sectionInViewObserver.observe(targetElement);
    targetElement.scrollIntoView({ behavior: scrollBehavior(), block: "center" });
    return true;
};
onHashNavigate(handleSectionNavigation);
const initialSelector = getSafeHashSelector();
const initialTarget = initialSelector
    ? document.querySelector(initialSelector)
    : null;
if (initialTarget) {
    handleSectionNavigation(initialTarget);
}
