
package main;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableListBase;
import javafx.scene.ImageCursor;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.services.ClickEffect;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Service;
import AudioPlayer.services.ServiceManager;
import AudioPlayer.services.notif.Notifier;
import AudioPlayer.services.playcount.PlaycountIncrementer;
import AudioPlayer.services.tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import AudioPlayer.tagging.MetadataReader;
import Configuration.*;
import Layout.Component;
import Layout.widget.Widget;
import Layout.widget.WidgetManager;
import Layout.widget.WidgetManager.WidgetSource;
import Layout.widget.feature.ConfiguringFeature;
import Layout.widget.feature.ImageDisplayFeature;
import Layout.widget.feature.PlaylistFeature;
import Layout.widget.impl.Layouts;
import action.Action;
import action.IsAction;
import action.IsActionable;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIconView;
import gui.GUI;
import gui.objects.PopOver.PopOver;
import gui.objects.TableCell.RatingCellFactory;
import gui.objects.TableCell.TextStarRatingCellFactory;
import gui.objects.Window.stage.UiContext;
import gui.objects.Window.stage.Window;
import gui.objects.Window.stage.WindowManager;
import gui.objects.icon.IconInfo;
import gui.pane.ActionPane;
import gui.pane.ActionPane.FastAction;
import gui.pane.ActionPane.FastColAction;
import gui.pane.CellPane;
import gui.pane.ShortcutPane;
import util.ClassName;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.InstanceInfo;
import util.InstanceName;
import util.access.VarEnum;
import util.access.Ѵ;
import util.animation.Anim;
import util.async.future.Fut;
import util.plugin.PluginMap;
import util.serialize.xstream.BooleanPropertyConverter;
import util.serialize.xstream.DoublePropertyConverter;
import util.serialize.xstream.IntegerPropertyConverter;
import util.serialize.xstream.LongPropertyConverter;
import util.serialize.xstream.ObjectPropertyConverter;
import util.serialize.xstream.PlaybackStateConverter;
import util.serialize.xstream.PlaylistItemConverter;
import util.serialize.xstream.StringPropertyConverter;
import util.units.FileSize;

import static Layout.widget.WidgetManager.WidgetSource.ANY;
import static Layout.widget.WidgetManager.WidgetSource.NEW;
import static Layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CSS3;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GITHUB;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.IMAGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.BRUSH;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.EXPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.IMPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.KEYBOARD_VARIANT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static gui.objects.PopOver.PopOver.ScreenPos.App_Center;
import static gui.objects.Window.stage.Window.WINDOWS;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static org.atteo.evo.inflector.English.plural;
import static util.File.Environment.browse;
import static util.Util.getEnumConstants;
import static util.Util.getImageDim;
import static util.UtilExp.setupCustomTooltipBehavior;
import static util.async.Async.*;
import static util.functional.Util.forEachAfter;
import static util.functional.Util.map;
import static util.functional.Util.stream;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layVertically;

/** Application. Represents the program. Single instance. */
@IsActionable
@IsConfigurable("General")
public class App extends Application implements Configurable {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    /** Single instance of the application representing this running application. */
    public static App APP;

    /**
     * Starts program.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


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
    public final EventSource<String> actionStream = new EventSource<>();
    public final AppInstanceComm appCommunicator = new AppInstanceComm();
    public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
    public final AppSerializator serialization = new AppSerializator();
    public final Configuration configuration = new Configuration();

    public Window window;
    public Window windowOwner;
    public final TaskBar taskbarIcon= new TaskBar();
    public final ActionPane actionPane = new ActionPane();
    public final ShortcutPane shortcutPane = new ShortcutPane();
    public final Guide guide = new Guide();
    public boolean normalLoad = true;
    private boolean initialized = false;

    public final ServiceManager services = new ServiceManager();
    public final PluginMap plugins = new PluginMap();

    public final ClassName className = new ClassName();
    public final InstanceName instanceName = new InstanceName();
    public final InstanceInfo instanceInfo = new InstanceInfo();


    @IsConfig(name = "Rating control.", info = "The style of the graphics of the rating control.")
    public final VarEnum<RatingCellFactory> ratingCell = new VarEnum<>(new TextStarRatingCellFactory(),
            () -> plugins.getPlugins(RatingCellFactory.class));

    @IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.", min = 1, max = 10)
    public final IntegerProperty maxRating = new SimpleIntegerProperty(5);

    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
    public final BooleanProperty partialRating = new SimpleBooleanProperty(true);

    @IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    public final BooleanProperty allowRatingChange = new SimpleBooleanProperty(true);

    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
    public final BooleanProperty hoverRating = new SimpleBooleanProperty(true);

    @IsConfig(name = "Debug value (double)", info = "For application testing. Generic number value"
            + "to control some application value manually.")
    public final Ѵ<Double> debug = new Ѵ<>(0d);
    @IsConfig(name = "Debug value (boolean)", info = "For application testing. Generic yes/false value"
            + "to control some application value manually.")
    public final Ѵ<Boolean> debug2 = new Ѵ<>(false,taskbarIcon::setVisible);

    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public String TAG_NO_VALUE = "-- no assigned value --";

    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public String TAG_MULTIPLE_VALUE = "-- multiple values --";

    public boolean ALBUM_ARTIST_WHEN_NO_ARTIST = true;


    public App() {
        if(APP==null) APP = this;
        else throw new RuntimeException("Multiple application instances disallowed");
    }

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
            lc.putProperty("LOG_DIR", DIR_LOG.getPath());
            // override default configuration
            jc.doConfigure(FILE_LOG_CONFIG);
        } catch (JoranException ex) {
            LOGGER.error(ex.getMessage());
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

        // log uncaught thread termination exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread,ex) -> LOGGER.error(thread.getName(), ex));

        // configure serialization
        XStream x = serialization.x;
        Mapper xm = x.getMapper();
        x.autodetectAnnotations(true);
        x.registerConverter(new StringPropertyConverter(xm));
        x.registerConverter(new BooleanPropertyConverter(xm));
        x.registerConverter(new ObjectPropertyConverter(xm));
        x.registerConverter(new DoublePropertyConverter(xm));
        x.registerConverter(new LongPropertyConverter(xm));
        x.registerConverter(new IntegerPropertyConverter(xm));
        x.registerConverter(new Window.WindowConverter());
        x.registerConverter(new PlaybackStateConverter());
        x.registerConverter(new PlaylistItemConverter());
        // x.registerConverter(new ObservableListConverter(xm)); // interferes with Playlist.class
        x.omitField(ObservableListBase.class, "listenerHelper");
        x.omitField(ObservableListBase.class, "changeBuilder");
        x.omitField(ObservableListWrapper.class, "elementObserver");
        x.alias("Component", Component.class);
        x.alias("Playlist", Playlist.class);
        x.alias("item", PlaylistItem.class);
        ClassIndex.getSubclasses(Component.class).forEach(c -> x.alias(c.getSimpleName(),c));
        x.useAttributeFor(Component.class, "id");
        x.useAttributeFor(Widget.class, "name");
        x.useAttributeFor(Playlist.class, "id");

        // add optional object class -> string converters
        className.add(Item.class, "Song");
        className.add(PlaylistItem.class, "Playlist Song");
        className.add(Metadata.class, "Library Song");
        className.add(MetadataGroup.class, "Song Group");
        className.add(List.class, "List");

        // add optional object instance -> string converters
        instanceName.add(Void.class, o -> "none");
        instanceName.add(Item.class, Item::getPath);
        instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
        instanceName.add(Metadata.class,Metadata::getTitle);
        instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
        instanceName.add(Component.class, o -> o.getName());
        instanceName.add(List.class, o -> o.size() + " " + plural("item",o.size()));
        instanceName.add(File.class, File::getPath);

        // add optional object instance -> info string converters
        instanceInfo.add(File.class, o -> {
            HashMap<String,String> m = new HashMap<>();

            FileSize fs = new FileSize(o);
            m.put("Size", fs.toString() + " (" + String.format("%,d ", fs.inBytes()).replace(',', ' ') + "bytes)");
            m.put("Format", FileUtil.getSuffix(o));

            ImageFileFormat iff = ImageFileFormat.of(o.toURI());
            if(iff.isSupported()) {
                Dimension id = getImageDim(o);
                m.put("Resolution", id==null ? "n/a" : id.width + " x " + id.height);
            }
            return m;
        });
        instanceInfo.add(Metadata.class, m -> {
            HashMap<String,String> map = new HashMap<>();
            stream(Metadata.Field.values())
                    .filter(f -> f.isTypeStringRepresentable() && !f.isFieldEmpty(m))
                    .forEach(f ->
                map.put(f.name(), m.getFieldS(f))
            );
            return map;
        });
        instanceInfo.add(PlaylistItem.class, o -> {
            HashMap<String,String> m = new HashMap<>();
            stream(PlaylistItem.Field.values()).filter(f -> f.isTypeString()).forEach(f ->
                m.put(f.name(), (String)o.getField(f))
            );
            return m;
        });

        // register actions
        actionPane.register(Widget.class,
            new FastAction<Widget>("Create launcher (def)","Creates a launcher "
                + "for this widget with default (no predefined) settings. \n"
                + "Opening the launcher with this application will open this "
                + "widget as if it were a standalone application.",
                EXPORT, w -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(DIR_APP);
                                     dc.setTitle("Export to...");
                    File dir = dc.showDialog(Window.getActive().getStage());
                    if(dir!=null) w.exportFxwlDefault(dir);
            })
        );
        actionPane.register(Component.class,
            new FastAction<Component>("Export","Creates a launcher "
                + "for this component with current settings. \n"
                + "Opening the launcher with this application will open this "
                + "component as if it were a standalone application.",
                EXPORT, w -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(LAYOUT_FOLDER());
                                     dc.setTitle("Export to...");
                    File dir = dc.showDialog(Window.getActive().getStage());
                    if(dir!=null) w.exportFxwl(dir);
            })
        );
        actionPane.register(Item.class,
            new FastColAction<Item>("New playlist", "Add items to new playlist widget.",
                PLAYLIST_PLUS,
                is -> WidgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addItems(is)))
        );
        actionPane.register(File.class,
            new FastAction<File>("New playlist", "Add items to new playlist widget.",
                PLAYLIST_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                f -> WidgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addFile(f))),
            new FastAction<File>("Apply skin", "Apply skin on the application.",
                BRUSH,
                FileUtil::isValidSkinFile,
                f -> GUI.setSkin(FileUtil.getName(f))),
            new FastAction<File>("View image", "Opens image in an image browser widget.",
                IMAGE,
                ImageFileFormat::isSupported,
                f -> WidgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImage(f))),
            new FastAction<File>("Open widget", "Opens exported widget.",
                IMPORT,
                f -> f.getPath().endsWith(".fxwl"),
                UiContext::launchComponent)
        );

        // add app parameter handlers
        parameterProcessor.addFileProcessor(
            f -> AudioFileFormat.isSupported(f, Use.APP),
            fs -> WidgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(fs))
        );
        parameterProcessor.addFileProcessor(
            FileUtil::isValidSkinFile,
            fs -> GUI.setSkin(FileUtil.getName(fs.get(0)))
        );
        parameterProcessor.addFileProcessor(
            ImageFileFormat::isSupported,
            fs -> WidgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImages(fs))
        );
        parameterProcessor.addFileProcessor(f -> f.getPath().endsWith(".fxwl"),
            fs -> fs.forEach(UiContext::launchComponent)
        );
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
            // forbid multiple application instances, instead
            // notify the 1st instance of 2nd (this) trying to run and exit
            if(getInstances()>1) {
                appCommunicator.fireNewInstanceEvent(fetchParameters());
                close();
                return;
            }

            // listen to other application instance launches
            // process app parameters of newly started instance
            appCommunicator.start();
            appCommunicator.onNewInstanceHandlers.add(parameterProcessor::process);

            Action.startGlobalListening();

            // custom tooltip behavior
            setupCustomTooltipBehavior(1000, 10000, 200);

            taskbarIcon.setTitle(getName());
            taskbarIcon.setIcon(getIcon());
            taskbarIcon.setOnClose(this::close);
            taskbarIcon.setOnMinimize(v -> WINDOWS.forEach(w -> w.setMinimized(v)));
            taskbarIcon.setOnAltTab(() -> {
                boolean apphasfocus = Window.getFocused()!=null;
                if(!apphasfocus) {
                    boolean allminimized = WINDOWS.stream().allMatch(Window::isMinimized);
                    if(allminimized)
                        WINDOWS.stream().forEach(w -> w.setMinimized(false));
                    else
                        WINDOWS.stream().filter(w -> w.isShowing()).forEach(Window::focus);
                }
            });

            // create window owner - all 'top' windows are owned by it
            windowOwner = Window.createWindowOwner();
            windowOwner.show();

            // discover plugins
            ClassIndex.getAnnotated(IsPluginType.class).forEach(plugins::registerPluginType);
            ClassIndex.getAnnotated(IsPlugin.class).forEach(plugins::registerPlugin);
            WidgetManager.initialize();

            // services must be created before Configuration
            services.addService(new TrayService());
            services.addService(new Notifier());
            services.addService(new PlaycountIncrementer());
            services.addService(new ClickEffect());

            // gather configs
            configuration.collectStatic();
            APP.services.forEach(configuration::collect);
            configuration.collect(this, guide, actionPane);
            configuration.collectComplete();
            // deserialize values (some configs need to apply it, will do when ready)
            configuration.load(FILE_SETTINGS);


            // initializing, the order is important
            Player.initialize();

            List<String> ps = fetchParameters();
            normalLoad = !ps.stream().anyMatch(s->s.endsWith(".fxwl"));

            // load windows, layouts, widgets
            // we must apply skin before we load graphics, solely because if skin defines custom
            // Control Skins, it will only have effect when set before control is created
            // and yes, this means reapplying diferent skin will have no effect in this regard...
            configuration.getFields(f -> f.getGroup().equals("GUI") && f.getGuiName().equals("Skin")).get(0).applyValue();
            WindowManager.deserialize(normalLoad);

            DB.start();

            // animations are nice, but during load the fx thread is completely exhausted
            // i do not see easy wau out of this
            // GUI.setLayoutMode(true);
            // Transition t = par(
            //     Window.windows.stream().map(w ->
            //         seq(
            //             new Anim(at -> ((SwitchPane)w.getSwitchPane()).zoomProperty().set(1-0.6*at))
            //                     .dur(500).intpl(new CircularInterpolator()),
            //             par(
            //                 par(
            //                     forEachIStream(w.left_icons.box.getChildren(),(i,icon)->
            //                         new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
            //                 ),
            //                 par(
            //                     forEachIRStream(w.right_icons.box.getChildren(),(i,icon)->
            //                         new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
            //                 ),
            //                 par(
            //                     w.getSwitchPane().getLayouts().values().stream()
            //                      .flatMap(l -> l.getAllWidgets())
            //                      .map(wi -> (Area)wi.load().getUserData())
            //                      .map(a ->
            //                         seq(
            //                             new Anim(a.content_root::setOpacity).dur(2000+random()*1000).intpl(0),
            //                             new Anim(a.content_root::setOpacity).dur(700).intpl(isAroundMin1(0.04, 0.1,0.2,0.3))
            //                         )
            //                      )
            //                 )
            //             ),
            //             par(
            //                 new Anim(at -> ((SwitchPane)w.getSwitchPane()).zoomProperty().set(0.4+0.7*at))
            //                         .dur(500).intpl(new CircularInterpolator())
            //             )
            //         )
            //     )
            // );
            // t.setOnFinished(e -> {
            //     GUI.setLayoutMode(false);
            // });
            // t.play();

            initialized = true;

        } catch(Exception e) {
            LOGGER.info("Application failed to start", e);
            LOGGER.error("Application failed to start", e);
        }

        // initialization is complete -> apply all settings
        configuration.getFields().forEach(Config::applyValue);

        // initialize non critical parts
        if(normalLoad) Player.loadLast();

        // show guide
        if(guide.first_time) run(3000, guide::start);

        // get rid of this, load from skins
        Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());  //pass in the image path
        ImageCursor c = new ImageCursor(image,3,3);
        window.getStage().getScene().setCursor(c);

        // process app parameters passed when app started
        parameterProcessor.process(fetchParameters());
    }

    public static boolean isInitialized() {
        return App.APP.initialized;
    }

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * NOTE: This method is called on the JavaFX Application Thread.
     */
    @Override
    public void stop() {
        if(initialized) {
            if(normalLoad) Player.state.serialize();
            if(normalLoad) WindowManager.serialize();
            configuration.save(getName(),FILE_SETTINGS);
            services.getAllServices()
                    .filter(Service::isRunning)
                    .forEach(Service::stop);
        }
        DB.stop();
        Action.stopGlobalListening();
        appCommunicator.stop();
    }

    /** Forces application to stop. Invokes {@link #stop()} as a result. */
    public void close() {
        // close app
        Platform.exit();
    }

    public <S extends Service> void use(Class<S> type, Consumer<S> action) {
        services.getService(type).filter(Service::isRunning).ifPresent(action);
    }

/******************************************************************************/

    public List<String> fetchParameters() {
        List<String> params = new ArrayList<>();
        getParameters().getRaw().forEach(params::add);
        // getParameters().getUnnamed().forEach(params::add);
        // getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
        return params;
    }

    /**
     * Calculates number of instances of this application running on this
     * system at this moment. In this context, application instance is considered
     * a application running on java virtual machine from user directory equal
     * to that of this application. This application is included in the count.
     *
     * @return number of running instances
     */
    public static int getInstances() {
        // try {
        //     MonitoredHost mh = MonitoredHost.getMonitoredHost("//localhost" );
        //     for (int id : mh.activeVms() ) {
        //         VmIdentifier vmid = new VmIdentifier("" + id);
        //         MonitoredVm vm = mh.getMonitoredVm(vmid, 0 );
        //         System.out.printf( "%d %s %s %s%n\n", id,MonitoredVmUtil.mainClass( vm, true ),
        //                            MonitoredVmUtil.jvmArgs( vm ),MonitoredVmUtil.mainArgs( vm ) );
        //     }
        // } catch (MonitorException ex) {
        //     java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        // } catch (URISyntaxException ex) {
        //     java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        // }
        // two occurrences of the result of MonitoredVmUtil.mainClass(), the program is started twice

        int i=0;
        String ud = System.getProperty("user.dir");

        for(VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            try {
                VirtualMachine vm = VirtualMachine.attach(vmd);
                String udi = vm.getSystemProperties().getProperty("user.dir");
                if(ud.equals(udi)) i++;
                vm.detach();
            } catch (AttachNotSupportedException | IOException ex) {
                LOGGER.warn("Unable to inspect virtual machine {}", vmd);
            }
        }
        return i;
    }

    /** @return name of the application. */
    public String getName() {
        return "PlayerFX";
    }


    public File getLocation() {
        return new File("").getAbsoluteFile();
    }

    /** @return image of the icon of the application. */
    public Image getIcon() {
        return new Image(new File("icon512.png").toURI().toString());
    }

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
     * @return absolute file of Location of database. */
    public static File LIBRARY_FOLDER() {
        return new File(DATA_FOLDER(), "Library");
    }

    /** @return absolute file of Location of saved playlists. */
    public static File PLAYLIST_FOLDER() {
        return new File(DATA_FOLDER(),"Playlists");
    }

    /** Absolute file of directory of this app. Equivalent to new File("").getAbsoluteFile(). */
    public final File DIR_APP = new File("").getAbsoluteFile();
    /** Temporary directory of the os. */
    public final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
    /** File for application configuration. */
    public final File FILE_SETTINGS = new File(DIR_APP,"settings.cfg");
    /** Directory for application logging. */
    public final File DIR_LOG = new File("log").getAbsoluteFile();
    /** File for application logging configuration. */
    public final File FILE_LOG_CONFIG = new File(DIR_LOG,"log_configuration.xml");

    /** Url for github website for project of this application. */
    public final URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");


    // jobs

    public static void refreshItemsFromFileJob(List<? extends Item> items) {
        Fut.fut()
           .then(() -> Player.refreshItemsWith(MetadataReader.readMetadata(items)),Player.IO_THREAD)
           .showProgress(APP.window.taskAdd())
           .run();
    }

    public static void itemToMeta(Item i, Consumer<Metadata> action) {
       if (i.same(Player.playingtem.get())) {
           action.accept(Player.playingtem.get());
           return;
       }

       Metadata m = DB.items_byId.get(i.getId());
       if(m!=null) {
           action.accept(m);
       } else {
            Fut.fut(i).map(MetadataReader::create,Player.IO_THREAD)
                      .use(action, FX).run();
       }
    }



/************************************ actions *********************************/

    @IsAction(name = "Open on github", desc = "Opens github page for this application. For developers.")
    public static void openAppGithubPage() {
        browse(APP.GITHUB_URI);
    }

    @IsAction(name = "Open app directory", desc = "Opens directory from which this application is "
            + "running from.")
    public static void openAppLocation() {
        browse(APP.DIR_APP);
    }

    @IsAction(name = "Open css guide", desc = "Opens css reference guide. For developers.")
    public static void openCssGuide() {
        browse(URI.create("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html"));
    }

    @IsAction(name = "Open icon viewer", desc = "Opens application icon browser. For developers.")
    public static void openIconViewer() {

        try {
            Font.loadFont(App.class.getResource(FontAwesomeIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(App.class.getResource(WeatherIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(App.class.getResource(MaterialDesignIconView.TTF_PATH).openStream(), 10.0);
            Font.loadFont(App.class.getResource(MaterialIconView.TTF_PATH).openStream(), 10.0);
        } catch (IOException e) {
            LOGGER.error("Couldnt load font",e);
        }

        StackPane root = new StackPane();
                  root.setPrefSize(600, 720);
        List<Button> typeicons = stream(FontAwesomeIcon.class,WeatherIcon.class,
                                      MaterialDesignIcon.class,MaterialIcon.class)
                .map((Class c) -> {
                    Button b = new Button(c.getSimpleName());
                    b.setOnMouseClicked(e -> {
                        if(e.getButton()==PRIMARY) {
                            if(b.getUserData()!=null) {
                                root.getChildren().setAll((ScrollPane)b.getUserData());
                            } else {
                                CellPane cp = new CellPane(70,80,5);
                                ScrollPane sp = cp.scrollable();
                                b.setUserData(sp);
                                runFX(() -> root.getChildren().setAll(sp));
                                Fut.fut()
                                   .then(() ->
                                       forEachAfter(2, map(getEnumConstants(c),i -> new IconInfo((GlyphIcons)i,55)), i -> {
                                           runFX(() -> {
                                               cp.getChildren().add(i);
                                               new Anim(i::setOpacity).dur(500).intpl(x -> sqrt(x)).play(); // animate
                                           });
                                       })
                                   )
                                   .showProgress(Window.getActive().taskAdd())
                                   .run();
                            }
                            e.consume();
                        }
                    });
                    return b;
                }).collect(toList());
        PopOver o = new PopOver(layVertically(20,TOP_CENTER,layHorizontally(8,CENTER,typeicons), root));
                o.show(App_Center);
    }

    @IsAction(name = "Open settings", desc = "Opens application settings.")
    public static void openSettings() {
        WidgetManager.use(ConfiguringFeature.class, WidgetSource.NO_LAYOUT, c -> c.configure(APP.configuration.getFields()));
    }

    @IsAction(name = "Open layout manager", desc = "Opens layout management widget.")
    public static void openLayoutManager() {
        WidgetManager.findExact(Layouts.class, WidgetSource.NO_LAYOUT);
    }
    @IsAction(name = "Open guide", desc = "Resume or start the guide.")
    public static void openGuide() {
        APP.guide.open();
    }

    @IsAction(name = "Open app actions", desc = "Actions specific to whole application.")
    public static void openActions() {
        APP.actionPane.show(Void.class, null,
            new FastAction<Void>(
                "Export widgets",
                "Creates launcher file in the destination directory for every widget.\n"
                + "Launcher file is a file that when opened by this application opens the widget. "
                + "If application was not running before, it will not load normally, "
                + "but will only open the widget.\n"
                + "Essentially, this exports the widgets as 'standalone' applications",
                EXPORT,
                ignored -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(App.LAYOUT_FOLDER());
                                     dc.setTitle("Export to...");
                    Window aw = Window.getActive();
                    File dir = dc.showDialog(aw.getStage());
                    if(dir!=null) {
                        WidgetManager.getFactories().forEach(w -> w.create().exportFxwlDefault(dir));
                    }
                }
            ),
            new FastAction<Void>(KEYBOARD_VARIANT,Action.get("Show shortcuts")),
            new FastAction<Void>(GITHUB,Action.get("Open on github")),
            new FastAction<Void>(CSS3,Action.get("Open css guide")),
            new FastAction<Void>(IMAGE,Action.get("Open icon viewer")),
            new FastAction<Void>(FOLDER,Action.get("Open app directory"))
        );
    }

    @IsAction(name = "Show shortcuts", desc = "Display all available shortcuts.", keys = "?")
    public static void showShortcuts() {
        APP.shortcutPane.show();
    }

}