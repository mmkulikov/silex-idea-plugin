IntelliJ IDEA / PhpStorm plugin for Silex
===================

Provides missing [Pimple Dependency Injection Container](http://pimple.sensiolabs.org/) autocomplete for [Silex](http://silex.sensiolabs.org/).
Plugin url: https://plugins.jetbrains.com/plugin/7809?pr=idea

Installation and Usage
------------
Install the plugin from JetBrains repositories (Settings → Plugins → Browse repositories and search for Silex).  
Download and register [Silex Pimple Dumper](https://github.com/Sorien/silex-pimple-dumper), it will create **pimple.json** file to your project root directory, plugin will use this file for service/parameter type resolution.

JSON Example
-------

```json
[
    {
        "name": "serviceValue",
        "type": "class",
        "value": "Service\\Namespace\\Location"
    },
    {
        "name": "arrayValue",
        "type": "array",
        "value": ""
    },
    {
        "name": "booleanValue",
        "type": "bool",
        "value": false
    }
]
```