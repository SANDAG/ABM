import pandas as pd
from ruamel.yaml import YAML
from collections import OrderedDict

yamlru = YAML(typ = "rt")

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
    with open(yaml_file, 'r') as stream:
        contents = stream.read()
        print(f"Contents of {yaml_file}: {contents}")
        stream.seek(0)  # Reset the stream position to the start of the file
        try:
            return yamlru.load(stream)
        except yamlru.YAMLError as exc:
            print(exc)

def write_yaml(yaml_file, yaml_dict):
    with open(yaml_file, 'w') as outfile:
        yamlru.dump(yaml_dict, outfile)