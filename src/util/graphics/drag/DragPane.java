/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics.drag;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;

import de.jensd.fx.glyphs.GlyphIcons;
import util.SingleR;
import util.functional.Functors.Ƒ1;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_EXITED_TARGET;
import static util.functional.Util.IS;

/**
 * Placeholder node as a visual aid for mouse drag operations. Shown when drag enters drag
 * accepting {@link Node} and hidden when it exists.
 * <p/>
 * This pane shows icon and description of the action that will take place when
 * drag is dropped and accepted and highlights the drag accepting area.
 * <p/>
 * The drag areas (the nodes which install this pane) do not have to be mutually exclusive, i.e.,
 * the nodes can cover each other, e.g. drag accepting node can be a child of already drag accepting
 * pane. The highlighted area then activates for the topmost drag accepting node
 * <p/>
 * The node should be drag accepting - have a drag over handler/filter. The condition under which
 * the node accepts the drag (e.g. only text) should be expressed as a {@link Predicate} and used
 * when installing this pane. Otherwise it will be shown for drag of any content and confuse user.
 * <p/>
 * See {@link #install(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.lang.String, java.util.function.Predicate)}
 *
 * @author Martin Polakovic
 */
public class DragPane extends Placeholder {

    private static final String ACTIVE = "DRAG_PANE";
    private static final String INSTALLED = "DRAG_PANE_INSTALLED";
    private static final String STYLECLASS = "drag-pane";
    private static final String STYLECLASS_ICON = "drag-pane-icon";
    private static final GlyphIcons DEFAULT_ICON = CLIPBOARD;
    public static final SingleR<DragPane,Data> PANE = new SingleR<>(DragPane::new,
        (p,data) -> {
            p.icon.setIcon(data.icon == null ? DEFAULT_ICON : data.icon);
            p.desc.setText(data.name.get());
        }
    );

    public static final void install(Node r, GlyphIcons icon, String name, Predicate<? super DragEvent> cond) {
        DragPane.install(r, icon, () -> name, cond);
    }

    public static final void install(Node r, GlyphIcons icon, Supplier<String> name, Predicate<? super DragEvent> cond) {
        install(r, icon, name, cond, e -> false, null);
    };

    /**
     * Installs drag highlighting for specified node and drag defined by specified predicate,
     * displaying specified icon and action description.
     * <p/>
     *
     * @param node drag accepting node. The node should be the accepting object for the drag event.
     *
     * @param icon icon symbolizing the action that will take place when drag is dropped
     *
     * @param name description of the action that will take place when drag is dropped. The text
     * is supplied when the drag enters the node. Normally, just pass in {@code () -> "text" }, but
     * you can derive the text from a state of the node or however you wish, e.g. when the action
     * can be different under some circumstances.
     * @param cond predicate filtering the drag events. The highlighting will show if the drag
     * event tests true.
     * <p/>
     * Must be consistent with the node's DRAG_OVER event handler which accepts the drag in order
     * for highlighting to make sense! Check out
     * {@link DragUtil#installDrag(javafx.scene.Node, de.jensd.fx.glyphs.GlyphIcons, java.lang.String, java.util.function.Predicate, java.util.function.Consumer) }
     * which guarantees consistency.
     * <p/>
     * Normally, one simple queries the Dragboard of the event for type of content. Predicate
     * returning always true will cause the drag highlighting to show regardless of the content of
     * the drag - even if the node does not allow the content to be dropped.
     * <p/>
     * It is recommended to build a predicate and use it for drag over handler as well,
     * see {@link DragUtil#accept(java.util.function.Predicate) }. This will guarantee absolute
     * consistency in drag highlighting and drag accepting behavior.
     *
     * @param except Optionally, it is possible to forbid drag highlighting even if the condition
     * tests true. This is useful for when node that shouldnt accept given event doesn't wish for
     * any of its parents accept (and highlight if installed) the drag. For example node may use
     * this parameter to refuse drag&drop from itself.
     * <pre>
     * This can simply be thought of as this (pseudocode):
     * Condition: event -> event.hasImage   // accept drag containing image file
     * Except: event -> hasPngImage   // but ignore pngs, accept all other images
     * </pre>
     * Exception condition should be a subset of condition - if condition returns false, the except
     * should as well. It should only constrict the condition
     * <p/>
     * Using more strict condition (logically equivalent with using except condition) will not have
     * the same effect, because then any parent node which can accept the event will be able to do
     * so and also show drag highlight if it is installed. Normally that is the desired behavior
     * here, but there are cases when it is not.
     * <p/>
     * Generally, this is useful to hint that the node which would normally accept the event, can
     * not. If the condition is not met, parents may accept the event instead. But if it is and the
     * except condition returns true, then area covering the node will refuse the drag whether
     * parents like it or not.
     *
     * @param area Optionally, the highlighting can have specified size and position. Normally it
     * mirrors the size and position of the node.
     * This function is called repeatedly (on DRAG_OVER event, which behaves like MOUSE_MOVE ) and
     * may be used to calculate size and position of the highlight. The result can be a portion of
     * the node's area and even react on mouse drag moving across the node.
     */
    public static final void install(Node node, GlyphIcons icon, Supplier<String> name, Predicate<? super DragEvent> cond, Predicate<? super DragEvent> except, Ƒ1<DragEvent,Bounds> area) {
        Data d = new Data(name, icon, cond);
        node.getProperties().put(INSTALLED, d);
        node.addEventHandler(DragEvent.DRAG_OVER, e -> {
            if(!node.getProperties().containsKey(ACTIVE)) { // guarantees cond executes only once
                if(d.cond.test(e)) {
                    PANE.get().hide();

                    if(except==null || !except.test(e)) { // null is equivalent to e -> false
                        node.getProperties().put(ACTIVE, ACTIVE);
                        Pane p = node instanceof Pane ? (Pane)node : node.getParent()==null ? null : (Pane)node.getParent();
                        Pane dp = PANE.getM(d);
                        if(p!=null && !p.getChildren().contains(dp)) {
                            p.getChildren().add(dp);
                            Bounds b = area==null ? node.getLayoutBounds() : area.apply(e);
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

            if(area!=null && node.getProperties().containsKey(ACTIVE)) {
                Pane dp = PANE.getM(d);
                    Bounds b = area.apply(e);
                    double w = b.getWidth();
                    double h = b.getHeight();
                    dp.setMaxSize(w,h);
                    dp.setPrefSize(w,h);
                    dp.setMinSize(w,h);
                    dp.resizeRelocate(b.getMinX(),b.getMinY(),w,h);
                    dp.toFront();
            }
        });
        node.addEventHandler(DRAG_EXITED_TARGET, e -> {
            node.getProperties().remove(ACTIVE);
        });
        node.addEventHandler(DRAG_EXITED, e -> {
            PANE.get().hide();
            node.getProperties().remove(ACTIVE);
        });
    }

    public static class Data {
        private final Supplier<String> name;
        private final GlyphIcons icon;
        private final Predicate<? super DragEvent> cond;

        public Data(Supplier<String> name, GlyphIcons icon) {
            this.name = name;
            this.icon = icon;
            this.cond = IS;
        }
        public Data(Supplier<String> name, GlyphIcons icon, Predicate<? super DragEvent> cond) {
            this.name = name;
            this.icon = icon;
            this.cond = cond;
        }
    }

    private DragPane() {
        super(DEFAULT_ICON,null,null);
        icon.styleclass(STYLECLASS_ICON);
        getStyleClass().add(STYLECLASS);
        setMouseTransparent(true);   // must not interfere with events
        setManaged(false);           // must not interfere with layout
    }
}