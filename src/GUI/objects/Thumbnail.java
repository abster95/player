/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import AudioPlayer.tagging.Cover.Cover;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.Traits.ScaleOnHoverTrait;
import GUI.objects.ContextMenu.ContentContextMenu;
import Layout.Widgets.Features.ImageDisplayFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import static javafx.scene.input.DataFormat.FILES;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import static javafx.scene.paint.Color.BLACK;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.util.Duration;
import main.App;
import util.Log;
import util.Parser.File.Enviroment;
import util.Parser.File.FileUtil;
import util.Parser.File.ImageFileFormat;
import util.SingleInstance;
import util.TODO;
import util.Util;
import static util.Util.createmenuItem;

/**
 * Thumbnail.
 * (Not necessarily) small image component. Supports resizing operarion and
 * has several added funcionalities like border, background or  mouse over
 * animations.
 * <p>
 * Thumbnail's background and border can be fully styled with css. Additionally,
 * border can be set to frame thumbnail or picture respectively.
 * <p>
 * Image is always positioned inside the thumbnail and aligned to center both
 * vertically and horizontally.
 * <p>
 * In order to save memory, thumbnail will load the image with load scale
 * factor determining the load size. For details see {@link #LOAD_COEFICIENT}
 * and {@link #calculateImageLoadSize()}.
 * <p>
 * Thumbnails initialized with File instead of Image object have
 * additional functionalities (context menu). It is recommended to use file
 * to pass an image into the thumbnail object, when possible.
 */
@TODO("add picture stick from outside/inside for keep ratio=true case")
@IsConfigurable
public final class Thumbnail extends ImageNode implements ScaleOnHoverTrait {
    
    // styleclasses
    public static final String bgr_styleclass = "thumbnail-bgr";
    public static final String border_styleclass = "thumbnail-border";
    
    // global propertiea
    @IsConfig(name="Thumbnail size", info = "Preffered size for thumbnails.")
    public static double default_Thumbnail_Size = 70;
    @IsConfig(name="Thumbnail anim duration", info = "Preffered hover scale animation duration for thumbnails.")
    public static double animDur = 100;
    public static boolean animated = false;
    
    private AnchorPane root = new AnchorPane();
    @FXML ImageView image;
    @FXML StackPane img_container;
    @FXML BorderPane content_container;
    @FXML Pane img_border;
    
    /**
     * Optional file representing the image. Not needed, but recommended. Its
     * needed to achieve context menu functionality that allows manipulation with
     * the image file.
     * More/other file-related functionalities could be supported in the future.
     */
    File img_file;
    
    /** Constructor. 
     * Use if you need  default thumbnail size and the image is expected to
     * change during life cycle.
     */
    public Thumbnail() {
        this(default_Thumbnail_Size);
    }
    
    /**
     * Use when you want to use default sized thumbnail no post-initial changes
     * of the image are expected. In other words situations, where thumbnail object
     * is viewed as immutable create-destroy type.
     * @param img 
     */
    public Thumbnail(Image img) {
        this(default_Thumbnail_Size);
        loadImage(img);
    }
    
    /**
     * Use if you need different size than default thumbnail size and the image
     * is expected to change during life cycle.
     * @param _size 
     */
    public Thumbnail (double _size) {
        FXMLLoader fxmlLoader = new FXMLLoader(Thumbnail.class.getResource("Thumbnail.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            root = (AnchorPane) fxmlLoader.load();
            initialize(_size);
        } catch (IOException e) {
            Log.err("Thumbnail source data coudlnt be read.");
        }        
    }    
    
    /** Use to create more specific thumbnail object from the get go. */
    public Thumbnail (Image img, double size) {
        this(size);
        loadImage(img);
    }
    
    private void initialize(double size) {
        // initialize values
        image.setCache(false);
        setSmooth(true);
        setPreserveRatio(true);
//        image.setCacheHint(CacheHint.SPEED);
        borderToImage(false);
        setBackgroundVisible(true);
        setDragImage(true);
        
        // animations
        installScaleOnHover();
        
        // experimental feature
        // change border framing style on mouse middle button click //experimental
        root.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==MIDDLE)
                setBorderToImage(!isBorderToImage());
        });
        
        setContextMenuOn(true);
        
        // set size
        root.setPrefSize(size,size);
        // initialize image size
        image.setFitHeight(size);
        image.setFitWidth(size);
        // bind image sizes to size
        image.fitHeightProperty().bind(Bindings.min(root.prefHeightProperty(), maxIMGH));
        image.fitWidthProperty().bind(Bindings.min(root.prefWidthProperty(), maxIMGW));

        
        // update ratios
        ratioALL.bind(root.prefWidthProperty().divide(root.prefHeightProperty()));
        image.imageProperty().addListener((o,ov,nv) ->
            ratioIMG.set( nv==null ? 1 : nv.getWidth()/nv.getHeight())
        );
        // keep image border size in line with image size bind pref,max size
        ratioIMG.greaterThan(ratioALL).addListener(border_sizer);
    }
    
 /******************************************************************************/
    
    @Override
    public void loadImage(Image img) {
        load(img, null);
    }
    @Override
    public void loadImage(File img) {
        img_file = img;
        Point2D size = calculateImageLoadSize(root);
        Image i = Util.loadImage(img_file, size.getX(), size.getY());
        load(i, img);
    }
    public void loadImage(Cover img) {
        Point2D size = calculateImageLoadSize(root);
        load(img.getImage(size.getX(), size.getY()), img.getFile());
    }
    
    private void load(Image i, File f) {        
        img_file = f;
        image.setImage(i);
        border_sizer.changed(null, false, ratioIMG.get()>ratioALL.get());
        if(i!=null) {
            maxIMGH.set(i.getHeight()*maxScaleFactor);
            maxIMGW.set(i.getWidth()*maxScaleFactor);
        }
    }
    
    @Override
    public void setFile(File img) {
        img_file = img;
    }

    @Override
    public File getFile() {
        return img_file;
    }
    
    @Override
    public Image getImage() {
        return image.getImage();
    }
    public boolean isEmpty() {
        return getImage() == null;
    }
    @Override
    protected ImageView getView() {
        return image;
    }
    
    /**
     * Note that in order for the image to resize properly prefSize of this pane
     * must be changed!
     * {@inheritDoc }
     * @return 
     */
    @Override
    public AnchorPane getPane() {
        return root;
    }
    
/*******************************  properties  *********************************/
    
    private double maxScaleFactor = 1.3;
    
    /**
     * Sets maximum allowed scaling factor for the image. 
     * <p>
     * The image in the thumbnail scales with it, but only up to its own maximal
     * size defined by:    imageSize * maximumScaleFactor
     * <p>
     * 
     * Default value is 1.3.
     * <p>
     * Note that original size in this context means size (width and height) the
     * image has been loaded with. The image can be loaded with any size, even
     * surpassing that of the resolution of the file.
     * 
     * @see #calculateImageLoadSize()
     * @throws IllegalArgumentException if parameter < 1
     */
    public void setMaxScaleFactor(double val) {
        if(val < 1) throw new IllegalArgumentException("Scale factor < 1 not allowed.");
        maxScaleFactor = val;
    }
    
    /** 
     * Whether border envelops thumbnail or image specifically.
     * This is important for when the picture doesnt have the same aspect ratio
     * as the thumbnail. Setting the border for thumbnail (false) will frame
     * the thumbnail without respect for image size. Conversely, setting the border
     * to image (true) will frame image itself, but the thumbnail itself will not
     * be resized to image size, therefore leaving empty space either horizontally
     * or vertically.
     */
    private boolean borderToImage = false;
    
    /** Returns value of {@link #borderToImage}. */
    public boolean isBorderToImage() {
        return borderToImage;
    }
    
    /** Sets the {@link #borderToImage}. */
    public void setBorderToImage(boolean val) {
        if(borderToImage==val) return;
        borderToImage(val);
    }
    
    /**
     * Set visibility of the border. Default true.
     * @param val 
     */
    private void borderToImage(boolean val) {
        borderToImage = val;
        if(val) {
            root.getStyleClass().remove(border_styleclass);
            img_border.getStyleClass().add(border_styleclass);

        } else {
            img_border.getStyleClass().remove(border_styleclass);
            root.getStyleClass().add(border_styleclass);
        }
    }
    
    /** 
     * Sets visibility of the background. The bgr is visible only when the image
     * size ratio and thumbnail size ratio does not match.
     * Default value is true. Invisible background becomes transparent.
     * Stylizable with css.
     */
    public void setBackgroundVisible(boolean val) {
        if(val) {
            if(!root.getStyleClass().contains(bgr_styleclass))
                root.getStyleClass().add(bgr_styleclass);
        }
        else root.getStyleClass().remove(bgr_styleclass);
    }

    public void setBorderVisible(boolean val) {
        if(val) {
            borderToImage(borderToImage);
        } else {
            root.getStyleClass().remove(border_styleclass);
            img_border.getStyleClass().remove(border_styleclass);
        }
    }
    
    /**
     * Set support for dragging file representing the displayed image. The file
     * can be dragged and dropped anywhere within the application.
     * Default true.
     * @param val 
     */
    public void setDragImage(boolean val) {
        if(val) {
            dragHandler = buildDragHandler();
            root.addEventHandler(DRAG_DETECTED, dragHandler);
        } else {
            root.removeEventHandler(DRAG_DETECTED, dragHandler);
            dragHandler = null;
        }
    }
    
    /**
     * Set whether thumbnail context menu should be used for thumbnail.
     * Default true.
     */
    public void setContextMenuOn(boolean val) {
        if (val) root.addEventHandler(MOUSE_CLICKED,contextMenuHandler);
        else root.removeEventHandler(MOUSE_CLICKED,contextMenuHandler);
    }
    
    private EventHandler<MouseEvent> dragHandler;
    
    private EventHandler<MouseEvent> buildDragHandler() {
        return e -> {
            if(e.getButton()==PRIMARY && img_file!=null) {
                Dragboard db = root.startDragAndDrop(TransferMode.LINK);
                // set image
                if(getImage()!=null) db.setDragView(getImage());
                // set content
                HashMap<DataFormat,Object> c = new HashMap();
                c.put(FILES, Collections.singletonList(img_file));
                db.setContent(c);
            }
        };
    }
    
    public void applyAlignment(Pos val) {
        content_container.getChildren().clear();        
        switch(val) {
            case BASELINE_CENTER:
            case CENTER: content_container.setCenter(img_container); break;
            case BOTTOM_LEFT:
            case BASELINE_LEFT:
            case CENTER_LEFT:
            case TOP_LEFT: content_container.setLeft(img_container); break;
            case BOTTOM_RIGHT:
            case BASELINE_RIGHT:
            case CENTER_RIGHT:
            case TOP_RIGHT: content_container.setRight(img_container); break;
            case TOP_CENTER: content_container.setTop(img_container); break;
            case BOTTOM_CENTER: content_container.setBottom(img_container); break;
        }
    }
    
/***************************  Implemented Traits  *****************************/
    
    // --- scale on hover
    @Override public ObjectProperty<Duration> DurationOnHoverProperty() {
        return durationOnHover;
    }
    @Override public BooleanProperty hoverableProperty() {
        return hoverable;
    }
    private final ObjectProperty<Duration> durationOnHover = new SimpleObjectProperty(this, "durationOnHover", Duration.millis(animDur));
    private final BooleanProperty hoverable = new SimpleBooleanProperty(this, "hoverable", false);
    
    @Override public Node getNode() {
        return root;
    }
    
/******************************************************************************/
    
    private final DoubleProperty ratioIMG = new SimpleDoubleProperty(1);
    private final DoubleProperty ratioALL = new SimpleDoubleProperty(1);
    private final DoubleProperty maxIMGW = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final DoubleProperty maxIMGH = new SimpleDoubleProperty(Double.MAX_VALUE);
    
    public double getRatioPane() {
        return ratioALL.get();
    }
    public double getRatioImg() {
        return ratioIMG.get();
    }
    public DoubleProperty ratioPaneProperty() {
        return ratioALL;
    }
    public DoubleProperty ratioImgProperty() {
        return ratioIMG;
    }
    
    private final ChangeListener<Boolean> border_sizer = (o,ov,nv)-> {
        if(nv) {
            img_border.prefHeightProperty().unbind();
            img_border.prefWidthProperty().unbind();
            img_border.prefHeightProperty().bind(image.fitWidthProperty().divide(ratioIMG));
            img_border.prefWidthProperty().bind(image.fitWidthProperty());
            img_border.maxHeightProperty().unbind();
            img_border.maxWidthProperty().unbind();
            img_border.maxHeightProperty().bind(image.fitWidthProperty().divide(ratioIMG));
            img_border.maxWidthProperty().bind(image.fitWidthProperty());
        } else {
            img_border.prefHeightProperty().unbind();
            img_border.prefWidthProperty().unbind();
            img_border.prefWidthProperty().bind(image.fitHeightProperty().multiply(ratioIMG));
            img_border.prefHeightProperty().bind(image.fitHeightProperty());
            img_border.maxHeightProperty().unbind();
            img_border.maxWidthProperty().unbind();
            img_border.maxWidthProperty().bind(image.fitHeightProperty().multiply(ratioIMG));
            img_border.maxHeightProperty().bind(image.fitHeightProperty());
        }
    };
    

/******************************************************************************/
    
    private static final SingleInstance<ContentContextMenu<Image>,Thumbnail> img_context_menu = new SingleInstance<>(
        () -> {
            ContentContextMenu<Image> m = new ContentContextMenu<>();
            m.getItems().addAll(
                createmenuItem("Save the image as ...", e -> {
                    FileChooser fc = new FileChooser();
                        fc.getExtensionFilters().addAll(ImageFileFormat.filter());
                        fc.setTitle("Save image as...");
                        fc.setInitialFileName("new_image");
                        fc.setInitialDirectory(App.getLocation());
                    File f = fc.showSaveDialog(App.getWindowOwner().getStage());
                    FileUtil.writeImage(m.getValue(), f);
                }),
                createmenuItem("Copy the image to clipboard", e -> {
                    if (m.getValue()==null) return;
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                                     content.putImage(m.getValue());
                    clipboard.setContent(content);
                })
            );
            return m;
        },
        (menu,thumbnail) -> {
            Image i = thumbnail.getImage();
            menu.setValue(i);
            menu.getItems().forEach(m->m.setDisable(i==null));
        }
    );
    
    private static final SingleInstance<ContentContextMenu<File>,Thumbnail> file_context_menu = new SingleInstance<>(
        () -> {
            ContentContextMenu<File> m = new ContentContextMenu<>();
            m.getItems().addAll(
                createmenuItem("Browse location", e ->
                    Enviroment.browse(m.getValue().toURI())
                ),
                createmenuItem("Edit the image in editor", e ->
                    Enviroment.edit(m.getValue())
                ),
                createmenuItem("Fulscreen", e -> {
                    Widget c = WidgetManager.getFactory("Image").create();
//                   Window w = Window.create();
//                          w.setSizeAndLocationToInitial();
//                          w.show();
//                          w.setFullscreen(true);
//                          w.setContent(c.load());
//                          w.getStage().getScene().getRoot().setOnKeyPressed(ke -> {
//                              if(ke.getCode()==ESCAPE) w.hide();
//                          });
//                   
                   
                    Popup p = new Popup();
                    AnchorPane n = new AnchorPane();
                    n.setBackground(new Background(new BackgroundFill(BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
                    n.setPrefWidth(Screen.getPrimary().getBounds().getWidth());
                    n.setPrefHeight(Screen.getPrimary().getBounds().getHeight());
                    p.setHideOnEscape(true);
                    Node cn = c.load();
                    n.getChildren().add(cn);
                    Util.setAPAnchors(cn, 0);
                    ((ImageDisplayFeature)c.getController()).showImage(m.getValue());
                    p.getContent().setAll(n);
                    p.show(App.getWindow().getStage(), 0, 0);
                }),
                createmenuItem("Open image", e ->
                    Enviroment.open(m.getValue())
                ),
                createmenuItem("Delete the image from disc", e ->
                    FileUtil.deleteFile(m.getValue())
                ),
                createmenuItem("Save the image as ...", e -> {
                   File of = m.getValue();
                   FileChooser fc = new FileChooser();
                       fc.getExtensionFilters().addAll(ImageFileFormat.filter());
                       fc.setTitle("Save image as...");
                       fc.setInitialFileName(of.getName());
                       fc.setInitialDirectory(App.getLocation());
                       
                   File nf = fc.showSaveDialog(App.getWindowOwner().getStage());
                   if(nf==null) return;
                   try {
                       Files.copy(of.toPath(), nf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                   } catch (IOException ex) {
                       Log.info("File export failed.");
                   }
                })
            );
            return m;
        },
        (menu,thumbnail) -> {
            File f = thumbnail.getFile();
            menu.setValue(f);
            menu.getItems().forEach(i->i.setDisable(f==null));
        }
    );
    
    private final EventHandler<MouseEvent> contextMenuHandler = e -> {
        if(e.getButton()==SECONDARY) {
            // decide mode (image vs file), build lazily & show where requested
            if (img_file != null)
                file_context_menu.get(this).show(root,e);
            else if (getImage() !=null)
                img_context_menu.get(this).show(root,e);
            e.consume();
        }
    };
    
}