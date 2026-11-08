import process from "node:process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));

const projectDirectory = path.resolve(
    scriptDirectory,
    process.argv[2] ?? ".."
);

const javadocDirectory = path.join(
    projectDirectory,
    "build/docs/javadoc"
);

const assetsDirectory = path.join(
    path.dirname(fileURLToPath(import.meta.url)),
    "../assets"
);

const javadocAssetsDirectory = path.join(assetsDirectory, "javadoc");
const sharedScriptsDirectory = path.join(javadocAssetsDirectory, "shared");

const sharedScriptFiles = fs.readdirSync(sharedScriptsDirectory);

const sourcePageScript = "source-page.js";
const classDeclarationScript = "class-declaration.js";

const getRelativePath = (filePath: string) => {
    return path.relative(projectDirectory, filePath);
}

const getRelativePathBetweenFiles = (fromFile: string, toFile: string) => {
    const relativePath = path.relative(
        path.dirname(fromFile),
        toFile
    );

    return relativePath.replaceAll("\\", "/");
}

type TextColor = "green" | "yellow" | "red";
const log = (message: string, color: TextColor = "yellow") => {
    const RESET_ANSI = "\x1b[0m";

    const colorCode = (() => {
        if (color === "green") {
            return "\x1b[32m";
        }
        if (color === "yellow") {
            return "\x1b[33m";
        }
        if (color === "red") {
            return "\x1b[31m";
        }
    })();

    console.log(`${colorCode}${message}${RESET_ANSI}`);
}

const addModule = (filePath: string, relativeScriptPath: string) => {
    const scriptPath = path.join(javadocDirectory, relativeScriptPath);
    return `<script defer type="module" src="${getRelativePathBetweenFiles(filePath, scriptPath)}"></script>`;
};

const copyJavadocAssets = (relativePath: string) => {
    const sourcePath = path.join(javadocAssetsDirectory, relativePath);
    const destinationPath = path.join(javadocDirectory, relativePath);

    fs.mkdirSync(path.dirname(destinationPath), { recursive: true });
    fs.cpSync(sourcePath, destinationPath, { recursive: true });

    log(`Copied javadoc/${relativePath}`);
};

const fileWalker = (directory: string) => {
    const files = fs.readdirSync(directory, { withFileTypes: true });

    for (const file of files) {
        const filePath = path.join(directory, file.name);

        if (file.isDirectory()) {
            fileWalker(filePath);
        } else if (file.isFile()) {
            if (filePath.endsWith(".html")) {
                htmlPatcher(filePath);
            }
        }
    }
}

const htmlPatcher = (filePath: string) => {
    const text = fs.readFileSync(filePath, "utf-8");

    const faviconFile = path.join(javadocDirectory, "favicon.svg");

    const inlineThemeScript = `<script>!function(){let s=localStorage.getItem("theme")||"system",e=s;"system"===s&&(e=window.matchMedia("(prefers-color-scheme:light)").matches?"light":"dark"),document.documentElement.setAttribute("data-theme",e)}();</script>`
    const stylesheetFile = path.join(javadocDirectory, "betterjdocs.css");

    let replacement =
        inlineThemeScript +
        `<link rel="stylesheet" href="${getRelativePathBetweenFiles(filePath, stylesheetFile)}">` +
        `<link rel="icon" href="${getRelativePathBetweenFiles(filePath, faviconFile)}?t=${Date.now()}" type="image/x-icon">`;

    for (const scriptFile of sharedScriptFiles) {
        replacement += addModule(filePath, path.join("shared", scriptFile));
    }

    const relativePath = path.relative(javadocDirectory, filePath);
    const isSourcePage = relativePath.split(path.sep).includes("src-html");
    const isClassDeclarationPage = /<body\b[^>]*\bclass=["'][^"']*\bclass-declaration-page\b[^"']*["']/i.test(text);
    
    if (isSourcePage) {
        replacement += addModule(filePath, path.join("pages", sourcePageScript));
    }

    if (isClassDeclarationPage) {
        replacement += addModule(filePath, path.join("pages", classDeclarationScript));
    }

    replacement += "</head>";

    const newText = text.replace(
        "</head>",
        replacement
    );

    if (newText === text) {
        log(`Failed to patch ${getRelativePath(filePath)}`, "red");
        return;
    }

    fs.writeFileSync(filePath, newText, "utf-8");
    log(`Patched ${getRelativePath(filePath)}`);
}

const javadocAssets = [
    "javadoc/betterjdocs.css",
    "javadoc/favicon.svg",
    "logos/StuyPulseLogo.png"
]
const addJavadocAssets = () => {
    for (const asset of javadocAssets) {
        const sourcePath = path.join(assetsDirectory, asset);
        const assetName = path.basename(asset);
        const destPath = path.join(javadocDirectory, assetName);
        fs.copyFileSync(sourcePath, destPath);
        log(`Copied ${getRelativePath(sourcePath)} to javadoc directory`);
    }
}

const checkpointFile = path.join(javadocDirectory, ".patched");

if (fs.existsSync(checkpointFile)) {
    log("Javadoc already patched, skipping patcher", "green"); 
} else {
    copyJavadocAssets("shared");
    copyJavadocAssets("pages");
    addJavadocAssets();
    fileWalker(javadocDirectory);

    log("Patcher complete", "green");
    fs.writeFileSync(checkpointFile, "done", "utf-8");
}
