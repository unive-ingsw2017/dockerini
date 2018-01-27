Datadroid v1.2

Written by Alvise Spanò (C) 2017-18 Università Ca' Foscari, Venezia, Italy

Datadroid is a tiny Java library for Android featuring tools for quickly writing apps based on Open Data, including Google Maps support and other component wrappers. It is currently under development and its features may change with time.

The library offers a level of abstraction over the typical Android application architecture, transparently performing a number of tasks for dealing with open data downloading, parsing and scraping from several supported sources, allowing the programmer to write code faster and deliver quality apps easily and quickly.

It consists of two distinct modules: a library that can be imported and referenced from a fresh project, paired with a template app Android Studio project that can be cloned and reused.

Additionally, it includes a prototype sub-package that wraps the core Android API in a strong-typed fashion, using generics and type constraints over type parameters to perform static checks over inter-component communication: code written using such typed wrappers tends to be less bugged, lowering testing time and development time in general.


INSTALLATION
============

- module 'lib' can be reused like a library.
- module 'template' can be used as a starting point for developing a new app; a dependency to the library module is needed. Just open the 'template' app within Android Studio and dependencies with relative paths should apply.
- IMPORTANT: you need a Google API key in order to view the map activity. Open the resource file "values/google_maps_api.xml" from the template app and follow the instructions within the comments. Once you get your own key, paste it into that XML file and perform a full rebuild of the project (menu Build -> Rebuild Project).
