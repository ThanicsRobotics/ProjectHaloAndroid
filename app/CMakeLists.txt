# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

set(SWIG_I_FILE "src/main/jni/gstreamer-pipeline.i")
set(JAVA_GEN_PACKAGE "com.sureshjoshi.core")
string(REPLACE "." "/" JAVA_GEN_SUBDIR ${JAVA_GEN_PACKAGE})
set(JAVA_GEN_DIR ${Project_SOURCE_DIR}/src/main/java/${JAVA_GEN_SUBDIR})

# Sets the minimum version of CMake required to build the native library.
set(CMAKE_VERBOSE_MAKEFILE 1) # Use 1 for debugging, 0 for release
cmake_minimum_required(VERSION 3.4.1)

# Sets some warning/error conditions and which version of C/C++ to use
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -std=c++14")

# Note: One could use a 'GLOB' here, but newly added source files won't auto-regen build files
# After adding files, you'd need to 'touch' the CMakeLists.txt to re-gen
set(SOURCE_FILES
    src/main/jni/GstreamerPipeline.cpp
    )

set(GSTREAMER_ROOT_ANDROID
    /media/hdd/gstreamer-android)

if (${ANDROID_ABI} EQUAL armeabi)
    set(GSTREAMER_ROOT ${GSTREAMER_ROOT_ANDROID}/arm)
elseif (${ANDROID_ABI} EQUAL armeabi-v7a)
    set(GSTREAMER_ROOT ${GSTREAMER_ROOT_ANDROID}/armv7)
elseif (${ANDROID_ABI} EQUAL arm64-v8a)
    set(GSTREAMER_ROOT ${GSTREAMER_ROOT_ANDROID}/arm64)
elseif (${ANDROID_ABI} EQUAL x86)
    set(GSTREAMER_ROOT ${GSTREAMER_ROOT_ANDROID}/x86)
elseif (${ANDROID_ABI} EQUAL x86_64)
    set(GSTREAMER_ROOT ${GSTREAMER_ROOT_ANDROID}/x86_64)
endif()

# Include a location to the header files
include_directories(src/main/jni
                    ${GSTREAMER_ROOT}/include)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library( gstreamer-pipeline
             SHARED
             ${SOURCE_FILES} )

add_library( gstreamer-android
             SHARED
             ${Project_SOURCE_DIR}/gst-build-${ANDROID_ABI} )
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that you want CMake to locate.
              log )
find_library( gstreamer_android-lib
              gstreamer_android )

# Fail the build if SWIG is not found - Ensure SWIG is installed and on the system path
# TODO: Does this run for EACH architecture?
find_package(SWIG REQUIRED)
include(${SWIG_USE_FILE})

# Remove old java files, in case we don't need to generate some of them anymore
file(REMOVE_RECURSE ${JAVA_GEN_DIR})

# Ensure file recognized as C++ (otherwise, exported as C file)
set_property(SOURCE src/main/jni/gstreamer-pipeline.i PROPERTY CPLUSPLUS ON)

# Setup SWIG flags and locations
set(CMAKE_SWIG_FLAGS -c++ -package ${JAVA_GEN_PACKAGE})
set(CMAKE_SWIG_OUTDIR ${JAVA_GEN_DIR})

# Export a wrapper file to Java, and link with the created C++ library
swig_add_module(gstreamer-pipeline_wrap java ${SWIG_I_FILE})
swig_link_libraries(gstreamer-pipeline_wrap gstreamer-pipeline)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries( # Specifies the target library.
                       gstreamer-pipeline

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )