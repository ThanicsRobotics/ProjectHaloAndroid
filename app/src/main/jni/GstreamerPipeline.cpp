//
// Created by andrew on 6/28/18.
//

#include "GstreamerPipeline.h"

GstreamerPipeline::GstreamerPipeline(std::string pipelineString)
    : pipeline(pipelineString)
{

}

void GstreamerPipeline::gst_native_init() {
    GST_DEBUG("Pipeline: ", pipeline);
//    CustomData *data = g_new0 (CustomData, 1);
//    SET_CUSTOM_DATA (env, thiz, custom_data_field_id, data);
//    GST_DEBUG_CATEGORY_INIT (debug_category, "tutorial-3", 0, "Android tutorial 3");
//    gst_debug_set_threshold_for_name("tutorial-3", GST_LEVEL_DEBUG);
//    GST_DEBUG ("Created CustomData at %p", data);
//    data->app = (*env)->NewGlobalRef (env, thiz);
//    GST_DEBUG ("Created GlobalRef for app object at %p", data->app);
    //pthread_create (&gst_app_thread, NULL, &app_function, data);
}