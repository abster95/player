
package main;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Service;
import AudioPlayer.services.ServiceManager;
import AudioPlayer.services.notif.Notifier;
import AudioPlayer.services.playcountincr.PlaycountIncrementer;
import AudioPlayer.services.tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import AudioPlayer.tagging.MetadataReader;
import Configuration.*;
import Layout.Component;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import Layout.Widgets.feature.ConfiguringFeature;
import action.Action;
import action.IsAction;
import action.IsActionable;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.PopOver.PopOver;
import static gui.objects.PopOver.PopOver.ScreenCentricPos.App_Center;
import gui.objects.TableCell.RatingCellFactory;
import gui.objects.TableCell.TextStarRatingCellFactory;
import gui.objects.Window.stage.Window;
import gui.objects.Window.stage.WindowManager;
import gui.objects.icon.IconInfo;
import gui.pane.CellPane;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.ImageCursor;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ClassName;
import static util.File.Environment.browse;
import util.File.FileUtil;
import util.InstanceName;
import util.access.AccessorEnum;
import static util.async.Async.FX;
import static util.async.Async.run;
import static util.async.Async.runLater;
import util.async.future.Fut;
import static util.functional.Util.map;
import util.plugin.PluginMap;


/**
 * Application. Launches and terminates program.
 */
@IsActionable
@IsConfigurable("General")
public class App extends Application {


    /**
     * Starts program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    private final Logger logger = LoggerFactory.getLogger(App.class);
    
/******************************************************************************/
    
    /**
     * Event source and stream for executed actions, providing their name. Use
     * for notifications of running the action or executing additional behavior.
     * <p>
     * A use case could be an application wizard asking user to do something.
     * The code in question simply notifies this stream of the name of action
     * upon execution. The wizard would then monitor this stream
     * and get notified if the expected action was executed.
     * <p>
     * Running an {@link Action} always fires an event.
     * Supports custom actions. Simply push a String value into the stream.
     */
    public static final EventSource<String> actionStream = new EventSource();
    
/******************************************************************************/
    
    
    // NOTE: for some reason cant make fields final in this class +
    // initializing fields right up here (or constructor) will have no effect
    public static Window window;
    public static final ServiceManager services = new ServiceManager();
    public static PluginMap plugins = new PluginMap();
    private static App instance;
    public static Guide guide;
    private Window windowOwner;
    
    private boolean initialized = false;
    
    
    public App() {
        instance = this;
    }    
    
/*********************************** CONFIGS **********************************/
    
    @IsConfig(name = "Show guide on app start", info = "Show guide when application "
            + "starts. Default true, but when guide is shown, it is set to false "
            + "so the guide will never appear again on its own.")
    public static boolean showGuide = true;
    
    @IsConfig(name = "Rating control.", info = "The style of the graphics of the rating control.")
    public static final AccessorEnum<RatingCellFactory> ratingCell = new AccessorEnum<>(new TextStarRatingCellFactory(),() -> plugins.getPlugins(RatingCellFactory.class));
    @IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.", min = 1, max = 10)
    public static final IntegerProperty maxRating = new SimpleIntegerProperty(5);
    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
    public static final BooleanProperty partialRating = new SimpleBooleanProperty(true);
    @IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    public static final BooleanProperty allowRatingChange = new SimpleBooleanProperty(true);
    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
    public static final BooleanProperty hoverRating = new SimpleBooleanProperty(true);
    @IsConfig(name = "Debug value", info = "For application testing. Generic number value"
            + "to control some application value manually.")
    public static final DoubleProperty debug = new SimpleDoubleProperty(0);
    
    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public static String TAG_NO_VALUE = "-- no assigned value --";
    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public static String TAG_MULTIPLE_VALUE = "-- multiple values --";
    public static boolean ALBUM_ARTIST_WHEN_NO_ARTIST = true;
    
    
/******************************************************************************/
    
    public static InstanceName instanceName = new InstanceName();
    public static ClassName className = new ClassName();
    
    /**
     * The application initialization method. This method is called immediately 
     * after the Application class is loaded and constructed. An application may
     * override this method to perform initialization prior to the actual starting
     * of the application.
     * <p>
     * NOTE: This method is not called on the JavaFX Application Thread. An
     * application must not construct a Scene or a Stage in this method. An 
     * application may construct other JavaFX objects in this method.
     */
    @Override
    public void init() {
        // configure logging
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(lc);
            lc.reset();
            lc.putProperty("LOG_DIR", LOG_DIR.getPath());
            // override default configuration
            jc.doConfigure(LOG_CONFIG_FILE);
        } catch (JoranException ex) {
            logger.error(ex.getMessage());
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
        
        // log uncaught thread termination exceptions
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> 
            logger.error(t.getName(), e)
        );
        
        
        // add optional object instance -> string converters
        className.add(Item.class, "Song");
        className.add(PlaylistItem.class, "Playlist Song");
        className.add(Metadata.class, "Library Song");
        className.add(MetadataGroup.class, "Song Group");
        // add optional object class -> string converters
        instanceName.add(Item.class, Item::getPath);
        instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
        instanceName.add(Metadata.class,Metadata::getTitle);
        instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
        instanceName.add(Component.class, o -> o.getName());
        instanceName.add(List.class, o -> String.valueOf(o.size()));
        instanceName.add(File.class, File::getPath);
    }
    
    /**
     * The main entry point for applications. The start method is
     * called after the init method has returned, and after the system is ready
     * for the application to begin running.
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread. 
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet. Applications
     * may create other stages, if needed, but they will not be primary stages
     * and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            Action.startGlobalListening();
            
            // create hidden main window
            windowOwner = Window.createWindowOwner();
            windowOwner.show();
            
            // discover plugins
            ClassIndex.getAnnotated(IsPluginType.class).forEach(plugins::registerPluginType);
            ClassIndex.getAnnotated(IsPlugin.class).forEach(plugins::registerPlugin);
            WidgetManager.initialize();
            
            // services must be loaded before Configuration
            services.addService(new TrayService());
            services.addService(new Notifier());
            services.addService(new PlaycountIncrementer());
            
            // gather configs
            Configuration.collectAppConfigs();
            // deserialize values (some configs need to apply it, will do when ready)
            Configuration.load();
            
            // initializing, the order is important
            Player.initialize();
            
            // collectAppConfigs windows from previous session
            WindowManager.deserialize();
            
            DB.start();
            
//            GUI.setLayoutMode(true);
//            Transition t = par(
//                Window.windows.stream().map(w -> 
//                    seq(
//                        new Anim(at -> ((SwitchPane)w.getLayoutAggregator()).zoomProperty().set(1-0.6*at))
//                                .dur(500).intpl(new CircularInterpolator()),
//                        par(
//                            par(
//                                forEachIStream(w.left_icons.box.getChildren(),(i,icon)-> 
//                                    new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
//                            ),
//                            par(
//                                forEachIRStream(w.right_icons.box.getChildren(),(i,icon)-> 
//                                    new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
//                            ),
//                            par(
//                                w.getLayoutAggregator().getLayouts().values().stream()
//                                 .flatMap(l -> l.getAllWidgets())
//                                 .map(wi -> (Area)wi.load().getUserData())
//                                 .map(a -> 
//                                    seq(
//                                        new Anim(a.content_root::setOpacity).dur(2000+random()*1000).intpl(0),
//                                        new Anim(a.content_root::setOpacity).dur(700).intpl(isAroundMin1(0.04, 0.1,0.2,0.3))
//                                    )
//                                 )
//                            )
//                        ),
//                        par(
//                            new Anim(at -> ((SwitchPane)w.getLayoutAggregator()).zoomProperty().set(0.4+0.7*at))
//                                    .dur(500).intpl(new CircularInterpolator())
//                        )
//                    )
//                )
//            );
//            t.setOnFinished(e -> {
//                GUI.setLayoutMode(false);
//            });
//            t.play();
            
            initialized = true;
            
        } catch(Exception e) {
            String ex = Stream.of(e.getStackTrace()).map(s->s.toString()).collect(Collectors.joining("\n"));
            System.out.println(ex);
            // copy exception trace to clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(ex);
            clipboard.setContent(content);
            
            e.printStackTrace();
        }
         //initialize non critical parts
        Player.loadLast();                      // should load in the end
        
        // all ready -> apply all settings
        Configuration.getFields().forEach(Config::applyValue);
        
        // handle guide
        guide = new Guide();
        if(showGuide) {
            showGuide = false;
            run(2222, () -> guide.start());
        }
        
        System.out.println(new File("cursor.png").getAbsoluteFile().toURI().toString());
        Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());  //pass in the image path
        ImageCursor c = new ImageCursor(image,3,3);
        window.getStage().getScene().setCursor(c);
        
//        List<String> s = new ArrayList<>();
//        System.out.println("raw");
//        App.getInstance().getParameters().getRaw().forEach(s::add);
//        System.out.println("unnamed");
//        App.getInstance().getParameters().getUnnamed().forEach(s::add);
//        System.out.println("named");
//        App.getInstance().getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
//        
//        String t = s.stream().collect(Collectors.joining("/n"));
//        Notifier.showTextNotification(t , "");
//        System.out.println("GGGGG " + t);
    }
 
    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * NOTE: This method is called on the JavaFX Application Thread. 
     */
    @Override
    public void stop() {
        if(initialized) {
            Player.state.serialize();            
            Configuration.save();
            services.getAllServices()
                    .filter(Service::isRunning)
                    .forEach(Service::stop);
        }
        DB.stop();
        Action.stopGlobalListening();
        // remove temporary files
        FileUtil.removeDirContent(TMP_FOLDER());
    }
    
    public static boolean isInitialized() {
        return App.instance.initialized;
    }

    /**
     * Returns applications' main window. Never null, but be aware that the window
     * might not be completely initialized. To find out whether it is, run
     * isGuiInitialized() beforehand.
     * @return window
     */
    public static Window getWindow() {
        return instance.window;
    }
    public static Window getWindowOwner() {
        return instance.windowOwner;
    }
    
    public static<S extends Service> void use(Class<S> type, Consumer<S> action) {
        services.getService(type).filter(Service::isRunning).ifPresent(action);
    }
    
    /**
     * Closes the application. Normally application closes when main window 
     * closes. Therefore this method should not need to be used.
     */
    public static void close() {
        // close window
        instance.windowOwner.close();
        // close app
        Platform.exit();
    }
    
/******************************************************************************/
    
    /**
     * The root location of the application. Equivalent to new File("").getAbsoluteFile().
     * @return absolute file of location of the root directory of this
     * application.
     */
    public static File getLocation() {
        return new File("").getAbsoluteFile();
    }

    /** @return Name of the application. */
    public static String getAppName() {
        return "PlayerFX";
    }
    
    /** @return image of the icon of the application. */
    public static Image getIcon() {
        return new Image(new File("icon512.png").toURI().toString());
    }
    
    /** Github url for project of this application. */
    public static URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");

    /** @return Player state file. */
    public static String PLAYER_STATE_FILE() {
        return "PlayerState.cfg";
    }

    /** @return absolute file of Location of widgets. */
    public static File WIDGET_FOLDER() {
        return new File("Widgets").getAbsoluteFile();
    }

    /** @return absolute file of Location of layouts. */
    public static File LAYOUT_FOLDER() {
        return new File("Layouts").getAbsoluteFile();
    }

    /** @return absolute file of Location of skins. */
    public static File SKIN_FOLDER() {
        return new File("Skins").getAbsoluteFile();
    }

    /** @return absolute file of Location of data. */
    public static File DATA_FOLDER() {
        return new File("UserData").getAbsoluteFile();
    }
    
    /** 
     * Use for temporary files & junk. This folder is emptied on app close.
     * 
     * @return absolute file of directory for temporary files.
     */
    public static File TMP_FOLDER() {
        return new File(DATA_FOLDER(),"Temp");
    }
    
    /** 
     * @return absolute file of Location of database. */
    public static File LIBRARY_FOLDER() {
        return new File(DATA_FOLDER(), "Library");
    }

    /** @return absolute file of Location of saved playlists. */
    public static File PLAYLIST_FOLDER() {
        return new File(DATA_FOLDER(),"Playlists");
    }
    
    public static File LOG_DIR = new File("log").getAbsoluteFile();
    public static File LOG_CONFIG_FILE = new File(LOG_DIR,"log_configuration.xml");
    
    
    // jobs
    
    public static void refreshItemsFromFileJob(List<? extends Item> items) {
        Fut.fut()
           .thenR(() -> Player.refreshItemsWithUpdatedBgr(MetadataReader.readMetadata(items)))
           .showProgress(App.getWindow().taskAdd())
           .run(); 
    }
    
    public static void itemToMeta(Item i, Consumer<Metadata> action) {
       Metadata m = DB.items_byId.get(i.getId());
       if(m!=null) {
           action.accept(m);
       } else {
            Fut.fut(i).then(MetadataReader::create).use(action, FX).run();
       }
    }
    
    
    
/************************************ actions *********************************/
    
    @IsAction(name = "Open github page", descr = "Open github project "
            + "website of this application in default browser. For developers.")
    public static void openAppGithubPage() {
        browse(GITHUB_URI);
    }
    
    @IsAction(name = "Open app dir", descr = "Open application location.")
    public static void openAppLocation() {
        browse(getLocation());
    }
    
    @IsAction(name = "Open css guide", descr = "Open official oracle css "
            + "reference guide. Helps with skinning. For developers.")
    public static void openCssGuide() {
        browse("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html");
    }
    
    @IsAction(name = "Open icon viewer", descr = "Open viewer to browse "
            + "application supported icons. For developers")
    public static void openIconViewer() {
        Fut.fut()
           .thenR(() -> {
                CellPane c = new CellPane(70,80,5);
                c.getChildren().addAll(map(FontAwesomeIcon.values(),i -> new IconInfo(i,55)));
                ScrollPane p = c.scrollable();
                p.setPrefSize(500, 720);
                PopOver o = new PopOver(p);
                runLater(() -> o.show(App_Center));
           })
           .showProgress(Window.getActive().taskAdd())
           .run();
    }
    
    @IsAction(name = "Open settings", descr = "Open preferred "
            + "settings widget to show applciation settings. Widget is open in "
            + "a popup or layout, or already open widget is reused, depending "
            + "on the settings")
    public static void openSettings() {
        WidgetManager.find(ConfiguringFeature.class, WidgetSource.NO_LAYOUT);
    }
}