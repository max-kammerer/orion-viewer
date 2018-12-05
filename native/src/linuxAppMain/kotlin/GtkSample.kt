//import kotlinx.cinterop.*
//import gtk3.*
//
//fun <F : CFunction<*>> g_signal_connect(obj: CPointer<*>, actionName: String,
//                                        action: CPointer<F>, data: gpointer? = null, connect_flags: Int = 0) {
//    g_signal_connect_data(obj.reinterpret(), actionName, action.reinterpret(),
//            data = data, destroy_data = null, connect_flags = connect_flags)
//
//}
//
//
//fun activate(app: CPointer<GtkApplication>?, user_data: gpointer?) {
//    val windowWidget = gtk_application_window_new(app)!!
//    val window = windowWidget.reinterpret<GtkWindow>()
//    gtk_window_set_title(window, "Orion Viewer")
//    gtk_window_set_default_size(window, 600, 800)
//    val canvas = gtk_drawing_area_new()!!
//    gtk_container_set_border_width(window.reinterpret(), 11)
//    gtk_container_add (window.reinterpret(), canvas)
//
//        g_signal_connect(canvas, "draw",
//            staticCFunction { da: CPointer<GtkWidget>?, cairo: CPointer<cairo_t>? , _: gpointer? ->
//                memScoped {
//                    val gdkRgba = alloc<GdkRGBA>()
//
//
//                    val context = gtk_widget_get_style_context (da)
//
//                    val width = gtk_widget_get_allocated_width (da)
//                    val height = gtk_widget_get_allocated_height (da)
//
//                    gtk_render_background (context, cairo, 0.0, 0.0, width.toDouble(), height.toDouble())
//
//
////                    cairo_arc (cairo,
////                            width / 2.0, height / 2.0,
////                            width / 2.0,
////                            0.0, 2 * 3.14)
//
//
//                    val rect = alloc<GdkRectangle>()
//                    rect.x = 0
//                    rect.y = 0
//                    rect.width = 100
//                    rect.height = 100
//
//
//                    gdk_cairo_rectangle(cairo, rect.ptr )
//
//                    gtk_style_context_get_color (context,
//                            gtk_style_context_get_state (context),
//                            gdkRgba.ptr)
//
//                    gdkRgba.blue = .5
//                    gdkRgba.red = .5
//                    gdkRgba.green = 00.1
//
//
//                    gdk_cairo_set_source_rgba(cairo, gdkRgba.ptr )
//
//
//                    cairo_fill(cairo)
//                    false
//
//                }
//
//            })
//
//
//    //createHeaderBar(window)
//
//    gtk_widget_show_all(windowWidget)
//}
//
////private fun createHeaderBar(window: CPointer<GtkWindow>) {
////    val header = gtk_header_bar_new()!!
////    gtk_window_set_titlebar(window, header)
////    val buttonBox = gtk_button_box_new(
////            GtkOrientation.GTK_ORIENTATION_HORIZONTAL)!!
////    gtk_container_add(header.reinterpret(), buttonBox)
////
////    val button = gtk_button_new_with_label("Click me!")!!
////    g_signal_connect(button, "clicked",
////            staticCFunction { _: CPointer<GtkWidget>?, _: gpointer? ->
////                println("Clicked")
////            })
////    g_signal_connect(button, "clicked",
////            staticCFunction { widget: CPointer<GtkWidget>? ->
////                gtk_widget_destroy(widget)
////            },
////            window, G_CONNECT_SWAPPED)
////    gtk_container_add(buttonBox.reinterpret(), button)
////}
//
//fun gtkMain(args: Array<String>): Int {
//    val app = gtk_application_new("org.gtk.example", G_APPLICATION_FLAGS_NONE)!!
//    g_signal_connect(app, "activate", staticCFunction(::activate))
//    val status = memScoped {
//        g_application_run(app.reinterpret(),
//                args.size, args.map { it.cstr.getPointer(memScope) }.toCValues())
//    }
//    g_object_unref(app)
//    return status
//}
//
//fun main(args: Array<String>) {
//    gtkMain(args)
//}
