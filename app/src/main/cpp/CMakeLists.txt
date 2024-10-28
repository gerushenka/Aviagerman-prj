cmake_minimum_required(VERSION 3.10)

project("aviagerman")

add_library(aviagerman-lib SHARED native.cpp)

find_library(log-lib log)

target_link_libraries(aviagerman-lib ${log-lib})
