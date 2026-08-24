# SPDX-License-Identifier: GPL-3.0-or-later
cmake_minimum_required(VERSION 3.22)
project(kokoly)

# espeak-ng, gepinnt auf den Referenz-Tag (siehe native/espeak-ng/PIN.md).
# FetchContent statt vendorn: reproduzierbar, kein GPL-Quelltext-Spiegel im Repo
# noetig (das Quellcodeangebot der GPL erfuellt der Pin + dieses Buildskript).
include(FetchContent)
FetchContent_Declare(
    espeak_ng
    GIT_REPOSITORY https://github.com/espeak-ng/espeak-ng.git
    GIT_TAG        1.52.0
    GIT_SHALLOW    TRUE
)

# Nur die Bibliothek, keine Audio-Ausgabe, keine Datenkompilierung.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(USE_ASYNC OFF CACHE BOOL "" FORCE)
set(USE_MBROLA OFF CACHE BOOL "" FORCE)
set(USE_LIBSONIC OFF CACHE BOOL "" FORCE)
set(USE_LIBPCAUDIO OFF CACHE BOOL "" FORCE)
set(USE_KLATT OFF CACHE BOOL "" FORCE)
set(USE_SPEECHPLAYER OFF CACHE BOOL "" FORCE)
set(EXTRA_cmn OFF CACHE BOOL "" FORCE)
set(EXTRA_ru OFF CACHE BOOL "" FORCE)
set(ESPEAK_BUILD_DATA OFF CACHE BOOL "" FORCE)
set(ESPEAK_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(ESPEAK_BUILD_EXE OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(espeak_ng)

add_library(kokoly_jni SHARED kokoly_jni.c)
target_link_libraries(kokoly_jni espeak-ng log)
