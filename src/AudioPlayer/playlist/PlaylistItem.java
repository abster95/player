
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playlist;

import PseudoObjects.FormattedDuration;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import utilities.FileUtil;
import utilities.Log;

/**
 * Playlist item implementation. Defines item in playlist.
 * 
 * As a playlist item this object bears three information: artist, title and
 * time duration, besides URI as an Item object.
 * 
 * Cannot be changed, only updated. In order to limit object initialization
 * performance impact of I/O tag read operation only URI is necessary to
 * instantiate this class. In such case, the other fields must be manually
 * updated by calling the update() method.
 * 
 * SERIALIZATION
 * - this class is serializable by XStream using PlaylistItemConverter.
 * @warn
 * Dont try to change property implementations SimpleObjectProperty
 * into more generic ObjectProperty. It will cause XStream serializing
 * to malperform (java7)(needs more testing).
 */
public final class PlaylistItem extends Item implements Comparable<PlaylistItem> {
    private final SimpleObjectProperty<URI> uri;
    private final SimpleObjectProperty<FormattedDuration> time;
    /** Consists of item's artist and title separated by separator string. */
    private final SimpleStringProperty name;
    private boolean updated = false;
    
    /**
     * URI Constructor.
     * Use when only uri is known. Item is initialized to updated = false. Use
     * update() method to get other fields - update the item.
     * @param _uri 
     */
    public PlaylistItem(URI _uri) {
        uri = new SimpleObjectProperty<>(_uri);
        name = new SimpleStringProperty(getPath());
        time = new SimpleObjectProperty<>(new FormattedDuration(0));
    }
    /**
     * Item Constructor.
     * Use to convert Item to PlaylistItem. PlaylistItem is initialized to
     * updated = false. Use update() method to get other fields - update the item.
     * @param item
     */
    public PlaylistItem(Item item) {
        uri = new SimpleObjectProperty<>(item.getURI());
        name = new SimpleStringProperty(getPath());
        time = new SimpleObjectProperty<>(new FormattedDuration(0));
    }
    /**
     * Full Constructor.
     * Use when all info is available. Item is initialized to updated state,
     * which avoids updating it and consequent I/O operation.
     * @param new_uri
     * @param new_name
     * @param length of the item in miliseconds.
     */
    public PlaylistItem(URI new_uri, String new_name, double length) {
        uri = new SimpleObjectProperty<>(new_uri);
        name = new SimpleStringProperty(new_name);
        time = new SimpleObjectProperty<>(new FormattedDuration(length));
        updated = true;
    }
    
    /**
     * @return the url. Never null.
     */
    @Override
    public URI getURI() {
        return uri.get();
    }
    
    public StringProperty uriProperty() {
        return name;
    }
    
    /**
     * @return value of the {@link #name name}. Never null.
     */
    public String getName() {
        return name.get();
    }
    
    /**
     * @return {@link #name name property}.
     */
    public StringProperty nameProperty() {
        return name;
    }
    
    /**
     * @return the artist portion of the name. Empty string if item
     * wasnt updated yet. Never null.
     */
    public String getArtist() {
        if(!updated()) return "";
        String s = name.get();
        return s.substring(0, s.indexOf(" - "));
    }
    
    /**
     * @return the title portion of the name. Empty string if item
     * wasnt updated yet. Never null.
     */
    public String getTitle() {
        if(!updated()) return "";
        String s = name.get();
        return s.substring(s.indexOf(" - ")+3);
    }
    
    /**
     * @return the time. 0 if item wasnt updated yet. Never null.
     */
    public FormattedDuration getTime() {
        return time.get();
    }
    
    /**
     * @return time in milliseconds. 0 if item wasnt updated yet.
     */
    public double getTimeMillis() {
        return time.get().toMillis();
    }
    
    /**
     * Until the item is updated the value wrapped inside the property
     * will be 0.
     * @return 
     */
    public SimpleObjectProperty<FormattedDuration> timeProperty() {
        return this.time;
    }

    /**
     * Updates this item by reading the tag of the source. Dont use this method
     * for lots of items at once on application thread! For that use bgr thread.
     * run
     */
    public void update() {
        if (isCorrupt()) return;
        try {
            // read tag for data
            AudioFile f;
            f = AudioFileIO.read(getFile());
            Tag t = f.getTag();
            AudioHeader h = f.getAudioHeader();
            
            // get values
            String artist = t.getFirst(FieldKey.ARTIST);
            String title = t.getFirst(FieldKey.TITLE);
            if (title.isEmpty())
                title = FileUtil.getName(getURI());
            String _name = artist + " - " + title;
            
            double _length = 1000 * h.getTrackLength();
            
            // set values
            name.set(_name);
            time.set(new FormattedDuration(_length));
            updated = true;
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException ex) {
            Log.err("Playlist item update failed.\n"+getURI());
        }
    }
    /**
     * Returns true if the item was marked updated. Once item is updated it will
     * stay in that state. Updated item guarantees that all its values are
     * valid, but doesnt guarantee that they are up to date. For manipulation
     * within the application there should be no need to update the item again.
     * If the item changes, the change should be handled by the application.
     * 
     * This method doesnt solve is as it marks items with invalid data. Because
     * there is no guarantee that every item is valid, when it is required for 
     * item to be, update() can be called after this method returns false. 
     * 
     * Creating item in URI constructor will result in initialization of
     * updated to false. Method update() then must be called manually.
     * @return 
     */
    public boolean updated() {
        return updated;
    }

    @Override
    public String toString() {
        return getName().toString() + "\n"
             + getURI().toString() + "\n"
             + getTime().toString() + "\n";
    }
    
    /**
     * @return SimplePlaylistItem representation of this item.
     */
    public SimplePlaylistItem toSimple() {
        return new SimplePlaylistItem(this);
    }
    
    /** Compares by natural order - name; */
    @Override
    public int compareTo(PlaylistItem o) {
        return getName().compareToIgnoreCase(o.getName());
    }
    
    /**  @return Comparator. Compares by length - time.  */    
    public static Comparator<PlaylistItem> getComparatorTime() {
        return (p1,p2) -> p1.getTime().compareTo(p2.getTime());
    }
    
    /**
     * Clones the item.
     * @param item
     * @return clone of the item or null if parameter null.
     */
    public static PlaylistItem clone(PlaylistItem item) {
        if (item==null) return null;
        
        URI uri = item.getURI();
        String name = item.getName();
        double length = item.getTimeMillis();
        PlaylistItem i = new PlaylistItem(uri, name, length);
                     i.updated = item.updated; 
        return i;
    }

}
