
package AudioPlayer.tagging;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Chapters.Chapter;
import AudioPlayer.tagging.Chapters.MetadataExtended;
import AudioPlayer.tagging.Cover.Cover;
import AudioPlayer.tagging.Cover.Cover.CoverSource;
import AudioPlayer.tagging.Cover.FileCover;
import AudioPlayer.tagging.Cover.ImageCover;
import Configuration.Configuration;
import PseudoObjects.FormattedDuration;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.images.Artwork;
import utilities.AudioFileFormat;
import static utilities.AudioFileFormat.UNKNOWN;
import utilities.FileUtil;
import static utilities.FileUtil.EMPTY_COLOR;
import static utilities.FileUtil.EMPTY_URI;
import utilities.ImageFileFormat;
import utilities.Log;
import utilities.Parser.ColorParser;
import utilities.TODO;
import utilities.Util;

/**
 * Information about audio file.
 * Provided by reading tag and audio file header (mostly).
 * Everything that is possible to know about the audio file is accessible through
 * this class. This class also provides some non-tag, application specific
 * or external information associated with this song, like cover files.
 * <p>
 * The class is practically immutable and does not provide any setters, nor
 * allows updating of its state or any of its values.
 * <p>
 * Metadata can be empty. See {@link #EMPTY}
 * <p>
 * The getters of this class return mostly string. For empty fields the output
 * is "" (empty string) and for non-string getters it varies, but it is never 
 * null. See documentation
 * for the specific field. The rule is however, that the check for empty value
 * should be necessary only in rare cases.
 * <p>
 * Every field returns string, primitive, or Object with toString method that
 * returns the best possible string representation of the field's value, including
 * its empty value.
 * Therefore, if framework were to be used, simply obtaining values for all
 * fields and using natural String (toString()) conversion is all that is needed.
 * As for how to leverage this: see {@link #getField()}.
 * 
 * @author uranium
 */
@Entity(name = "Item")
public final class Metadata extends MetaItem {
    // note: for some fields the initialized values below are not their
    // 'default' values. Rather, those are autogenerated when requested
    
    // note: do not rename the fields! The names are basically hardcoded in JPA
    // queries
    
    // identification fields
    @Id
    private String uri;
    // header fields
    private AudioFileFormat format = UNKNOWN;
    private long filesize = 0;
    private String encoding_type = "";
    private int bitrate = -1;
    private String encoder = "";
    private String channels = "";
    private String sample_rate = "";
    private double duration = 0;
    // tag fields
    private String title = "";
    private String album = "";
    private String artist = "";
    private List<String> artists = null;
    private String album_artist = "";
    private String composer = "";
    private String publisher = "";
    private String track = "";
    private String tracks_total = "";
    private String disc = "";
    private String discs_total = "";
    private String genre = "";
    private String year = "";
    @Transient
    private Artwork cover = null;
    private int rating = -1;
    private int playcount = -1;
    private String category = "";
    private String comment = "";
    private String lyrics = "";
    private String mood = "";
    private String custom1 = "";
    private String custom2 = "";
    private String custom3 = "";
    private String custom4 = "";
    private String custom5 = "";
    
    /** 
     * EMPTY metadata. Substitute for null. Always use instead of null. Also
     * corrupted items should transform into EMPTY metadata.
     * <p>
     * All fields are at their default values.
     * <p>
     * There are two ways to check whether Metadata object is EMPTY. Either use
     * reference operator this == Metadata.EMPTY or call {@link #isEmpty()}.
     * <p>
     * Note: The reference operator works, because there is always only one
     * instance of EMPTY metadata.
     * <p>
     * Note: EMPTY metadata must never be serialized.
     */
    public static final Metadata EMPTY = new Metadata();
    
    // public - this type of metadata is simply a conversion, harmless & we need
    // to allow the access from Item subclasses
    /**
     * Creates metadata from specified item. Attempts to fill as many metadata 
     * information fields from the item as possible, leaving everything else empty.
     * For example, if the item is playlist item, sets artist, length and title fields.
     */
    public Metadata(Item item) {
        uri = item.getURI().toString();
        if(item instanceof PlaylistItem) {
            PlaylistItem pitem = (PlaylistItem)item;
            artist = pitem.getArtist();
            duration = pitem.getTime().toMillis();
            title = pitem.getTitle();
        }
    }
    
    // constructs empty metadata
    // private - do not allow anyone create new empty metadata
    private Metadata() {
        uri = EMPTY_URI.toString();
    }
    
    // the constructor for creating new metadata by reading the tag
    // package access - only allow the MetadataReaders to create metadata like this
    /**
     * Reads and creates metadata info for file.
     * Everything is read at once. There is no additional READ operation after the
     * object has been created.
     * 
     */
    Metadata(AudioFile audiofile) {
        uri = audiofile.getFile().getAbsoluteFile().toURI().toString();
        filesize = super.getFilesize().inBytes();
        
        loadGeneralFields(audiofile);
        switch (getFormat()) {
            case mp3:   loadSpecificFieldsMP3((MP3File)audiofile);
                        break;
            case flac:  //FLACFile flac = (MP3File)audioFile;
                        loadSpecificFieldsFLAC();
                        break;
            case ogg:   //MP3File ogg = (MP3File)audioFile;
                        loadSpecificFieldsOGG();
                        break;
            case wav:   //MP3File ogg = (MP3File)audioFile;
                        loadSpecificFieldsWAV();
                        break;
            default:
        }        
        
    }
    /** loads all generally supported fields  */
    private void loadGeneralFields(AudioFile aFile) {
        Tag tag = aFile.getTag();
        AudioHeader header = aFile.getAudioHeader();
        
        // format and encoding type are switched in jaudiotagger library...
        format = AudioFileFormat.of(getURI());
        if (format==UNKNOWN) format = super.getFormat();
        bitrate = Bitrate.create(header.getBitRateAsNumber()).getValue();
        duration = 1000 * header.getTrackLength();
        encoding_type = Util.emptifyString(header.getFormat());
        channels = Util.emptifyString(header.getChannels());
        sample_rate = Util.emptifyString(header.getSampleRate());
        
        encoder = getGeneral(tag,FieldKey.ENCODER);
        
        title = getGeneral(tag,FieldKey.TITLE);
        album = getGeneral(tag,FieldKey.ALBUM);
//        artists = getGenerals(tag,FieldKey.ARTIST);
        artist = getGeneral(tag,FieldKey.ARTIST);
        album_artist = getGeneral(tag,FieldKey.ALBUM_ARTIST);
        composer = getGeneral(tag,FieldKey.COMPOSER);
        
        track = getGeneral(tag,FieldKey.TRACK);
        tracks_total = getGeneral(tag,FieldKey.TRACK_TOTAL);
        disc = getGeneral(tag,FieldKey.DISC_NO);
        discs_total = getGeneral(tag,FieldKey.DISC_TOTAL);
        genre = getGeneral(tag,FieldKey.GENRE);
        year = getGeneral(tag,FieldKey.YEAR);
        
        category = getGeneral(tag,FieldKey.GROUPING);
        cover = tag.getFirstArtwork();
        comment = getComment(tag);
        lyrics = getGeneral(tag,FieldKey.LYRICS);
        mood = getGeneral(tag,FieldKey.MOOD);
        custom1 = getGeneral(tag,FieldKey.CUSTOM1);
        custom2 = getGeneral(tag,FieldKey.CUSTOM2);
        custom3 = getGeneral(tag,FieldKey.CUSTOM3);
        custom4 = getGeneral(tag,FieldKey.CUSTOM4);
        custom5 = getGeneral(tag,FieldKey.CUSTOM5);
    }
    private String getGeneral(Tag tag, FieldKey f) {
        if (!tag.hasField(f)) return "";
        return Util.emptifyString(tag.getFirst(f));
    }
    // use this to get comment, not getField(COMMENT); because it is bugged
    private String getComment(Tag tag) { 
        // there is a bug where getField(Comment) returns CUSTOM1 field, this is workaround
        // this is how COMMENT field look like:
        //      Language="English"; Text="example"; 
        // this is how CuSTOM fields look like:  
        //      Language="Media Monkey Format"; Description="Songs-DB_Custom5"; Text="example"; 
        if (!tag.hasField(FieldKey.COMMENT)) return "";
        int i = -1;
        List<TagField> fields = tag.getFields(FieldKey.COMMENT);
        // get index of comment within all comment-type tags
        for(TagField t: fields) // custom
            if (!t.toString().contains("Description"))
                i = fields.indexOf(t);
        if(i>-1) return tag.getValue(FieldKey.COMMENT, i);
        else return "";
    }
    
    private List<String> getGenerals(Tag tag, FieldKey f) {
        List<String> out = new ArrayList<>(); 
        if (!tag.hasField(f)) return out;
        
        try{
//        for (TagField field: tag.getFields(f)) {
//            String tmp = field.toString();
//                   tmp = tmp.substring(6, tmp.length()-3);
//            out.add(Util.emptifyString(tmp));
//        }
        for (String val: tag.getAll(f))
            out.add(Util.emptifyString(val));
        
        
        }catch(Exception w) { 
            // contrary to what compiler is sayingL no exception is not too broad
            // do not change the exception or some weird stuff will be happening
            // jaudiotagger throws some additional exceptions here and there...
            w.printStackTrace();
        }
        return out;
    }
    
    private void loadSpecificFieldsMP3(MP3File mp3) {
        AbstractID3v2Frame frame1 = mp3.getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
        FrameBodyPOPM body1 = null;
        Long rat = null;
        Long cou = null;
        if (frame1 != null) {
            body1 = (FrameBodyPOPM) frame1.getBody();
        }
        if (body1 != null) {
            rat = body1.getRating();//returns null if empty
            cou = body1.getCounter();//returns null if empty
        }
        
        rating = rat==null ? -1 : Math.toIntExact(rat);
        playcount = cou==null ? -1 : Math.toIntExact(cou);
        publisher = Util.emptifyString(mp3.getID3v2TagAsv24().getFirst(ID3v24Frames.FRAME_ID_PUBLISHER));
    }
    private void loadSpecificFieldsWAV() {
        rating = -1;
        playcount = -1;
        publisher = "";
    }
    private void loadSpecificFieldsOGG() {
        rating = -1;
        playcount = -1;
        publisher = "";
    }
    private void loadSpecificFieldsFLAC() {
        rating = -1;
        playcount = -1;
        publisher = "";
    }

/******************************************************************************/  

    public String getId() {
       return uri;
    }
    public void setId(String id) {
        this.uri = id;
    }
    
    @Override 
    public URI getURI() {
        return URI.create(uri.replace(" ","%20"));
    }
    
    /**
     * @see #EMPTY
     * @return true if this metadata is empty.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }
    
    // because of the above we need to override the following two methods:
    
    /** @{@inheritDoc} */
    @Override
    @MetadataFieldMethod(Field.PATH)
    public String getPath() {
        if (isEmpty()) return "";
        else return super.getPath();
    }
    
    /** @{@inheritDoc} */
    @MetadataFieldMethod(Field.FILESIZE)
    @Override
    public FileSize getFilesize() {
        return new FileSize(filesize);
    }    
    
    // not that we override this method. We read file type (format) from metadata
    // and (i believe) it is more safe than other approaches, so rather than
    // using super implementation, we will check tag and fall back to super if
    // needed (see constructor)
    /**  {@inheritDoc } */
    @Override
    @MetadataFieldMethod(Field.FORMAT)
    public AudioFileFormat getFormat() {
        return format;
    }

    /** @return the bitrate */
    @MetadataFieldMethod(Field.BITRATE)
    public Bitrate getBitrate() {
        return new Bitrate(bitrate);
    }
    
    /**
     * For example: Stereo.
     * @return channels as String
     */
    @MetadataFieldMethod(Field.CHANNELS)
    public String getChannels() {
        return channels;
    }
    
    /**
     * For example: 44100
     * @return 
     */
    @MetadataFieldMethod(Field.SAMPLE_RATE)
    public String getSampleRate() {
        return sample_rate;
    }
    
    /**
     * For example: MPEG-1 Layer 3.
     * @return encoding type
     */
    @MetadataFieldMethod(Field.ENCODING)
    public String getEncodingType() {
        // format and encoding type are switched in jaudiotagger library...
        return encoding_type;
    }
    
    /** @return the encoder or empty String if not available. */
    @MetadataFieldMethod(Field.ENCODER)
    public String getEncoder() {
        return encoder;
    }
    
    /** @return the length in milliseconds */
    @MetadataFieldMethod(Field.LENGTH)
    public FormattedDuration getLength() {
        return new FormattedDuration(duration);
    }
    
/******************************************************************************/    
    
    /** @return the title "" if empty. */
    @MetadataFieldMethod(Field.TITLE)
    public String getTitle() {
        return title;
    }
    
    /** @return the album "" if empty. */
    @MetadataFieldMethod(Field.ALBUM)
    public String getAlbum() {
        return album;
    }
    
    /**
     * Returns list all of artists.
     * @return the artist
     * empty List if empty.
     */
    public List<String> getArtists() {
        return artists;
    }
    
    /**
     * Returns all of artists as a String. Uses ", " separator. One line
     * Empty string no arist.
     * @return 
     */
    public String getArtistsAsString() {
        String temp = "";
        for (String s: artists) {
            temp = temp + ", " + s;
        }
        return temp;
    }
    
    /**
     * Returns first artist found in tag.
     * If you want to get all artists dont use this method.
     * @return the first artist
     * "" if empty.
     */
    @MetadataFieldMethod(Field.ARTIST)
    public String getArtist() {
        return Configuration.ALBUM_ARTIST_WHEN_NO_ARTIST && artist.isEmpty() ? album_artist : artist;
    }
    
    /** @return the album_artist, "" if empty. */
    @MetadataFieldMethod(Field.ALBUM_ARTIST)
    public String getAlbumArtist() {
        return album_artist;
    }

    /** @return the composer, "" if empty. */
    @MetadataFieldMethod(Field.COMPOSER)
    public String getComposer() {
        return composer;
    }

    /** @return the publisher, "" if empty. */
    @MetadataFieldMethod(Field.PUBLISHER)
    public String getPublisher() {
        return publisher;
    }

    /** @return the track, "" if empty. */
    @MetadataFieldMethod(Field.TRACK)
    public String getTrack() {
        return track;
    }
    
    /** @return the tracks total, "" if empty. */
    @MetadataFieldMethod(Field.TRACKS_TOTAL)
    public String getTracksTotal() {
        return tracks_total;
    }
    
    /**
     * Convenience method. Returns complete info about track order within album in
     * format track/tracks_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getTrack()} and {@link #getTracksTotal())}.
     * <p>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, 4/23, ?/?, ...
     * @return track album order information.
     */
    @MetadataFieldMethod(Field.TRACK_INFO)
    public String getTrackInfo() {
        if (!track.isEmpty() && !tracks_total.isEmpty()) {
            return track+"/"+tracks_total;
        } else
        if (!track.isEmpty() && tracks_total.isEmpty()) {
            return track+"/?";
        } else        
        if (track.isEmpty() && !tracks_total.isEmpty()) {
            return "?/"+tracks_total;
        } else {
        //if (track.isEmpty() && tracks_total.isEmpty()) {
            return "?/?";
        }
    }
    
    /** @return the disc, "" if empty. */
    @MetadataFieldMethod(Field.DISC)
    public String getDisc() {
        return disc;
    }
    
    /** @return the discs total, "" if empty. */
    @MetadataFieldMethod(Field.DISCS_TOTAL)
    public String getDiscsTotal() {
        return discs_total;
    }
    
    /**
     * Convenience method. Returns complete info about disc number in album in
     * format disc/discs_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getDisc() ()} and {@link #getDiscsTotal()}.
     * <p>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, ?/?, 5/?, ...
     * @return disc information.
     */
    @MetadataFieldMethod(Field.DISCS_INFO)
    public String getDiscInfo() {
        if (!disc.isEmpty() && !discs_total.isEmpty()) {
            return disc+"/"+discs_total;
        } else
        if (!disc.isEmpty() && discs_total.isEmpty()) {
            return disc+"/?";
        } else        
        if (disc.isEmpty() && !discs_total.isEmpty()) {
            return "?/"+discs_total;
        } else {
        //if (disc.isEmpty() && discs_total.isEmpty()) {
            return "?/?";
        }
    }
    
    /** @return the genre, "" if empty. */
    @MetadataFieldMethod(Field.GENRE)
    public String getGenre() {
        return genre;
    }

    /** @return the year, "" if empty. */
    @MetadataFieldMethod(Field.YEAR)
    public String getYear() {
        return year;
    }
    
    /**
     * Returns cover from the respective source.
     * @param source
     */
    public Cover getCover(CoverSource source) {
        switch(source) {
            case TAG: return getCover();
            case DIRECTORY: return new FileCover(getCoverFromDirAsFile(), "");
            case ANY : {
                Cover c = getCover();
                return c.getImage()!=null 
                        ? c 
                        : new FileCover(getCoverFromDirAsFile(), "");
            }
            default: throw new AssertionError(source + " in default switch value.");
        }
    }
    
    @MetadataFieldMethod(Field.COVER)
    private Cover getCover() {
        try {
            if(cover==null) return new ImageCover((Image)null, getCoverInfo());
            else return new ImageCover((BufferedImage) cover.getImage(), getCoverInfo());
        } catch (IOException ex) {
            return new ImageCover((Image)null, getCoverInfo());
        }
    }
    
    @MetadataFieldMethod(Field.COVER_INFO)
    private String getCoverInfo() {
        try {
            return cover.getDescription() + " " + cover.getMimeType() + " "
                       + ((RenderedImage)cover.getImage()).getWidth() + "x"
                       + ((RenderedImage)cover.getImage()).getHeight();
        } catch(IOException | NullPointerException e) {
            // nevermind errors. Return "" on fail.
            return "";
        }
    }
    
    /**
     * Identical to getCoverFromDir() method, but returns the image file
     * itself. Only file based items.
     * <p>
     * If the Image object of the cover suffices, it is recommended to
     * avoid this method, or even better use getCoverFromAnySource()
     * @return 
     */
    @TODO(value = "ensure null doesnt cause any problems")
    private File getCoverFromDirAsFile() {
        if(!isFileBased()) return null;
        
        File dir = getFile().getParentFile();
        if (!FileUtil.isValidDirectory(dir)) return null;
                
        File[] files;
        files = dir.listFiles( f -> {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if(i == -1) return false; 
            String name = filename.substring(0, i);
            return (ImageFileFormat.isSupported(f.toURI()) && (
                        name.equalsIgnoreCase("cover") ||
                        name.equalsIgnoreCase("folder") ||
                        name.equalsIgnoreCase(getFilenameFull()) ||
                        name.equalsIgnoreCase(getFilename()) ||
                        name.equalsIgnoreCase(getTitle()) ||
                        name.equalsIgnoreCase(getAlbum())
                ));
        });
        
        if (files.length == 0) return null;
        else return files[0];
    }
    
    /** @return the rating or 0 if empty. */
    public int getRating() {
        return rating==-1 ? 0 : rating;
    }
    
    /** 
     * Recommended method to use to obtain rating.
     * @return the rating in 0-1 percent range */
    @MetadataFieldMethod(Field.RATING)
    public double getRatingPercent() {
        return getRating()/getRatingMax();
    }
    
    /** @return the rating value (can for file type) or "" if empty. */
    public String getRatingAsString() {
        if (rating == -1)  return ""; 
        return String.valueOf(getRating());
    }
    
    /** @return the rating in 0-1 percent range or "" if empty. */
    public String getRatingPercentAsString() {
        if (rating == -1)  return ""; 
        return String.valueOf(getRatingPercent());
    }
    
    /**
     * @param max_stars
     * @return the current rating value in 0-max_stars value system. 0 if not available.
     */
    public double getRatingToStars(int max_stars) {
        return getRatingPercent()*max_stars;
    }
    
    /** @return the playcount, 0 if empty. */
    @MetadataFieldMethod(Field.PLAYCOUNT)
    public int getPlaycount() {
        return playcount == -1 ? 0 : playcount;
    }
    
    /**
     * @return the playcount
     * "" if empty.
     */
    public String getPlaycountAsString() {
        return (playcount == -1) ? "" : String.valueOf(playcount);
    }
    
    /** 
     * tag field: grouping
     * @return the category "" if empty.
     */
    @MetadataFieldMethod(Field.CATEGORY)
    public String getCategory() {
        return category;
    }
    
    /** @return the comment, "" if empty. */
    @MetadataFieldMethod(Field.COMMENT)
    public String getComment() {
        return comment;
    }

    /**
     * @return the lyrics, "" if empty.
     */
    @MetadataFieldMethod(Field.LYRICS)
    public String getLyrics() {
        return lyrics;
    }

    /**  @return the mood, "" if empty. */
    @MetadataFieldMethod(Field.MOOD)
    public String getMood() {
        return mood;
    }
    
    /**  
     * Color is located in the Custom1 tag field.
     * @return the color value associated with the song from tag or {@link EMPTY_COLOR} if
     * none.
     */
    @MetadataFieldMethod(Field.COLOR)
    public Color getColor() {
         return custom1.isEmpty() ? EMPTY_COLOR : new ColorParser().fromS(custom1);
    }
    
    /** @return the value of custom1 field. "" if empty. */
    @MetadataFieldMethod(Field.CUSTOM1)
    public String getCustom1() {
        return custom1;
    }
    /** @return the value of custom2 field. "" if empty. */
    @MetadataFieldMethod(Field.CUSTOM2)
    public String getCustom2() {
        return custom2;
    }
    /** @return the value of custom3 field. "" if empty. */
    @MetadataFieldMethod(Field.CUSTOM3)
    public String getCustom3() {
        return custom3;
    }
    /** @return the value of custom4 field. "" if empty. */
    @MetadataFieldMethod(Field.CUSTOM4)
    public String getCustom4() {
        return custom4;
    }
    /** @return the value of custom5 field. "" if empty. */
    @MetadataFieldMethod(Field.CUSTOM5)
    public String getCustom5() {
        return custom5;
    }
    
/******************************************************************************/
    
    /**
     * Note: This is non-tag information.
     * @return some additional non-tag information
     */
    public MetadataExtended getExtended() {
        MetadataExtended me = new MetadataExtended(this);
                         me.readFromFile();
        return me;
    }
    
    /**
     * @return index of the item in playlist belonging to this metadata or -1 if
     * not on playlist.
     */
    public int getPlaylistIndex() {
        return PlaylistManager.getIndexOf(this)+1;
    }
    
    /**
     * Returns index of the item in playlist belonging to this metadata.
     * Convenience method. Example: "15/30". "Order/total" items in playlist.
     * Note: This is non-tag information.
     * @return Empty string if not on playlist.
     */
    public String getPlaylistIndexInfo() {
        int i = getPlaylistIndex();
        return i==-1 ? "" : i + "/" + PlaylistManager.getSize();
    }
    
    /**
     * Returns chapters associated with this item. A {@link Chapter} is a textual
     * information associated with time and the song item. There are usually
     * many chapters per item.
     * <p>
     * Chapters are concatenated into string located in the Custom2 tag field.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from tag data
     */
    public List<Chapter> getChapters() {
        String chapterString = getCustom2();
        if(chapterString.isEmpty()) return EMPTY_LIST;
        
        List<Chapter> cs = new ArrayList();
        // we have got a regex over here so "||\" is equivalent to '\' character
        for(String c: getCustom2().split("\\|", 0)) {
            try {
                cs.add(new Chapter(c));
            } catch( IllegalArgumentException e) {
                Log.err("String '" + c + "' not be parsed as chapter. Will be ignored.");
                // ignore
            }
        }
        return cs;
    }
    
    /**
     * Legacy method.
     * Before chapters were written to tag they were contained within xml files
     * in the same directory as the item and the same file name (but xml suffix).
     * This method reads the file when it is invoked and parses its content.
     * <p>
     * This method should only be used when transferring information in legacy
     * xml files into tag.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from xml data
     */
    public List<Chapter> getChaptersFromXML() {
        MetadataExtended me = new MetadataExtended(this);
                         me.readFromFile();
        List<Chapter> cs = me.getChapters();
                      cs.sort(Chapter::compareTo);
        return cs;
    }
    
    /**
     * Convenience method combining the results of {@link #getChapters()} and 
     * {@link #getChaptersFromXML()}.
     * <p>
     * This method should only be used when transferring information in legacy
     * xml files into tag.
     * <p>
     * The result is ordered by natural order.
     * @return ordered list of chapters parsed from all available sources
     */
    @MetadataFieldMethod(Field.CHAPTERS)
    public List<Chapter> getChaptersFromAny() {
        List<Chapter> cs = getChapters();
                      cs.addAll(getChaptersFromXML());
                      cs.sort(Chapter::compareTo);
        return cs;
    }
    
    public boolean containsChapterAt(Duration at) {
        return getChapters().stream().anyMatch(ch->ch.getTime().equals(at));
    }
    
/******************************************************************************/
    
    public Object getField(Field fieldType) {
        // reflection implementation that works, but for performance reasons was
        // scrapped (table cell rendering with this is not a good idea)
        // it could be improved from O(n) to O(1) though if there was name contract
        // between the Field name and its getter - might try that
//        for(Method m : Metadata.class.getDeclaredMethods()) {
//            MetadataFieldMethod a = m.getAnnotation(MetadataFieldMethod.class);
//            if(a!=null && a.value()==fieldType) {
//                try {
//                    return m.invoke(this);
//                } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
//                    throw new RuntimeException("Method for metadata field " + fieldType + " invocation failed.");
//                }
//            }
//        }
//        throw new RuntimeException("Method for metadata field " + fieldType + " not found.");
        
        switch(fieldType) {
            case PATH :  return getPath();
            case FORMAT :  return getFormat();
            case FILESIZE : return getFilesize();
            case ENCODING : return getEncodingType();
            case BITRATE : return getBitrate();
            case ENCODER : return getEncoder();
            case CHANNELS : return getChannels();
            case SAMPLE_RATE : return getSampleRate();
            case LENGTH : return getLength();
            case TITLE : return getTitle();
            case ALBUM : return getAlbum();
            case ARTIST : return getArtist();
            case ALBUM_ARTIST : return getAlbumArtist();
            case COMPOSER : return getComposer();
            case PUBLISHER : return getPublisher();
            case TRACK : return getTrack();
            case TRACKS_TOTAL : return getTracksTotal();
            case TRACK_INFO : return getTrackInfo();
            case DISC : return getDisc();
            case DISCS_TOTAL : return getDiscsTotal();
            case DISCS_INFO : return getDiscInfo();
            case GENRE : return getGenre();
            case YEAR : return getYear();
            case COVER : return getCover();
            case COVER_INFO : return getCoverInfo();
            case RATING : return getRatingPercent();
            case PLAYCOUNT : return getPlaycount();
            case CATEGORY : return getCategory();
            case COMMENT : return getComment();
            case LYRICS : return getLyrics();
            case MOOD : return getMood();
            case COLOR : return getColor();
            case CHAPTERS : return getChapters();
            case CUSTOM1 : return getCustom1();
            case CUSTOM2 : return getCustom2();
            case CUSTOM3 : return getCustom3();
            case CUSTOM4 : return getCustom4();
            case CUSTOM5 : return getCustom5();
            default : throw new RuntimeException("ddd");
        }
    }
    
    /**
     * Returns Field-Value map representation of this object.
     * <p>
     * Note: This method can be used on {@link #EMPTY} to inspect the class
     * type of the fields without actually using any Metadata object.
     * 
     * @return 
     */
    public Map<Field,Object> getFields() {
        Map<Field,Object> m = new HashMap();
        for(Field f : Field.values()) {
            m.put(f, getField(f));
        }
        return m;
    }
    
/******************************************************************************/
    
    /** 
     * Non-tag data and chapters are excluded.
     * @return mostly complete information about item's metadata in string form. 
     */
    @Override
    public String toString() {
        String output;
        
        output = "path:  " + getPath() + "\n"
               + "name: " + getFilename() + "\n"
               + "format: " + getFormat() + "\n"
               + "filesize: " + getFilesize().toString() + "\n"

               + "bitrate: " + bitrate + "\n"
               + "encoder: " + encoder + "\n"
               + "length: " + getLength().toString() + "\n"

               + "title: " + title + "\n"
               + "album: " + album + "\n"
               + "artists: " + artists.size() + "\n"
               +  artists.stream().map(a->"    " + a + "\n").collect(Collectors.joining(""))
               + "album artist: " + album_artist + "\n"
               + "composer: " + composer + "\n"
               + "publisher: " + publisher + "\n"

               + "track: " + getTrackInfo() + "\n"
               + "disc: " + getDiscInfo() + "\n"
               + "genre: " + genre + "\n"
               + "year: " + year + "\n"

               + "cover: " + getCoverInfo() + "\n"

               + "rating: " + getRatingAsString() + "\n"
               + "playcount: " + getPlaycountAsString() + "\n"
               + "category: " + category + "\n"
               + "comment: " + comment + "\n"
               + "lyrics: " + lyrics + "\n"
               + "mood: " + mood + "\n"
               + "color: " + getColor() + "\n"
               + "chapters: " + mood + "\n"
               + getChaptersFromAny().stream().map(a->"    " + a + "\n").collect(Collectors.joining(""))
               + "custom1: " + custom1 + "\n"
               + "custom2: " + custom2 + "\n"
               + "custom3: " + custom3 + "\n"
               + "custom4: " + custom4 + "\n"
               + "custom5: " + custom5 + "\n";
        
        return output;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (uri != null ? uri.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        
        if (!(o instanceof Metadata))
            return false;
          
        Metadata m = (Metadata) o;
        return uri != null && m.uri != null && uri.equals(m.uri);
    }
    
    public static enum Field {
        PATH,
        FORMAT,
        FILESIZE,
        ENCODING,
        BITRATE,
        ENCODER,
        CHANNELS,
        SAMPLE_RATE,
        LENGTH,
        TITLE,
        ALBUM,
        ARTIST,
        ALBUM_ARTIST,
        COMPOSER,
        PUBLISHER,
        TRACK,
        TRACKS_TOTAL,
        TRACK_INFO,
        DISC,
        DISCS_TOTAL,
        DISCS_INFO,
        GENRE,
        YEAR,
        COVER,
        COVER_INFO,
        RATING,
        PLAYCOUNT,
        CATEGORY,
        COMMENT,
        LYRICS,
        MOOD,
        COLOR,
        CHAPTERS,
        CUSTOM1,
        CUSTOM2,
        CUSTOM3,
        CUSTOM4,
        CUSTOM5;
        
        public boolean isStringRepresentable() {
            return this != COVER;
        }
        
        public String toCapitalizedS() {
            return Util.capitalizeStrong(name());
        }
        
        public Class getType() {
            return EMPTY.getField(this).getClass();
        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD) 
    public static @interface MetadataFieldMethod {
        Field value();
    }
}