
package Layout.Widgets;

import GUI.ContextManager;
import Layout.LayoutManager;
import Layout.WidgetImpl.Configurator;
import Layout.WidgetImpl.HtmlEditor;
import Layout.WidgetImpl.Spectrumator;
import Layout.WidgetImpl.Visualisation;
import Layout.Widgets.Features.Feature;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 * Handles operations with Widgets.
 * <p>
 */
public final class WidgetManager {
    /** Collection of registered Widget Factories. Non null, unique.*/
    static final Map<String,WidgetFactory> factories = new HashMap<>();
    
    public static void initialize() {
        registerInternalWidgetFactories();
        registerExternalWidgetFactories();
    }
    
    /**
     * @return read only list of registered widget factories
     */
    public static Stream<WidgetFactory> getFactories() {
        return factories.values().stream();
    }
    
    /**
     * Looks for widget factory in the list of registered factories. Searches
     * by name.
     * @param name
     * @return widget factory, null if not found
     */
    public static WidgetFactory getFactory(String name) {
        // get factory
        WidgetFactory wf = factories.get(name);
        
        // attempt to register new factory for the file
        if(wf==null) {
            try {
                File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
                URL source = f.toURI().toURL();
                new FXMLWidgetFactory(name, source).register();
                Log.deb("registering " + name);
            } catch(MalformedURLException e) {
                Log.err("Error registering wirget: " + name);
            }
        }
        
        // return factory or null
        return factories.get(name);
    }

    
    /** Registers internal widget factories. */
    private static void registerInternalWidgetFactories() {
        new ClassWidgetFactory("Settings", Configurator.class).register();
        //new ClassWidgetFactory("Playlist Manager", PlaylistManagerComponent.class).register();
        new ClassWidgetFactory("Circles", Visualisation.class).register();
        //new ClassWidgetFactory("Graphs", Graphs.class).register();
        new ClassWidgetFactory("HTMLEditor", HtmlEditor.class).register();
        new ClassWidgetFactory("Spectrumator", Spectrumator.class).register();
        new EmptyWidgetFactory().register();
    }
    /**
     * Registers external widget factories.
     * Searches for .fxml files in widget folder and registers them as widget
     * factories.
     */
    private static void registerExternalWidgetFactories() {
        Log.deb("Searching for external widgets.");
        // get folder
        File dir = App.WIDGET_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("External widgets registration failed.");
            return;
        }
        // get .fxml files
        try {
            Files.find(dir.toPath(), 2, (path,u) -> path.toString().endsWith(".fxml"))
                    .forEach(p-> registerFactory(FileUtil.getName(p.toUri())));
        } catch(IOException e) {
            Log.err("Error during looking for widgets. Some widgets might not be available.");
        }
    }
    
    private static void registerFactory(String name) {
        // avoid registering twice
        if(factories.get(name) != null) return;
        
        try {
            File f = new File(App.WIDGET_FOLDER(), name + File.separator + name + ".fxml");
            URL source = f.toURI().toURL();
            new FXMLWidgetFactory(name, source).register();
            Log.deb("registering " + name);
        } catch(MalformedURLException e) {
            Log.err("Error registering wirget: " + name);
        }
    }
    
/******************************************************************************/
    
    /**
     * remembers standalone widgets not part of any layout, mostly in popups
     * as for normal widgets - they can be obtained from layouts
     * Do not use.
     */
    public static final List<Widget> standaloneWidgets = new ArrayList();

    public static Stream<Widget> getWidgets(Widget_Source source) {
        switch(source) {
            case LAYOUT:
                return LayoutManager.getLayouts()
                        .flatMap(l->l.getAllWidgets());
            case ACTIVE:
            case FACTORY:
                return Stream.concat(standaloneWidgets.stream(), 
                        LayoutManager.getLayouts()
                            .flatMap(l->l.getAllWidgets()));
            default: throw new AssertionError(source + " in default switch value.");
        }
    }
    
    /**
     * Returns widged based on condition. If there is preferred widget it will 
     * be returned first. Otherwise any widget can be returned from those that
     * fulfill condition.
     * If the widget isnt found it will be attempted to create it and display it
     * in a new window.
     * @param cond
     * @return widget fulfilling condition. Null if application has no access to
     * any widget fulfilling the condition.
     */
    public static Widget getWidget(Predicate<WidgetInfo> cond, Widget_Source source) {
        Widget out = null;
        // attempt to get preferred widget from loaded widgets
        if (out == null)
            out = getWidgets(source)
                    .filter(cond::test)
                    .filter(w-> w.isPreffered())
                    .findFirst().orElse(null);
        // attempt to get any widget from loaded widgets
        if (out == null)
            out = getWidgets(source)
                    .filter(cond::test)
                    .findFirst().orElse(null);
        
        // attempt to create new if no result yet
        if (out == null && source==Widget_Source.FACTORY) {
            WidgetFactory f;
            // attempt to get preferred factory
                f = getFactories()
                    .filter(cond::test)
                    .filter(w->w.preferred)
                    .findFirst().orElse(null);
            if (f==null)
            // attempt to get any factory
                f = getFactories()
                    .filter(cond::test)
                    .findFirst().orElse(null);
            // open widget if found
            out = f==null ? null : f.create();
            if(out!=null) {
                standaloneWidgets.add(out);
                ContextManager.showFloating(out);
            }
        }
        
        return out;
    }
    
    /**
     * Returns widget with specific behavior denoted by a {@link Feature}
     * interface. This interface is implemented by the widget's controller.
     * <p>
     * It is expected that the application contains many widgets and widget
     * types, some of which share some behavior. This can be discovered and
     * exploited by using common behavior interface - Feature.
     * <p>
     * This method looks up the available widgets and attempts to return best fit
     * for the specified Feature.
     * <p>
     * If the interface defines methods (it could be a simple marker
     * interface) and developer wishes to use them, a casting to respective
     * Feature is necessary. Cast only to the same interface as the one provided
     * as a parameter - in such case the casting will always succeed.
     * <p>
     * Example:   (TaggingFeature) widget.getController()
     * <p>
     * It does not make sense to try to obtain the exact class type of the
     * widget's controller as obtaining the widget or at most the Feature's type
     * and behavior is the point of this method.
     * In fact it is impossible to do this safely as there is no guarantee as to
     * what the Controller's type will be. Using the reflection could get over
     * this obstacle, but as noted, it should never be done.
     * @return any open widget supporting tagging or loaded new if none is loaded.
     * Null if no tagging widget available in application.
     */
    public static<F extends Feature> F getWidget(Class<F> feature, Widget_Source source) {
        Widget out = null;
        // attempt to get preferred widget from active widgets
        if (out == null)
            out = getWidgets(source)
                    .filter(w-> feature.isAssignableFrom(w.getController().getClass()))
                    .filter(w-> w.isPreffered())
                    .findFirst().orElse(null);
        // attempt to get any widget from active widgets
        if (out == null)
            out = getWidgets(source)
                    .filter(w-> feature.isAssignableFrom(w.getController().getClass()))
                    .findFirst().orElse(null);
        
        if (out == null && source==Widget_Source.FACTORY) {
           WidgetFactory f;
            // attempt to get preferred factory
                f = getFactories()
                    .filter(w->feature.isAssignableFrom(w.getControllerClass()))
                    .filter(w->w.preferred)
                    .findFirst().orElse(null);
            if (f==null)
            // attempt to get any factory
                f = getFactories()
                    .filter(w->feature.isAssignableFrom(w.getControllerClass()))
                    .findFirst().orElse(null);
            // open widget if found
            out = f==null ? null : f.create();
            if(out!=null) {
                standaloneWidgets.add(out);
                ContextManager.showFloating(out);
            }
        }
        return out==null ? null : (F) out.getController();
    }
    
    /**
     * Denotes source for widgets. Used when looking up a widget. Sometimes it
     * is desirable to limit the source.
     */
    public static enum Widget_Source {
        /**
         * The source will be all currently active widgets contained within all 
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         */
        LAYOUT,
        /**
         * The source will contain all currently active widgets in the app.
         * Contains LAYOUT source and in addition all currently active 
         * standalone widgets.
         */
        ACTIVE,
        /**
         * The source will contain all available widgets to the application.
         * Contains ACTIVE source, but in addition will search all
         * registered widget factories and create new widget if necessary.
         * Most complete source.
         */
        FACTORY;
    }
    public static enum Widget_Target {
        LAYOUT,
        TAB,
        POPUP,
        WINDOW;
    }
    
    // if space add new widget to layout, if not make new tab, switch tabs
}