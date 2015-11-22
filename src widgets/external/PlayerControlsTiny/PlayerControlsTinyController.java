package PlayerControlsTiny;


import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Layout.widget.Widget;
import Layout.widget.controller.FXMLController;
import Layout.widget.feature.PlaybackFeature;
import gui.GUI;
import gui.objects.Seeker;
import gui.objects.icon.Icon;
import util.Util;
import util.access.V;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.UNKNOWN;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;

/** FXMLController for widget. */
@Widget.Info(
    name = "Playback Mini",
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    howto = "Playback actions:\n"
          + "    Control Playback\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "    Left click : Seek - move playback to seeked position\n"
          + "    Mouse drag : Seek (on release)\n"
          + "    Right click : Cancel seek\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "\nChapter actions:\n"
          + "    Right click : Create chapter\n"
          + "    Right click chapter : Open chapter\n"
          + "    Mouse hover chapter (optional) : Open chapter\n",
    description = "Minimalistic playback control widget.",
    notes = "",
    version = "1",
    year = "2015",
    group = Widget.Group.PLAYBACK
)
public class PlayerControlsTinyController extends FXMLController implements PlaybackFeature {

    @FXML AnchorPane root;
    @FXML HBox layout, controlBox, volBox;
    @FXML Slider volume;
    @FXML Label currTime, titleL, artistL;
    private Seeker seeker = new Seeker();
    private Icon prevB, playB, stopB, nextB, volB;

    @IsConfig(name = "Show chapters", info = "Display chapter marks on seeker.")
    public final V<Boolean> showChapters = new V<>(true, seeker::setChaptersVisible);
    @IsConfig(name = "Open chapters", info = "Display pop up information for chapter marks on seeker.")
    public final V<Boolean> popupChapters = new V<>(true, seeker::setChaptersShowPopUp);
    @IsConfig(name = "Snap seeker to chapters on drag", info = "Enable snapping to chapters during dragging.")
    public final V<Boolean> snapToChap = new V<>(true, seeker::setSnapToChapters);
    @IsConfig(name = "Open max 1 chapter", info = "Allows only one chapter open. Opening chapter closes all open chapters.")
    public final V<Boolean> singleChapMode = new V<>(true, seeker::setSinglePopupMode);
    @IsConfig(name = "Open chapter on hover", info = "Opens chapter also when mouse hovers over them.")
    public final V<Boolean> showChapOnHover = seeker.selectChapOnHover;
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    public boolean elapsedTime = true;
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;


    @Override
    public void init() {
        PlaybackState ps = PLAYBACK.state;

        // make volume
        volume.setMin(ps.volume.getMin());
        volume.setMax(ps.volume.getMax());
        volume.setValue(ps.volume.get());
        volume.valueProperty().bindBidirectional(ps.volume);
        d(volume.valueProperty()::unbind);

        // make seeker
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        d(seeker::dispose);
        d(maintain(GUI.snapDistance, d->d, seeker.chapSnapDist));
        layout.getChildren().add(2,seeker);
        HBox.setHgrow(seeker, ALWAYS);

        // make icons
        prevB = new Icon(STEP_BACKWARD, 14, null, PlaylistManager::playPreviousItem);
        playB = new Icon(null, 14, null, PLAYBACK::pause_resume);
        stopB = new Icon(STOP, 14, null, PLAYBACK::stop);
        nextB = new Icon(STEP_FORWARD, 14, null, PlaylistManager::playNextItem);
        controlBox.getChildren().addAll(prevB,playB,stopB,nextB);
        volB = new Icon(null, 14, null, PLAYBACK::toggleMute);
        volBox.getChildren().add(0,volB);

        // monitor properties and update graphics + initialize
        d(maintain(ps.volume, v -> muteChanged(ps.mute.get(), v.doubleValue())));
        d(maintain(ps.mute, m -> muteChanged(m, ps.volume.get())));
        d(maintain(ps.status, this::statusChanged));
        d(maintain(ps.currentTime,t->currentTimeChanged()));
        d(Player.playingtem.onUpdate(this::playbackItemChanged));

        // drag & drop
        installDrag(
            root, PLAYLIST_PLUS, "Add to active playlist",
            DragUtil::hasAudio,
            e -> {
                List<Item> items = DragUtil.getAudioItems(e);
                if(playDropped) {
                    PlaylistManager.use(p -> p.setNplay(items));
                } else {
                    PlaylistManager.use(p -> p.addItems(items));
                }
            }
        );
    }

    @Override
    public void refresh() { }

/******************************************************************************/

    @FXML private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        currentTimeChanged();
    }

    private void playbackItemChanged(Metadata m) {
        titleL.setText(m.getTitle());
        artistL.setText(m.getArtist());
        seeker.reloadChapters(m);
    }

    private void statusChanged(Status status) {
        if (status == null || status == UNKNOWN ) {
            seeker.setDisable(true);
            playB.icon(PLAY);
        } else if (status == PLAYING) {
            seeker.setDisable(false);
            playB.icon(PAUSE);
        } else {
            seeker.setDisable(false);
            playB.icon(PLAY);
        }
    }

    private void muteChanged(boolean mute, double vol) {
        volB.icon(mute ? VOLUME_OFF : vol>.5 ? VOLUME_UP : VOLUME_DOWN);
    }

    private void currentTimeChanged() {
        // update label
        if (elapsedTime) {
            Duration elapsed = PLAYBACK.getCurrentTime();
            currTime.setText(Util.formatDuration(elapsed));
        } else {
            if (PLAYBACK.getTotalTime() == null) return;
            Duration remaining = PLAYBACK.getRemainingTime();
            currTime.setText("- " + Util.formatDuration(remaining));
        }
    }
}