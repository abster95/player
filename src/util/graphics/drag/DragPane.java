/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics.drag;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.icon.Icon;
import util.SingleⱤ;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_EXITED_TARGET;
import static util.graphics.Util.layHeaderBottom;
import static util.graphics.Util.removeFromParent;

/**
 * Visual aid for drag operations. Is shown when drag enters drag accepting {@link Node} and hidden
 * when it exists.
 * <p>
 * This pane shows icon and description of the action that will take place when
 * drag is dropped and accepted and highlights the drag accepting area.
 * <p>
 * The drag areas (the nodes which install this pane) do not have to be mutually exclusive, i.e.,
 * the nodes can cover each other, e.g. drag accepting node can be a child of already drag accepting
 * pane. The highlighted area then activates for the topmost drag accepting node
 * <p>
 * The node should be drag accepting - have a drag over handler/filter. The condition under which
 * the node accepts the drag (e.g. only text) should be expressed as a {@link Predicate} and used
 * when installing this pane. Otherwise it will be shown for drag of any content and confuse user.
 * <p>
 * See {@link #installDragSignalPane(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.lang.String, java.util.function.Predicate)}
 *
 * @author Plutonium_
 */
public class DragPane {

    private static final String ACTIVE = "DRAG_PANE";
    private static final String INSTALLED = "DRAG_PANE_INSTALLED";
    private static final String STYLECLASS = "drag-pane";
    private static final SingleⱤ<Pane,Data> PANE = new SingleⱤ<>(() -> {
            Pane p = new StackPane(new Label("Drag"));
                 p.getStyleClass().add(STYLECLASS);
                 p.setMouseTransparent(true);   // must not interfere with events
                 p.setManaged(false);           // must not interfere with layout
            return p;
        },
        (p,data) -> {
            // we could reuse the layout, but premature optimization...
            p.getChildren().setAll(layHeaderBottom(8, Pos.CENTER,
                new Icon(data.icon == null ? CLIPBOARD : data.icon,25),
                new Label(data.name.get()))
            );
        }
    );

    /**
     * Installs drag highlighting for specified node and drag defined by specified predicate,
     * displaying specified icon and action description.
     *
     * @param r drag accepting node
     * @param icon icon symbolizing the action that will take place when drag is dropped
     * @param name description of the action that will take place when drag is dropped
     * @param cond predicate filtering the drag events. Must be consistent with the drag accepting
     * node's DRAG_OVER event handler which accepts the drag! Predicate returning always true will
     * cause the drag highlighting to work regardless of the content of the drag - even if the node
     * does not allow the content to be dropped.
     * <p>
     * It is recommended to build a predicate and use it for drag over handler as well,
     * see {@link DragUtil#accept(java.util.function.Predicate) }. This will guarantee absolute
     * consistency in drag highlighting and drag accepting behavior.
     */
    public static final void installDragSignalPane(Node r, GlyphIcons icon, String name, Predicate<DragEvent> cond) {
        installDragSignalPane(r, icon, () -> name, cond);
    }

    /**
     * Same as {@link #installDragSignalPane(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.lang.String, java.util.function.Predicate)},
     * but the description is supplied (and built) at the time of drag entering the node (every
     * time), so it can be dynamic and reflect certain state.
     */
    public static final void installDragSignalPane(Node r, GlyphIcons icon, Supplier<String> name, Predicate<DragEvent> cond) {
        installDragSignalPane(r, icon, name, cond, e -> false);
    };

    public static final void installDragSignalPane(Node r, GlyphIcons icon, Supplier<String> name, Predicate<DragEvent> cond, Predicate<DragEvent> orConsume) {
        Data d = new Data(name, icon, cond);
        r.getProperties().put(INSTALLED, d);
        //r.addEventFilter(DragEvent.ANY, e -> System.out.println(e.getEventType() + " " + e.getSource())); // debug
        r.addEventHandler(DragEvent.DRAG_OVER, e -> {
            if(!r.getProperties().containsKey(ACTIVE)) {
                if(d.cond.test(e)) {
                    // this may not make sense, one would expect this to ALWAYS be called (before
                    // the check above), but we must remove the pane ONLY when new location acceps
                    // the drag
                    removeFromParent(PANE.get());

                    if(!orConsume.test(e)) {
                        r.getProperties().put(ACTIVE, ACTIVE);
                        Pane p = r instanceof Pane ? (Pane)r : r.getParent()==null ? null : (Pane)r.getParent();
                        Pane dp = PANE.getM(d);
                        if(p!=null && !p.getChildren().contains(dp)) {
                            p.getChildren().add(dp);
                            Bounds b = r.getLayoutBounds();
                            double w = b.getWidth();
                            double h = b.getHeight();
                            dp.setMaxSize(w,h);
                            dp.setPrefSize(w,h);
                            dp.setMinSize(w,h);
                            dp.resizeRelocate(b.getMinX(),b.getMinY(),w,h);
                            dp.toFront();
                        }
                    }
                    e.consume();
                }
            }
        });
        r.addEventHandler(DRAG_EXITED_TARGET, e -> {
            r.getProperties().remove(ACTIVE);
        });
        r.addEventHandler(DRAG_EXITED, e -> {
            removeFromParent(PANE.get());
            r.getProperties().remove(ACTIVE);
        });
    }

    private static class Data {
        final Supplier<String> name;
        final GlyphIcons icon;
        final Predicate<DragEvent> cond;

        Data(Supplier<String> name, GlyphIcons icon, Predicate<DragEvent> cond) {
            this.name = name;
            this.icon = icon;
            this.cond = cond;
        }
    }
}