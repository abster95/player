
package main;

import Action.Action;
import AudioPlayer.Player;
import AudioPlayer.playback.PlaycountIncrementer;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.MoodManager;
import Configuration.Configuration;
import GUI.GUI;
import GUI.NotifierManager;
import GUI.Window;
import GUI.WindowManager;
import Layout.Widgets.WidgetManager;
import Library.BookmarkManager;
import java.io.File;
import java.net.URI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;


/**
 * Application. Launches and terminates program.
 */
public class App extends Application {

    /**
     * Starts program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
/******************************************************************************/
    
    // NOTE: for some reason cant make fields final in this class +
    // initializing fields right up here (or constructor) will have no effect
    public static Window window;
    private Window windowOwner;
    private boolean initialized = false;
    
    private static App instance;
    
    public App() {
        instance = this;
    }
    
//    /** Returns instance of this app singleton. */
//    public static App getInstance() {
//        return instance;
//    }
    
/******************************************************************************/
    
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
        Action.startGlobalListening();
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
            
        
        Configuration.load();           // must initialize first
        
        try {
            // initializing, the order is important
            Player.initialize();
            WidgetManager.initialize();             // must initialize before below
            GUI.initialize();                       // must initialize before below
          
            // create hidden main window
            windowOwner = Window.createWindowOwner();
            windowOwner.show();
            
            // we need to initialize skin before windows do
            Configuration.applyField("skin");
            
            DB.start();
            
            // initialize windows from previous session
            WindowManager.deserialize();
            
            initialized = true;
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Application failed to start. Reason: " + e.getMessage());
        }
        
        // initialize non critical parts
        Player.loadLast();                      // should load in the end

        NotifierManager.start();           // after window is shown (and css aplied)
        PlaycountIncrementer.initialize();
        MoodManager.initialize();
        Action.getActions().values().forEach(Action::register);        

        Configuration.applyFieldsAll();         // apply all (and gui) settings
        
        
        
        // playing with parameters/ works, but how do i pass param when executing this from windows?
        
//        List<String> s = new ArrayList<>();
//        System.out.println("raw");
//        App.getInstance().getParameters().getRaw().forEach(s::add);
//        System.out.println("unnamed");
//        App.getInstance().getParameters().getUnnamed().forEach(s::add);
//        System.out.println("named");
//        App.getInstance().getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
//        
//        String t = s.stream().collect(Collectors.joining("/n"));
//        NotifierManager.showTextNotification(t , "");
//        System.out.println("GGGGG " + t);
    }
 
    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * NOTE: This method is called on the JavaFX Application Thread. 
     */
    @Override
    public void stop() {
        Action.stopGlobalListening();
        if(initialized) {
            Player.state.serialize();            
            Configuration.save();
            BookmarkManager.saveBookmarks();
            NotifierManager.stop();
            
        }
        DB.stop();
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
        return new Image(new File("icon.png").toURI().toString());
    }
    
    /** @return github link for this application. */
    public static URI getGithubLink() {
        return URI.create("https://www.github.com/sghpjuikit/player/");
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
    /** @return absolute file of Location of data. */
    public static File LIBRARY_FOLDER() {
        return new File(DATA_FOLDER(), "Library");
    }

    /** @return absolute file of Location of saved playlists. */
    public static File PLAYLIST_FOLDER() {
        return new File(DATA_FOLDER(),"Playlists");
    }
}