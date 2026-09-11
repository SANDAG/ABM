import pandas as pd
from ruamel.yaml import YAML
from collections import OrderedDict

def load_properties(file_dir):
    prop = OrderedDict()
    comments = {}
    with open(file_dir, 'r') as properties:
        comment = []
        for line in properties:
            line = line.strip()
            if not line or line.startswith('#'):
                comment.append(line)
                continue
            key, value = line.split('=')
            key = key.strip()
            tokens = value.split(',')
            if len(tokens) > 1:
                value = _parse_list(tokens)
            else:
                value = _parse(value)
            prop[key] = value
            comments[key], comment = comment, []
    return prop

def _parse_list(values):
    converted_values = []
    for v in values:
        converted_values.append(_parse(v))
    return converted_values

def _parse(value):
    value = str(value).strip()
    if value == 'true':
        return True
    elif value == 'false':
        return False
    for caster in int, float:
        try:
            return caster(value)
        except ValueError:
            pass
    return value

def open_yaml(yaml_file):
    yaml = YAML()
    yaml.preserve_quotes = True
    with open(yaml_file, 'r') as stream:
        try:
            return yaml.load(stream)
        except Exception as exc:
            print(exc)
            raise

def write_yaml(yaml_file, yaml_dict):
    yaml = YAML()
    yaml.preserve_quotes = True
    with open(yaml_file, 'w') as outfile:
        yaml.dump(yaml_dict, outfile)