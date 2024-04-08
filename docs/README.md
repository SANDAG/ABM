# Instructions for Editing/Managing Documentation

TODO: update/refine as needed

## Getting Started

To get started using Anaconda, do the following:

```
conda create -n rsm_docs python=3.10
conda activate rsm_docs
pip install -r docs_requirements.txt
```

To create the website locally:

```
mkdocs serve
```

See the `mkdocs.yml` file for the configuration.

To create the website on GitHub pages, see the .github/workflows/docs.yml
