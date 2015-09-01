/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.image;

import java.io.File;
import java.util.function.Consumer;

import javafx.css.PseudoClass;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import util.File.Environment;
import util.async.future.Fut;
import util.graphics.Icons;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static util.async.future.Fut.fut;
import static util.graphics.Util.setAnchors;

/**
 * Thumbnail which can accept a file. A custom action invoked afterwards can be 
 * defined. Thumbnail has a highlight mode showed on hover.
 * <p>
 * File can be accepted either by using file chooser opened by clicking on this
 * thumbnail, or by file drag&drop.
 * 
 * @author Plutonium_
 */
public class ChangeableThumbnail extends Thumbnail {
    private final StackPane rt = new StackPane();
    private final Text icon;
    /** 
     * Action for when image file is dropped or received from file chooser.
     * Default does nothing. Null indicates no action. 
     * <p>
     * Obtaining the image file may be blocking operation. Therefore, the file 
     * is obtained by executing the provided supplier. That must be done 
     * manually and should be done on bgr thread.
     */
    public Consumer<Fut<File>> onFileDropped = null;
    /** 
     * Action for when image is highlighted. 
     * Default does nothing. Must not be null.
     */
    public Consumer<Boolean> onHighlight = v -> {};
    
    public ChangeableThumbnail() {
        super();
        
        root.getChildren().add(rt);
        setAnchors(rt,0d);
        
        // cover add icon
        icon = Icons.createIcon(PLUS, 40);
        icon.setMouseTransparent(true);
        icon.setVisible(false);
        rt.getChildren().add(icon);
        icon.getStyleClass().add("changeable-thumbnail-icon");
        
        // highlight on/off on mouse
        rt.addEventHandler(MOUSE_EXITED, e -> highlight(false));
        rt.addEventHandler(MOUSE_ENTERED, e -> highlight(true));
        rt.addEventHandler(DRAG_OVER, e -> { if(DragUtil.hasImage(e.getDragboard())) highlight(true); });
        rt.addEventHandler(DRAG_EXITED, e -> highlight(false));
        
        // add image on click
        rt.setOnMouseClicked(e -> {
            if (e.getButton()==PRIMARY) {
                File f = Environment.chooseFile("Select image to add to tag",false, new File(""), root.getScene().getWindow());
                if (f!= null && onFileDropped!=null) onFileDropped.accept(fut(f));
            }
        });
        
        // add image on drag & drop image file
        getPane().setOnDragOver(DragUtil.imgFileDragAccepthandlerNo(this::getFile));
        getPane().setOnDragDropped(e -> {
            Dragboard d = e.getDragboard();
            if (onFileDropped!=null && DragUtil.hasImage(d)) {
                onFileDropped.accept(DragUtil.getImage(e));
                e.setDropCompleted(true);
                e.consume();
            }
        });
    }
    
    private void highlight(boolean v) {
        icon.setVisible(v);
        PseudoClass highlightedPC = getPseudoClass("highlighted");
        root.pseudoClassStateChanged(highlightedPC, v);
        img_border.pseudoClassStateChanged(highlightedPC, v);
        imageView.pseudoClassStateChanged(highlightedPC, v);
        onHighlight.accept(v);
    }
}
