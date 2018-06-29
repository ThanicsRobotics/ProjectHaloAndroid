//
// Created by andrew on 6/28/18.
//

#include <string>
#include <stdint.h>
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <gst/gst.h>
#include <gst/video/video.h>
#include <pthread.h>

#ifndef PROJECTHALOANDROID_GSTREAMERPIPELINE_H
#define PROJECTHALOANDROID_GSTREAMERPIPELINE_H


class GstreamerPipeline {
public:
    GstreamerPipeline(std::string pipelineString);
    void gst_native_init();
private:
    std::string pipeline;
};


#endif //PROJECTHALOANDROID_GSTREAMERPIPELINE_H
