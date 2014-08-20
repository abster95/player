package AudioPlayer.services.LastFM;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PercentTimeEventHandler;
import AudioPlayer.playback.TimeEventHandler;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Configuration.MapConfigurable;
import Configuration.ValueConfig;
import GUI.objects.SimpleConfigurator;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.Log;
import utilities.Password;
import utilities.TODO;
import utilities.functional.functor.ChangeConsumer;

/**
 *
 * @author Michal
 */
@IsConfigurable("LastFM")
public class LastFMManager {

    private static String username;
    private static final String apiKey = "f429ccceafc6b81a6ffad442cec758c3";
    private static final String secret = "8097fcb4a54a9805599060e47ab69561";

    private static Session session;
    private static final Preferences preferences = Preferences.userNodeForPackage(LastFMManager.class);

    private static boolean percentSatisfied;
    private static boolean timeSatisfied;

    
    private static boolean loginSuccess;
    private boolean durationSatisfied;
    @IsConfig(name = "Scrobbling on")
    private static final BooleanProperty scrobblingEnabled = new SimpleBooleanProperty(false){
        @Override
        public void set(boolean nv) {         
            if (nv){
                if(isLoginSet()){
                    session = Authenticator.getMobileSession(
                            acquireUserName(), 
                            acquirePassword().get(), 
                            apiKey, secret);
                    Result lastResult = Caller.getInstance().getLastResult();                    
                    if(lastResult.getStatus() != Result.Status.FAILED){
                        LastFMManager.setLoginSuccess(true);
                        LastFMManager.start();
                        super.set(true);
                    }else{
                        LastFMManager.setLoginSuccess(false);
                        LastFMManager.stop();
                        super.set(false);
                    }
                }
            } else {
                LastFMManager.stop();
                super.set(false);
            }
        }
    };

 
    
    private static void setLoginSuccess(boolean b) {
       loginSuccess = b; 
    }
    public static boolean isLoginSuccess(){
        return loginSuccess;
    }
    public static String getHiddenPassword() {
        return "****";
    }



    public static void toggleScrobbling() {
        scrobblingEnabled.set(!scrobblingEnabled.get());
    }


    public LastFMManager() { }

    public static void start() {
        playingItemMonitoring = Player.getCurrent().subscribeToChanges(itemChangeHandler);

        PLAYBACK.realTimeProperty().setOnTimeAt(timeEvent);
        PLAYBACK.realTimeProperty().setOnTimeAt(percentEvent);
    }
    
    
    @TODO("Implement")
    public static boolean isLoginSet() {
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        return !"".equals(acquireUserName());
    }
    
    public static void saveLogin(String value, Password value0) {
        if(saveUserName(value) && savePassword(value0)){
            scrobblingEnabled.set(true);
        }else{
            scrobblingEnabled.set(false);
        } 
    }
    
    
    public static SimpleConfigurator getLastFMconfig(){
        return new SimpleConfigurator<>(
            new MapConfigurable(
                new ValueConfig("Username", LastFMManager.acquireUserName()),
                new ValueConfig("Password", LastFMManager.acquirePassword())                                  
            ), 
            vc -> LastFMManager.saveLogin(
               (String)vc.getField("Username").getValue(),
               (Password)vc.getField("Password").getValue())                                     
        );    

    }
    
    public static final boolean saveUserName(String username) {
        preferences.put("lastfm_username", username);  
        return preferences.get("lastfm_username", "").equals(username);
        
    }
    public static final boolean savePassword(Password pass) {
        preferences.put("lastfm_password", pass.get());
        return preferences.get("lastfm_password", "").equals(pass.get());
    }
    public static String acquireUserName() {
        return preferences.get("lastfm_username", "");
    }
    private static Password acquirePassword(){
        return new Password(preferences.get("lastfm_password", ""));
    }
    
    /************** Scrobble logic - event handlers etc ***********************/
     
    public static final void updateNowPlaying() {
        Metadata currentMetadata = AudioPlayer.Player.getCurrent().get();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );
    }

    private static void scrobble(Metadata track) {
        Log.info("Scrobbling: " + track.getArtist() + " - " + track.getTitle());
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleResult result = Track.scrobble(track.getArtist(), track.getTitle(), now, session);
    }
    
    private static void reset() {
        timeSatisfied = percentSatisfied = false;
    }
    
    private static Subscription playingItemMonitoring;
       
    private static final PercentTimeEventHandler percentEvent = new PercentTimeEventHandler(
            0.5,
            () -> {
                Log.deb("Percent event for scrobbling fired");
                setPercentSatisfied(true);
            },
            "LastFM percent event handler.");

    private static final TimeEventHandler timeEvent = new TimeEventHandler(
            Duration.minutes(4),
            () -> {
                Log.deb("Time event for scrobbling fired");
                setTimeSatisfied(true);
            },
            "LastFM time event handler");
    
    private static final ChangeConsumer<Metadata> itemChangeHandler = (ov,nv) -> {
            if ((timeSatisfied || percentSatisfied)
                    && ov.getLength().greaterThan(Duration.seconds(30))) {
                scrobble(ov);
//                System.out.println("Conditions for scrobling satisfied. Track should scrobble now.");
            }
            updateNowPlaying();
            reset();
        };
    
/***************************   GETTERS and SETTERS    *************************/
    
    public static boolean getScrobblingEnabled() {
        return scrobblingEnabled.get();
    }

    public static BooleanProperty scrobblingEnabledProperty() {
        return scrobblingEnabled;
    }

 
    public void setScrobblingEnabled(boolean value) {
        LastFMManager.scrobblingEnabled.set(value);
    }   
    
    
    private static void setTimeSatisfied(boolean b) {
        timeSatisfied = b;
    }

    private static void setPercentSatisfied(boolean b) {
        percentSatisfied = b;
    }
    
    
    
      public static void stop() {
        if (playingItemMonitoring!=null) playingItemMonitoring.unsubscribe();
        PLAYBACK.realTimeProperty().removeOnTimeAt(percentEvent);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeEvent);
    }
}
