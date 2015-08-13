 
package ImageViewer;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.IsInput;
import Layout.Widgets.feature.ImageDisplayFeature;
import Layout.Widgets.feature.ImagesDisplayFeature;
import gui.InfoNode.ItemInfo;
import gui.objects.icon.Icon;
import gui.objects.image.Thumbnail;
import main.App;
import util.Util;
import util.access.Accessor;
import util.animation.Anim;
import util.async.executor.FxTimer;
import util.async.runnable.Run;
import util.graphics.drag.DragUtil;

import static Layout.Widgets.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_RIGHT;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.application.Platform.runLater;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static util.File.FileUtil.getFilesImage;
import static util.Util.setAnchors;
import static util.async.Async.FX;
import static util.async.future.Fut.fut;
import static util.functional.Util.forEachWithI;

/**
 * 
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Image Viewer",
    description = "Displays images in directory. Shows main image and thumbnails. "
        + "Looks for images in subfolders.",
    howto = ""
        + "    The widget displays an image and image thumbnails for images in "
        + "specific directory - data source. Main image can change automatically "
        + " (slideshow) or manually by clicking on the thumbnail, or navigating "
        + "to next/previous image.\n"
        + "    User can display image or images in a location by setting the "
        + "file or directory, e.g., by drag & drop. The widget can also follow "
        + "playing or selected songs, displaying images in their parent "
        + "directory.\n"
        + "    The image search is recursive and search depth configurable.\n"
        + "\n"
        + "Available actions:\n"
        + "    Left click: Shows/hides thumbnails\n"
        + "    Left click bottom : Toggles info pane\n"
        + "    Nav icon click : Previous/Next image\n"
        + "    Info pane right click : Shows/hides bacground for info pane\n"
        + "    Image right click : Opens image context menu\n"
        + "    Thumbnail left click : Set as image\n"
        + "    Thumbnail right click : Opens thumbnail context menu\n"
        + "    Drag&Drop audio : Displays images for the first dropped item\n"
        + "    Drag&Drop image : Show images\n",
    notes = "",
    version = "1.0",
    year = "2015",
    group = OTHER
)
public class ImageViewerController extends FXMLController implements ImageDisplayFeature, ImagesDisplayFeature {
    
    // gui
    @FXML private AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;
    private final Thumbnail mainImage = new Thumbnail();
    private Anim thumbAnim;
    private ItemInfo itemPane;
    
    // state
    private final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
    private final ObservableList<File> images = FXCollections.observableArrayList();
    private final List<Thumbnail> thumbnails = new ArrayList();
    // eager initialized state
    private FxTimer slideshow = new FxTimer(Duration.ZERO,INDEFINITE,this::nextImage);
    private Metadata data = Metadata.EMPTY;
    
    // cnfigurable
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public final Accessor<Double> thumbSize = new Accessor<>(70d, v -> thumbnails.forEach(t->t.getPane().setPrefSize(v,v)));
    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
    public final Accessor<Double> thumbGap = new Accessor<>(2d, v -> {
        thumb_pane.setHgap(v);
        thumb_pane.setVgap(v);
    });
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public final Accessor<Duration> slideshow_dur = new Accessor<>(seconds(15), slideshow::setTimeoutAndRestart);
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public final Accessor<Boolean> slideshow_on = new Accessor<>(true, v -> {
        if (v) slideshow.start(); else slideshow.stop();
    });
    @IsConfig(name = "Show big image", info = "Show thumbnails.")
    public final Accessor<Boolean> showImage = new Accessor<>(true, mainImage.getPane()::setVisible);
    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
    public final Accessor<Boolean> showThumbnails = new Accessor<>(true, this::thumbAnimPlay);
    @IsConfig(name = "Hide thumbnails on mouse exit", info = "Hide thumbnails when mouse leaves the widget area.")
    public final Accessor<Boolean> hideThumbEager = new Accessor<>(true, v -> {
        if (v) entireArea.setOnMouseExited(e -> {
                   // ensure that mouse really exited, as mouse exit event also
                   // happens when mouse enters context menu or app ui drag sets
                   // ui to mouse transparent
                   if(!entireArea.contains(e.getX(), e.getY()))
                       showThumbnails.setNapplyValue(false);
               });
        else entireArea.setOnMouseExited(null);
    });
    @IsConfig(name = "Show thumbnails on mouse enter", info = "Show thumbnails when mouse enters the widget area.")
    public final Accessor<Boolean> showThumbEager = new Accessor<>(false, v -> {
        if (v) entireArea.setOnMouseEntered(e -> showThumbnails.setNapplyValue(true));
        else entireArea.setOnMouseEntered(null);
    });
    @IsConfig(name = "Show thumbnails rectangular", info = "Always frame thumbnails into squares.")
    public final Accessor<Boolean> thums_rect = new Accessor<>(false, v -> thumbnails.forEach(t -> {
        t.setBorderToImage(!v);
        t.setBackgroundVisible(v);
    }));
    @IsConfig(name = "Alignment", info = "Preferred image alignment.")
    public final Accessor<Pos> align = new Accessor<>(CENTER, mainImage::applyAlignment);
    @IsConfig(name = "Theater mode", info = "Turns off slideshow, shows image background to fill the screen, disables image border and displays information about the song.")
    public final Accessor<Boolean> theater_mode = new Accessor<>(false, this::applyTheaterMode);
    
    @IsConfig(name = "Forbid no content", info = "Ignores empty directories and doesnt change displayed images if there is nothing to show.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "File search depth", info = "Depth to searcho for files in folders. 1 for current folder only.")
    public int folderTreeDepth = 2;
    @IsConfig(name = "Max amount of thubmnails", info = "Important for directories with lots of images.")
    public int thumbsLimit = 50;
    @IsConfig(name = "Displayed image", editable = false)
    private int active_image = -1;
    
    
    
    /** {@inheritDoc} */
    @Override
    public void init() {
        inputs.getInput("Location of").bind(Player.playing.o);
        
        loadSkin("skin.css",entireArea);
        
        // main image
        mainImage.setBorderVisible(true);
        mainImage.setBorderToImage(true);
        entireArea.getChildren().add(mainImage.getPane());
        setAnchors(mainImage.getPane(),0);
        
        // thumb anim
        thumbAnim = new Anim(millis(500), thumb_root::setOpacity);
        thumb_root.visibleProperty().bind(thumb_root.opacityProperty().isNotEqualTo(0));
        thumb_root.toFront();
        
        // image navigation
        Icon nextB = new Icon(ARROW_RIGHT, 18, "Next image");
        Pane nextP = new StackPane(nextB);
             nextP.setOnMouseClicked(Run.of(this::nextImage).toHandlerConsumed());
             nextP.getStyleClass().add("nav-pane");
             nextP.prefWidthProperty().bind(entireArea.widthProperty().divide(10));
             nextP.setMinWidth(20);
             nextP.opacityProperty().bind(Bindings.subtract(1, thumb_root.opacityProperty()));
             nextP.visibleProperty().bind(nextP.opacityProperty().isNotEqualTo(0));
             nextB.visibleProperty().bind(nextP.hoverProperty());
        Icon prevB = new Icon(ARROW_LEFT, 18, "Previous image");
        Pane prevP = new StackPane(prevB);
             prevP.setOnMouseClicked(Run.of(this::prevImage).toHandlerConsumed());
             prevP.getStyleClass().add("nav-pane");
             prevP.prefWidthProperty().bind(entireArea.widthProperty().divide(10));
             prevP.setMinWidth(20);
             prevP.opacityProperty().bind(Bindings.subtract(1, thumb_root.opacityProperty()));
             prevP.visibleProperty().bind(prevP.opacityProperty().isNotEqualTo(0));
             prevB.visibleProperty().bind(prevP.hoverProperty());
        entireArea.getChildren().addAll(prevP,nextP);
        AnchorPane.setBottomAnchor(nextP, 0d);
        AnchorPane.setTopAnchor(nextP, 0d);
        AnchorPane.setRightAnchor(nextP, 0d);
        AnchorPane.setBottomAnchor(prevP, 0d);
        AnchorPane.setTopAnchor(prevP, 0d);
        AnchorPane.setLeftAnchor(prevP, 0d);
        
        // thumbnails & make sure it doesnt cover whole area
        Util.setAnchors(thumb_root, 0);
        entireArea.heightProperty().addListener((o,ov,nv) -> AnchorPane.setBottomAnchor(thumb_root, nv.doubleValue()*0.3));
        
        entireArea.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getY()>0.8*entireArea.getHeight() && e.getX()>0.7*entireArea.getWidth()) {
                    theater_mode.setCycledNapplyValue();
                } else {
                    showThumbnails.setCycledNapplyValue();
                }
                e.consume();
            }
        });
        // prevent scrollpane from preventing show thumbnails change
        thumb_root.setOnMouseClicked(e -> {
            if (e.getButton()==PRIMARY) {
                showThumbnails.setCycledNapplyValue();
                e.consume();
            }
        });
        
        // refresh if source data changed
        ChangeListener<File> locationChange = (o,ov,nv) -> readThumbnails();
        folder.addListener(locationChange);
        d(() -> folder.removeListener(locationChange));
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        entireArea.setOnDragOver(DragUtil.fileDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped(e -> {
            if(e.getDragboard().hasFiles()) {
                dataChanged(e.getDragboard().getFiles().get(0));
                e.setDropCompleted(true);
                e.consume();
                return;
            }
            if(e.getGestureSource().equals(mainImage.getPane())) return;
            if(DragUtil.hasAudio(e.getDragboard())) {
                // get first item
                List<Item> items = DragUtil.getAudioItems(e);
                if (!items.isEmpty()) dataChanged(items.get(0));
                // end drag
                e.setDropCompleted(true);
                e.consume();
            } else 
            if(DragUtil.hasImage(e.getDragboard())) {
                fut().supply(DragUtil.getImages(e))
                     .use(this::showImages,FX)
                     .showProgress(App.getWindow().taskAdd())
                     .run();
                
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        // consume scroll event to prevent app scroll behavior // optional
        entireArea.setOnScroll(Event::consume);
    }
    
    /** {@inheritDoc} */
    @Override
    public void onClose() {
        thumb_reader.stop();    // prevent continued thumbnail creation
        slideshow.stop();       // stop slideshow
    }
    
/********************************* PUBLIC API *********************************/
 
    /** {@inheritDoc} */
    @Override
    public void refresh() {
        thumbSize.applyValue();
        thumbGap.applyValue();
        slideshow_dur.applyValue();
        slideshow_on.applyValue();
        showThumbnails.applyValue();
        hideThumbEager.applyValue();
        thums_rect.applyValue();
        align.applyValue();
        theater_mode.applyValue();
        readThumbnails();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return thumbnails.isEmpty();
    }
    
    /** {@inheritDoc} */
    @Override
    public void showImage(File img_file) {
        if(img_file!=null && img_file.getParentFile()!=null) {
            folder.set(img_file.getParentFile());
            // this resets the slideshow counter
            // notice that we query whether slideshow is running, not whether it is
            // supposed to run, which might not always be the same
            if(slideshow.isRunning()) slideshow.start();
            active_image = -1;
            mainImage.loadImage(img_file);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void showImages(List<File> img_files) {
        if(img_files.isEmpty()) return;
        
        showImage(img_files.get(0));
        active_image = 0;
        img_files.forEach(this::addThumbnail);
    }
    
/****************************** HELPER METHODS ********************************/
    
    @IsInput("Location of")
    private void dataChanged(Item i) {
        if(i==null) dataChanged(Metadata.EMPTY);
        else App.itemToMeta(i, this::dataChanged);
    }
    
    private void dataChanged(Metadata m) {
        // remember data
        data = m;
        // calculate new location
        File new_folder = (data==null || !data.isFileBased()) ? null : data.getLocation();
        dataChanged(new_folder);
    }
    
    @IsInput("Directory")
    private void dataChanged(File i) {
        // calculate new location
        File new_folder = i;
        // prevent refreshing location if shouldnt
        if(keepContentOnEmpty && new_folder==null) return;
        // refresh location
        folder.set(new_folder);
        if(theater_mode.getValue()) itemPane.setValue("", data);
    }
    
    class Exec {
        ExecutorService e = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
                   t.setDaemon(true);
                   return t;
        });
        List<Runnable> l = new ArrayList();
        boolean executing = false;
        
        public void run(Runnable r) {
            if(!l.isEmpty()) l.add(r);
            else {
                e.execute(() -> {
                    r.run();
                    Platform.runLater(() -> {
                        l.remove(r);
                        if(!l.isEmpty()) run(l.get(0));
                    });
                });
            }
        }
        
        public void stop() {
            l.clear();
        }
    }
    
    Exec thumb_reader = new Exec();
    
    
    
    private void readThumbnails() {
        // clear old content before adding new
        // setImage(-1) // it is set during thumbnail reading, no need to clear it
        images.clear();
        thumbnails.clear();
        thumb_pane.getChildren().clear();   

        thumb_reader.stop();
        thumb_reader.run(() -> {
            int ai = active_image;
            // discover files
            List<File> files = folder.get()==null 
                ? EMPTY_LIST
                : getFilesImage(folder.get(),folderTreeDepth).limit(thumbsLimit).collect(toList());
            if(files.isEmpty()) {
                Platform.runLater(() -> setImage(-1));
            } else {
                // add actions - turn file into thumbnail
                forEachWithI(files, (i,f) -> {
                    // create & load thumbnail on bgr thread
                    Thumbnail t = createThumbnail(f);
                    // add to gui
                    runLater(() -> insertThumbnail(t));
                    // set as image if it should be
                    // for example when widget loads, the image might be
                    // restored from deserialized index value
                    if(i==ai) runLater(() -> setImage(ai));
                });
            }
        });
    }
    
    private void addThumbnail(final File f) {
        insertThumbnail(createThumbnail(f));
    }
    
    private Thumbnail createThumbnail(final File f) {
        // create thumbnail
        Thumbnail t = new Thumbnail(thumbSize.getValue(),thumbSize.getValue());
                  t.setBorderToImage(!thums_rect.getValue());
                  t.setBackgroundVisible(thums_rect.getValue());
                  t.hoverable.set(true);
                  t.loadImage(f);
                  t.getPane().setOnMouseClicked( e -> {
                      if (e.getButton() == PRIMARY) {
                          setImage(images.indexOf(f));
                          e.consume();
                      }
                  });
        return t;
    }
    
    private void insertThumbnail(Thumbnail t) {
        // store
        thumbnails.add(t);
        images.add(t.getFile());
        thumb_pane.getChildren().add(t.getPane());
        // if this is first thumbnail display it immediatelly
        // but only if the displayed image isnt one of the thumbnails - isnt located
        // in folder.get() directory
        // avoids image loading + necessary to display custom image, which fires
        // thumbnail refresh and subsequently would change the displayed image
        File displLoc = mainImage.getFile()==null ? null : mainImage.getFile().getParentFile();
        File currLoc = folder.get();
        if (thumbnails.size()==1 && currLoc!=null && !currLoc.equals(displLoc))
            setImage(0);
    }    
    
    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;
        
        if (index == -1) {
            Image i = null;
            mainImage.loadImage(i);
            // this is unnecessary because we check the index for validity
            // also unwanted, sometimes this would erase our deserialized index
            //  active_image = -1;
        } else {
            mainImage.loadImage(images.get(index));
            active_image = index;
        }
    }
    
    public void nextImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else { 
            int index = (active_image >= images.size()-1) ? 0 : active_image+1;
            setImage(index);
        }
        if(slideshow.isRunning()) slideshow.start();
    }
    
    public void prevImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image < 1) ? images.size()-1 : active_image-1;
            setImage(index);
        }
        if(slideshow.isRunning()) slideshow.start();
    }
    
    private void thumbAnimPlay(boolean v) {
        thumbAnim.playFromDir(v);
    }
    
    private void applyTheaterMode(boolean v) {
        if(v && itemPane==null) {
            itemPane = new ItemInfo(false);
            entireArea.getChildren().add(itemPane);
            itemPane.toFront();
            AnchorPane.setBottomAnchor(itemPane, 20d);
            AnchorPane.setRightAnchor(itemPane, 20d);
            itemPane.setValue("", data);
            entireArea.pseudoClassStateChanged(getPseudoClass("theater"), v);

            itemPane.setOnMouseClicked(ee -> {
                if(ee.getButton()==SECONDARY) {
                    if(itemPane.getStyleClass().isEmpty()) itemPane.getStyleClass().addAll("block-alternative");
                    else itemPane.getStyleClass().clear();
                    ee.consume();
                }
            });
        }
        
        slideshow_on.applyValue(v ? false : slideshow_on.getValue());
        mainImage.setBackgroundVisible(v);
        mainImage.setBorderVisible(!v);
        if (itemPane!=null) itemPane.setVisible(v);
    }
}