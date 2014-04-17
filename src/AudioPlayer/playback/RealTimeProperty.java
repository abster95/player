/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import utilities.functional.functor.Procedure;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

/**
 *
 * @author uranium
 */
public final class RealTimeProperty {
    final ObjectProperty<Duration> totalTime;
    final ObjectProperty<Duration> currentTime;
    final ObjectProperty<Duration> realTime = new SimpleObjectProperty<>(Duration.ZERO);
    int count = 0;
   
    // temp
    public Duration curr_sek = Duration.ZERO;
    public Duration real_seek = Duration.ZERO;
    boolean eventT_fired = false; // prevention so event fires only once
    boolean eventP_fired = false; // prevention so event fires only once
    
    public RealTimeProperty(ObjectProperty<Duration> _totalTime, ObjectProperty<Duration> _currentTime) {
        totalTime = _totalTime;
        currentTime = _currentTime;
    }
    void initialize() {
        // bind realTime to playback
        currentTime.addListener((Observable o) -> {
            Duration d = real_seek.add(currentTime.get().subtract(curr_sek));
            realTime.set(d);
        });
        // update count
        setOnTimeAt(new PercentTimeEventHandler(1, () -> {
            count++;
        }));        
    }
    
    void synchroRealTime_onPlayed() {
        real_seek = Duration.ZERO;
        curr_sek = Duration.ZERO;
        count = 0;
    }
    void synchroRealTime_onStopped() {
        real_seek = Duration.ZERO;
        curr_sek = Duration.ZERO;
        count = 0;
    }
    void synchroRealTime_onPreSeeked() {
        real_seek = realTime.get();
    }
    void synchroRealTime_onPostSeeked(Duration duration) {
        curr_sek = duration;
    }
    
/******************************************************************************/
    
    /** @return value of this property */
    public Duration get() {
        return realTime.get();
    }
    
    public ObjectProperty<Duration> realTimeProperty() {
        return realTime;
    }
    
    public void addListener(InvalidationListener listener) {
        realTime.addListener(listener);
    }
    public void removeListener(InvalidationListener listener) {
        realTime.removeListener(listener);
    }
    
    /**
     * Registers specified behavior to behave when real played time of the played
     * item reaches certain portion of total time.
     * For example doSometing(); when item has been playing for exactly
     * 50% of its total time.
     * @param h
     */
    public void setOnTimeAt(PercentTimeEventHandler h) {
        if (percentHandlers.isEmpty()) 
            realTime.addListener(percentEventDistributor);
        percentHandlers.add(h);
    }
    /**
     * Removes the handler if it has been registered before.
     * @param h
     */
    public void removeOnTimeAt(PercentTimeEventHandler h) {
        if (h != null)
           percentHandlers.remove(h);
        if ( percentHandlers.isEmpty()) 
           realTimeProperty().removeListener(percentEventDistributor);
    }
    /**
     * Registers specified behavior to behave when real played time of the played
     * item reaches certain point.
     * For example doSometing(); when item has been playing for exactly 10 seconds
     * @param h
     */
    public void setOnTimeAt(TimeEventHandler h) {
        if (timeHandlers.isEmpty())
            realTime.addListener(timeEventDistributor);
        timeHandlers.add(h);
    }
    /**
     * Removes the handler if it has been registered before.
     * @param h 
     */
    public void removeOnTimeAt(TimeEventHandler h) {
        if (h != null)
           timeHandlers.remove(h);
        if (timeHandlers.isEmpty()) 
           realTimeProperty().removeListener(timeEventDistributor);
    }
    
//*****************************************************************************/
    
    private final List<PercentTimeEventHandler> percentHandlers = new ArrayList<>();
    private final List<TimeEventHandler> timeHandlers = new ArrayList<>();
    
    private final InvalidationListener percentEventDistributor = (Observable o) -> {
        percentHandlers.stream().forEach(PercentTimeEventHandler::handle);
    };
    
    private final InvalidationListener timeEventDistributor = (Observable o) -> {
        timeHandlers.forEach(TimeEventHandler::handle);
    };

    
}