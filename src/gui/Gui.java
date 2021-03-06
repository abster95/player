
package gui;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.WatchService;
import java.util.*;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.css.StyleManager;

import gui.objects.window.stage.Window;
import layout.container.layout.Layout;
import util.access.V;
import util.access.VarEnum;
import util.action.IsAction;
import util.action.IsActionable;
import util.animation.interpolator.CircularInterpolator;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.file.FileMonitor;
import util.file.Util;

import static gui.Gui.OpenStrategy.INSIDE;
import static java.io.File.separator;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.stream.Collectors.toList;
import static javafx.animation.Interpolator.LINEAR;
import static javafx.application.Application.STYLESHEET_CASPIAN;
import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.scene.text.FontPosture.ITALIC;
import static javafx.scene.text.FontPosture.REGULAR;
import static javafx.scene.text.FontWeight.BOLD;
import static javafx.scene.text.FontWeight.NORMAL;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.Util.capitalizeStrong;
import static util.animation.interpolator.EasingMode.EASE_OUT;
import static util.file.FileMonitor.monitorDirectory;
import static util.file.FileMonitor.monitorFile;
import static util.functional.Util.listRO;
import static util.functional.Util.set;

/**
 *
 * @author Martin Polakovic
 */
@IsActionable
@IsConfigurable
public class Gui {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gui.class);
    private static final String DEF_SKIN = Gui.class.getResource("skin/Skin.css").toExternalForm();
    private static String skinOldUrl = ""; // set to not sensible non null value
    public static final BooleanProperty layout_mode = new SimpleBooleanProperty(false);

    private static final Map<File,WatchService> fileMonitors = new HashMap<>();
    private static final Set<String> skins = new HashSet<>();

    // applied configs
    @IsConfig(name = "Skin", info = "Application skin.")
    public static final VarEnum<String> skin = new VarEnum<String>("Default", () -> skins, Gui::setSkin);
    /**
     * Font of the application. Overrides font defined by skin. The font can be
     * overridden programmatically or stylesheet.
     *
     * Note: font is applied only if the GUI is fully initialized, otherwise does
     * nothing.
     */
    @IsConfig(name = "Font", info = "Application font.")
    public static final V<Font> font = new V<>(Font.getDefault(), Gui::applyFont);

    // non applied configs
    @IsConfig(name = "Layout mode blur bgr", info = "Layout mode use blur effect.")
    public static boolean blur_layoutMode = false;
    @IsConfig(name = "Layout mode fade bgr", info = "Layout mode use fade effect.")
    public static boolean opacity_layoutMode = true;
    @IsConfig(name = "Layout mode fade intensity", info = "Layout mode fade effect intensity.", min=0.0, max=1.0)
    public static double opacity_LM = 1;
    @IsConfig(name = "Layout mode blur intensity", info = "Layout mode blur efect intensity.", min=0.0, max=20.0)
    public static double blur_LM = 4;
    @IsConfig(name = "Layout mode anim length", info = "Duration of layout mode transition effects.")
    public static Duration duration_LM = millis(250);
    @IsConfig(name = "Snap", info = "Allows snapping feature for windows and controls.")
    public static final V<Boolean> snapping = new V<>(true);
    @IsConfig(name = "Snap activation distance", info = "Distance at which snap feature gets activated")
    public static final V<Double> snapDistance = new V<>(6d);
    @IsConfig(name = "Lock layout", info = "Locked layout will not enter layout mode.")
    public final static BooleanProperty locked_layout = new SimpleBooleanProperty(false){
        @Override public void set(boolean v) {
            super.set(v);
            APP.actionStream.push("Layout lock");
        }
    };
    @IsConfig(name = "Layout open strategy", info = "How will certain layout element open and close.")
    public static OpenStrategy open_strategy = INSIDE;

    @IsConfig(name = "Table orientation", group = "Table",
            info = "Orientation of the table.")
    public static final ObjectProperty<NodeOrientation> table_orient = new SimpleObjectProperty<>(NodeOrientation.INHERIT);
    @IsConfig(name = "Zeropad numbers", group = "Table",
            info = "Adds 0s for number length consistency.")
    public static final V<Boolean> table_zeropad = new V<>(false);
    @IsConfig(name = "Search show original index", group = "Table",
            info = "Show unfiltered table item index when filter applied.")
    public static final  V<Boolean> table_orig_index = new V<>(false);
    @IsConfig(name = "Show table header", group = "Table",
            info = "Show table header with columns.")
    public static final  V<Boolean> table_show_header = new V<>(true);
    @IsConfig(name = "Show table controls", group = "Table",
            info = "Show table controls at the bottom of the table. Displays menubar and table items information")
    public static final  V<Boolean> table_show_footer = new V<>(true);


    static {
        skins.addAll(getSkins());
        monitorDirectory(APP.DIR_SKINS, (type,file) -> {
            LOGGER.info("Change {} detected in skin directory for {}", type,file);
            skins.clear();
            skins.addAll(getSkins());
        });
    }
    private static File monitoredSkin = null;
    private static FileMonitor monitorSkin = null;
    private static void monitorSkinStart(File cssFile) {
        if(cssFile.equals(monitoredSkin)) return;
        monitorSkinStop();
        monitorSkin = monitorFile(cssFile, type -> {
            if(type==ENTRY_MODIFY)
                loadSkin();
        });
    }
    private static void monitorSkinStop() {
        if(monitorSkin!=null) monitorSkin.stop();
        monitorSkin = null;
        monitoredSkin = null;
    }


    /**
     * Component might rely on this method to alter its behavior. For example
     * it can leave layout mode on its own independently but forbid this
     * behavior if this method returns TRUE.
     * <p/>
     * Note that this method consistently returns FALSE at the time
     * of entering and leaving the layout mode, thus allowing to safely query
     * layout mode state just before the state change. In other words the state
     * change itself will not influence the result, which changes only after the
     * change occurred. not after it was invoked.
     * <p/>
     * Basically, at the time of invoking show() and hide() methods the flag
     * is (and must be) FALSE and never TRUE
     *
     * @return whether layout mode was on or not.
     */
    public static boolean isLayoutMode() {
        return layout_mode.get();
    }

    public static BooleanProperty layoutLockedProperty() {
        return locked_layout;
    }

    /** Set lock to prevent layouting. */
    public static void setLayoutlocked(boolean val) {
        locked_layout.set(val);
    }

    /** Return lock to prevent layouting. */
    public static boolean isLayoutLocked() {
        return locked_layout.get();
    }

    @IsAction(name = "Toggle layout lock.", desc = "Lock/unlock layout.", keys = "F4")
    /** Toggles lock to prevent layouting. */
    public static void toggleLayoutLocked() {
        locked_layout.set(!locked_layout.get());
    }

    /** Loads/refreshes whole gui. */
    @IsAction(name = "Reload GUI.", desc = "Reload application GUI. Includes skin, font, layout.", keys = "F5")
    public static void refresh() {
        if (APP.initialized) {
            skin.applyValue();
            font.applyValue();
            loadLayout();
        }
    }

    /** Loads/refreshes active layout. */
    @IsAction(name = "Reload layout", desc = "Reload layout.", keys = "F6")
    public static void loadLayout() {
        APP.widgetManager.getLayouts().forEach(Layout::load);
    }

    /** Toggles layout controlling mode. */
    @IsAction(name = "Reload skin", desc = "Reloads skin.", keys = "F7")
    public static void loadSkin() {
        skin.applyValue();
    }

    /**
     * Sets layout mode on/off.
     * <p/>
     * Note that {@link #isLayoutMode()} consistently returns FALSE at the time
     * of entering and leaving the layout mode.
     * @see #isLayoutMode()
     */
    public static void setLayoutMode(boolean val) {
        // avoid pointless operation
        if(layout_mode.get()==val) return;
        // Note that we set the layout mode flag after invoking show() but
        // before invoking hide().
        // This is important to maintain consistency. See documentation.
        if (val) {
            APP.widgetManager.getLayouts().forEach(Layout::show);
            layout_mode.set(val);
        } else {
            layout_mode.set(val);
            APP.widgetManager.getLayouts().forEach(Layout::hide);
            setZoomMode(false);
        }
        if(val) APP.actionStream.push("Layout mode");
    }

    public static void setZoomMode(boolean val) {
        Window w = APP.windowManager.getFocused();
        if(w!=null && w.getSwitchPane()!=null) w.getSwitchPane().zoom(val);
    }

    /** Toggles layout mode. */
    @IsAction(name = "Manage Layout", desc = "Toggles layout mode on/off.")
    public static void toggleLayoutMode() {
        setLayoutMode(!layout_mode.get());
    }

    /** Toggles zoom mode. */
    @IsAction(name = "Zoom Layout", desc = "Toggles layout zoom in/out.")
    public static void toggleZoomMode() {
        Window w = APP.windowManager.getFocused();
        if(w!=null && w.getSwitchPane()!=null) w.getSwitchPane().toggleZoom();
    }

    public static void setLayoutNzoom(boolean v) {
        setLayoutMode(v);
        setZoomMode(v);
    }

    @IsAction(name = "Manage Layout & Zoom", desc = "Enables layout managment mode and zooms.", keys = "F8")
    public static void toggleLayoutNzoom() {
        toggleLayoutMode();
        toggleZoomMode();
    }

    @IsAction(name = "Show/Hide application", desc = "Equal to switching minimized mode.", keys = "CTRL+ALT+W", global = true)
    @IsAction(name = "Minimize", desc = "Switch minimized mode.", keys = "F10")
    public static void toggleMinimizeFocus() {
        // After this operation, all windows are either minimized or not, but vefore it, the state
        // may vary. Thus we need to decide whether we minimize all windows or the opposite.
        // if any window is not minimized, app
        boolean m = APP.windowManager.windows.stream().map(w -> w.isMinimized()).reduce(false, Boolean::logicalOr);
        boolean f = APP.windowManager.windows.stream().map(w -> w.focused.get()).reduce(false, Boolean::logicalOr);
        APP.windowManager.windows.forEach(w -> {
            if(!m && !f)
                w.focus();
            else
                w.setMinimized(!m);
        });
    }

    @IsAction(name = "Show application", desc = "Shows application.", global = true)
    public static void showApp() {
        APP.windowManager.windows.forEach(w -> w.setMinimized(false));
    }

    @IsAction(name = "Hide application", desc = "Hides application.", global = true)
    public static void hideApp() {
        APP.windowManager.windows.forEach(w -> w.setMinimized(true));
    }

    public static void toggleMinimize() {
        boolean m = APP.windowManager.windows.stream().map(Window::isMinimized).reduce(false, Boolean::logicalOr);
        APP.windowManager.windows.forEach(w -> w.setMinimized(!m));
    }

    @IsAction(name = "Maximize", desc = "Switch maximized mode.", keys = "F11")
    public static void toggleMaximize() {
        APP.windowManager.getActive().toggleMaximize();
    }

    @IsAction(name = "Loop maximized state", desc = "Switch to different maximized window states.", keys = "F3")
    public static void toggleMaximizedState() {
        Window w = APP.windowManager.getActive();
        w.setMaximized(w.isMaximized().next());
    }

    @IsAction(name = "Fullscreen", desc = "Switch fullscreen mode.", keys = "F12")
    public static void toggleFullscreen() {
        APP.windowManager.getActive().toggleFullscreen();
    }

    @IsAction(name = "Align tabs", desc = "Aligns tabs to the window", keys = "SHIFT+UP")
    public static void tabAlign() {
        APP.windowManager.getActive().getSwitchPane().alignTabs();
    }

    @IsAction(name = "Align to next tab", desc = "Goes to next tab and aligns tabs to the window", keys = "SHIFT+RIGHT")
    public static void tabNext() {
        APP.windowManager.getActive().getSwitchPane().alignRightTab();
    }

    @IsAction(name = "Align to previous tab", desc = "Goes to previous tab and aligns tabs to the window", keys = "SHIFT+LEFT")
    public static void tabPrevious() {
        APP.windowManager.getActive().getSwitchPane().alignLeftTab();
    }


    /**
     * Searches for .css files in skin folder and registers them as available
     * skins. Use on app start or to discover newly added layouts dynamically.
     */
    public static Set<String> getSkins() {
        LOGGER.info("Looking for valid skins");

        // get + verify path
        File dir = APP.DIR_SKINS;
        if (!Util.isValidatedDirectory(dir)) {
            LOGGER.error("Search for skins failed." + dir.getPath() + " could not be accessed.");
            return set();
        }

        // find skins
        Set<String> skins = new HashSet<>();
        File[] dirs = dir.listFiles(File::isDirectory);
        skins.clear();
        for (File d: dirs) {
            String name = d.getName();
            File css = new File(d, name + ".css");
            if(Util.isValidFile(css)) {
                skins.add(name);
                LOGGER.info("\t" + name);
            }
        }

        LOGGER.info(skins.size() + " skins found.");
        LOGGER.info("Registering internal skins.");
        skins.add(capitalizeStrong(STYLESHEET_CASPIAN));
        skins.add(capitalizeStrong(STYLESHEET_MODENA));
        LOGGER.info("\tModena");
        LOGGER.info("\tCaspian");

        return skins;
    }

/*****************************  setter methods ********************************/

    /**
     * Changes application's skin and applies it.
     * This is a convenience method that constructs a file from the skinname
     * and calls the setSkin(File skin) method.
     * The skin file will be constructed like this:
     * application location .../Skins/skinname/skinname.css
     * For any details regarding the mechanics behind the method see documentation
     * of that method.
     * @param s name of the skin to apply.
     */
    public static void setSkin(String s) {
        if (s == null || s.isEmpty()) throw new IllegalArgumentException();
        LOGGER.info("Skin {} applied", s);

        if (s.equalsIgnoreCase(STYLESHEET_MODENA)) {
            setSkinModena();
        } else if (s.equalsIgnoreCase(STYLESHEET_CASPIAN)) {
            setSkinCaspian();
        } else {
            File skin_file = new File(APP.DIR_SKINS.getPath(), s + separator + s + ".css");
            setSkinExternal(skin_file);
        }
    }
    /**
     * Changes application's skin.
     * @param cssFile - css file of the skin to load. It is expected that the skin
     * file and resources are available.
     * @return true if the skin has been applied.
     * False return value signifies that gui has not been initialized or that
     * skin file could be accessed.
     * True return value does not imply successful skin loading, but guarantees
     * that the new skin has been applied regardless of the success. There can
     * still be parsing errors resulting in imperfect skin application.
     */
    public static boolean setSkinExternal(File cssFile) {
        if (APP.windowOwner.isInitialized() && Util.isValidSkinFile(cssFile)) {
            try {
                monitorSkinStart(cssFile);
                String url = cssFile.toURI().toURL().toExternalForm();
                // remove old skin
                StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
                StyleManager.getInstance().removeUserAgentStylesheet(DEF_SKIN);

                Application.setUserAgentStylesheet(STYLESHEET_MODENA);
                // set core skin
//                StyleManager.getInstance().setDefaultUserAgentStylesheet(DEF_SKIN);
                // add new skin
                StyleManager.getInstance().addUserAgentStylesheet(DEF_SKIN);
                StyleManager.getInstance().addUserAgentStylesheet(url);
                // set current skin
                skin.setValue(Util.getName(cssFile));
                // store its url so we can remove the skin later
                skinOldUrl = url;
                return true;
            } catch (MalformedURLException ex) {
                LOGGER.error(ex.getMessage());
                return false;
            }
        }
        return false;
    }

    private static boolean setSkinModena() {
        monitorSkinStop();
        if (APP.windowOwner.isInitialized()) {
            // remove old skin
            StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
            // set code skin
            Application.setUserAgentStylesheet(STYLESHEET_MODENA);
            skin.setValue("Modena");
            return true;
        }
        return false;
    }
    private static boolean setSkinCaspian() {
        monitorSkinStop();
        if (APP.windowOwner.isInitialized()) {
            // remove old skin
            StyleManager.getInstance().removeUserAgentStylesheet(skinOldUrl);
            // set code skin
            Application.setUserAgentStylesheet(STYLESHEET_CASPIAN);
            skin.setValue("Caspian");
            return true;
        }
        return false;
    }

    public static List<File> getGuiImages() {
        File location = new File(APP.DIR_SKINS, skin + separator + "Images");
        if(Util.isValidDirectory(location)) {
            return Util.getFilesImage(location, 1).collect(toList());
        } else {
            LOGGER.warn("Can not access skin directory: " + location.getPath());
            return listRO();
        }
    }

    /*****************************  helper methods ********************************/

    private static void applyFont(Font f) {
        // apply only if application initialized correctly
        if (APP.initialized) {
            // we need to apply to each window separately
            APP.windowManager.windows.forEach(w ->{
                String tmp = f.getStyle().toLowerCase();
                FontPosture style = tmp.contains("italic") ? ITALIC : REGULAR;
                FontWeight weight = tmp.contains("bold") ? BOLD : NORMAL;
                // for some reason javaFX and css values are quite different...
                String styleS = style==ITALIC ? "italic" : "normal";
                String weightS = weight==BOLD ? "bold" : "normal";
                w.getStage().getScene().getRoot().setStyle(
                        "-fx-font-family: \"" + f.getFamily() + "\";" +
                                "-fx-font-style: " + styleS + ";" +
                                "-fx-font-weight: " + weightS + ";" +
                                "-fx-font-size: " + f.getSize() + ";"
                );
            });
        }
    }








    public static final Duration ANIM_DUR = Duration.millis(300);

//    public static void closeAndDo(Node n, Runnable action) {
//        double pos = n.getOpacity()==1 ? 0 : n.getOpacity();
//               pos *= pos;
//        Animation a = buildAnimation(n, action);
//                  a.setRate(-1);
//                  a.playFrom(ANIM_DUR.subtract(ANIM_DUR.multiply(pos)));
//    }
//    public static void openAndDo(Node n, Runnable action) {
//        double pos = n.getOpacity()==1 ? 0 : n.getOpacity();
//               pos *= pos;
//        Animation a = buildAnimation(n, action);
//                  a.playFrom(ANIM_DUR.multiply(pos));
//    }
//
//    private static Animation buildAnimation(Node n, Runnable action) {
//        Anim a = new Anim(ANIM_DUR,x -> n.setOpacity(x*x));
//            a.then(action);
//        return a;
//    }

    public static void closeAndDo(Node n, Runnable action) {
        double pos = n.getScaleX()==1 ? 0 : n.getScaleX();
        Animation a = buildAnimation(n, action);
        a.setRate(-1);
        a.playFrom(ANIM_DUR.subtract(ANIM_DUR.multiply(pos)));
    }
    public static void openAndDo(Node n, Runnable action) {
        double pos = n.getScaleX()==1 ? 0 : n.getScaleX();
        Animation a = buildAnimation(n, action);
        a.playFrom(ANIM_DUR.multiply(pos));
    }

    private static Animation buildAnimation(Node n, Runnable action) {
        FadeTransition a1 = new FadeTransition(ANIM_DUR);
        a1.setFromValue(0);
        a1.setToValue(1);
        a1.setInterpolator(LINEAR);
        ScaleTransition a2 = new ScaleTransition(ANIM_DUR);
        a2.setInterpolator(new CircularInterpolator(EASE_OUT));
        a2.setFromX(0);
        a2.setFromY(0);
        a2.setToX(1);
        a2.setToY(1);
        Animation pt = new ParallelTransition(n, a1, a2);
        pt.setOnFinished(action==null ? null : e->action.run());
        return pt;
    }

    public enum OpenStrategy {
        POPUP, INSIDE
    }
}
