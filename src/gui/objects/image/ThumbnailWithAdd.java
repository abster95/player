/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.image;

import java.io.File;
import java.util.function.Consumer;

import de.jensd.fx.glyphs.GlyphIcons;
import util.file.Environment;
import util.async.future.Fut;
import util.file.FileType;
import util.graphics.drag.DragPane;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static util.async.future.Fut.fut;
import static util.file.FileType.FILE;

/**
 * Thumbnail which can accept a file. A custom action invoked afterwards can be
 * defined. Thumbnail has a highlight mode showed on hover.
 * <p/>
 * File can be accepted either by using file chooser opened by clicking on this
 * thumbnail, or by file drag&drop.
 *
 * @author Martin Polakovic
 */
public class ThumbnailWithAdd extends Thumbnail {

    private final DragPane.Data dragData;
    /**
     * Action for when image file is dropped or received from file chooser.
     * Default does nothing. Null indicates no action.
     * <p/>
     * Obtaining the image file may be blocking operation (hence the {@link Fut}). The future will
     * never be null, but the obtained image can be null!
     */
    public Consumer<Fut<File>> onFileDropped = null;
    /**
     * Action for when image is highlighted.
     * Default does nothing. Must not be null.
     */
    public Consumer<Boolean> onHighlight = v -> {};

    public ThumbnailWithAdd() {
        this(DETAILS,"Set Image");
    }

    public ThumbnailWithAdd(GlyphIcons dragIcon, String dragDescription) {
        super();
        dragData = new DragPane.Data(() -> dragDescription, dragIcon);

        // highlight on hover | drag
        root.addEventHandler(MOUSE_EXITED, e -> highlight(false));
        root.addEventHandler(MOUSE_ENTERED, e -> highlight(true));
        root.addEventHandler(DRAG_OVER, e -> { if(DragUtil.hasImage(e)) onHighlight.accept(true); });
        root.addEventHandler(DRAG_EXITED, e -> onHighlight.accept(false));

        // add image on click
        root.addEventHandler(MOUSE_CLICKED, e -> {
            if (e.getButton()==PRIMARY) {
                File f = Environment.chooseFile("Select image to add to tag", FILE, new File(""), root.getScene().getWindow());
                if (f!= null && onFileDropped!=null) onFileDropped.accept(fut(f));
                e.consume();
            }
        });

        // drag&drop
        DragUtil.installDrag(
            root, dragIcon,dragDescription,
            DragUtil::hasImage,
            e -> {
                // why does the Fut (CompletableFuture) compute without running the Fut ???
                // now the Fut executes over and over because this event fires like that (btw wtf?)
                // so the below can not be used right now
//                Fut<File> fi = getImage(e);
//                File i = fi.isDone() ? fi.getDone() : null;
//                boolean same = i!=null && i.equals(except.get());

                File i = DragUtil.getImageNoUrl(e);  // workaround
                return i!=null && i.equals(getFile());  // false if image file is already displayed
            },
            e -> {
                if (onFileDropped!=null)
                    onFileDropped.accept(DragUtil.getImage(e));
            }
        );
    }

    private void highlight(boolean v) {
        if(v) DragPane.PANE.getM(dragData).showFor(root);
        else DragPane.PANE.get().hide();

        onHighlight.accept(v);
    }
}