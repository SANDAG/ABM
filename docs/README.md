# Editing and building the documentation

The ABM documentation site, https://sandag.github.io/ABM/, is built from the Markdown files in this folder by [Zensical](https://zensical.org/). Zensical reads its settings from `mkdocs.yml` in the repository root.

| Path | What it holds |
|---|---|
| `docs/` | The pages (Markdown), images, and `stylesheets/extra.css` |
| `mkdocs.yml` | Site settings and the navigation menu (`nav:`) |
| `docs_requirements.txt` | The build tools, with pinned versions |
| `.github/workflows/docs.yml` | The GitHub Actions workflow that builds and publishes the site |

## One-time setup

Install [uv](https://docs.astral.sh/uv/getting-started/installation/). Then, from the repository root, create a docs environment in `.venv` and install the build tools into it:

```
uv venv --python 3.12 .venv
uv pip install --python .venv -r docs_requirements.txt
```

uv downloads Python 3.12 if needed. You don't need to activate the environment: `--python .venv`, here and below, points uv at it, even when another environment (a conda env or a model venv) is active in your terminal. Without that flag, uv can install into the active environment instead.

If `docs_requirements.txt` changes, run both commands again, adding `--clear` to the first to replace the old environment.

## Preview while you edit

```
uv run --python .venv zensical serve
```

Open http://localhost:8000. The preview rebuilds each time you save a file. Press Ctrl+C to stop it.

## Check before opening a pull request

```
uv run --python .venv zensical build --strict
```

A clean build ends with "No issues found". Otherwise, each problem (a broken link, for example) is listed with its file and line, and the build stops with "Aborted because --strict flag is set". The built site goes into `site/`, which git ignores.

## How the site is published

The Docs workflow (`.github/workflows/docs.yml`) publishes the site with [mike](https://github.com/squidfunk/mike), which keeps a separate copy of the docs for each version on the `gh-pages` branch:

- Every push to `main` updates **dev**, the docs for unreleased work.
- Every release tag `vX.Y.Z` adds version **X.Y.Z**. If it is the highest release so far, **latest** moves to it. Pre-release tags such as `v16.0.0-rc1` are published, but never become latest.
- The site's front page redirects to **latest**. Readers can switch versions from the menu next to the site title, or go straight to one, for example https://sandag.github.io/ABM/dev/.

Pull requests that change the docs get the strict build check automatically. Nothing is published until the change reaches `main`.

## Tips

- A new page appears in the menu only after you add it under `nav:` in `mkdocs.yml`.
- Text in angle brackets, like `<time period>`, is read as an HTML tag and disappears from the page. Put it in backticks, or write it as `&lt;time period&gt;`. Inside HTML tables, only the `&lt;` `&gt;` form works.