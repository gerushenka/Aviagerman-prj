cmake_minimum_required(VERSION 3.10)

project("aviagerman")

add_library(aviagerman-lib SHARED src/main/cpp/native-lib.cpp)

find_library(log-lib log)

target_link_libraries(aviagerman-lib ${log-lib})
