cmake_minimum_required(VERSION 3.22.1)
project(bbkeyboard)

add_library(bbkeyboard SHARED
    jni_bridge.cpp
    keyboard_engine.cpp
    predictor.cpp
    builtin_words.cpp
)

target_compile_features(bbkeyboard PRIVATE cxx_std_17)

target_include_directories(bbkeyboard PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(log-lib log)

target_link_libraries(bbkeyboard ${log-lib})
