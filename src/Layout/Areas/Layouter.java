
package Layout.Areas;

import java.util.Objects;

import javafx.animation.*;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import Layout.BiContainerPure;
import Layout.Container;
import Layout.FreeFormContainer;
import Layout.PolyContainer;
import gui.GUI;
import gui.objects.Pickers.Picker;
import gui.objects.Pickers.WidgetPicker;
import util.animation.interpolator.CircularInterpolator;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static gui.GUI.*;
import static javafx.animation.Interpolator.LINEAR;
import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static main.App.APP;
import static util.animation.interpolator.EasingMode.EASE_OUT;
import static util.functional.Util.isInR;
import static util.functional.Util.stream;
import static util.graphics.Util.setAnchors;
import static util.graphics.drag.DragUtil.installDrag;

/**
 * @author uranium
 *
 * @TODO make dynamic indexes work and this widget part of layout map. See
 * TO Do file API section
 */
@Layout.Widgets.Widget.Info
public final class Layouter implements ContainerNode {

    private final Container container;
    private final int index;

    public final Picker<String> cp = new Picker();
    public final AnchorPane root = new AnchorPane(cp.root);

    private final FadeTransition a1;
    private final ScaleTransition a2;
    private final EventHandler<MouseEvent> clickShowHider;
    private final EventHandler<MouseEvent> exitHider;

    public Layouter(Container con, int index) {
        Objects.requireNonNull(con);

        this.index = index;
        this.container = con;

        cp.onSelect = layout -> {
            switch(layout) {
                case "Split Vertically" : closeAndDo(cp.root, this::showSplitV);
                                        break;
                case "Split Horizontally" : closeAndDo(cp.root, this::showSplitH);
                                        break;
                case "Widget" : closeAndDo(cp.root, this::showWidgetArea);
                                break;
                case "Tabs"   : closeAndDo(cp.root, this::showTabs);
                                break;
                case "FreeForm" : closeAndDo(cp.root, this::showFreeform);
                                  break;
            }
        };
        cp.onCancel = this::hide;
        cp.textCoverter = text -> text;
        cp.itemSupply = () -> stream("Split Vertically", "Split Horizontally",
                                        "Widget", "FreeForm"); // , "Tabs"
        cp.buildContent();

        setAnchors(cp.root, 0d);

        Interpolator i = new CircularInterpolator(EASE_OUT);
        a1 = new FadeTransition(ANIM_DUR, cp.root);
        a1.setInterpolator(LINEAR);
        a2 = new ScaleTransition(ANIM_DUR, cp.root);
        a2.setInterpolator(i);

        cp.root.setOpacity(0);
        cp.root.setScaleX(0);
        cp.root.setScaleY(0);

        // drag&drop
        installDrag(
            root, EXCHANGE, "Switch components",
            DragUtil::hasComponent,
            e -> isInR(container, DragUtil.getComponent(e).child,DragUtil.getComponent(e).container),
            e -> container.swapChildren(index,DragUtil.getComponent(e))
        );

        clickShowHider =  e -> {
            if(e.getButton()==PRIMARY) {
                if(cp.root.getOpacity()!=0) return;
                // avoid when under lock
                if(container.lockedUnder.get()) return;
                // rely on the public show() implementation, not internal one
                show();
                e.consume();
            }

        };
        exitHider =  e -> cp.onCancel.run();
//        exitHider =  e -> {
//            // rely on the public show() implementation, not internal one
//            cp.onCancel.run();
//            e.consume();
//        };

        // setParentRec mode
        setWeakMode(true); // this needs to be called in constructor
        // setParentRec show
        setShow(GUI.isLayoutMode());
    }

/****************************  functionality  *********************************/

    @Override
    public void show() {
        showControls(true);
//        openAndDo(cp.root, null);
    }

    @Override
    public void hide() {
        // prevent leaving layout mode when layout mode active
        if(GUI.isLayoutMode())return;
        showControls(false);
//        closeAndDo(cp.root, null);
    }

    private void showControls(boolean val) {
        a1.stop();
        a2.stop();
        if (val) {
            a1.setToValue(1);
            a2.setToX(1);
            a2.setToY(1);
        } else {
            a1.setToValue(0);
            a2.setToX(0);
            a2.setToY(0);
        }
        a1.play();
        a2.play();

//        Interpolator in = new SineInterpolator();
////        forEachIndexedStream(cp.getCells(), (i,n) -> new Anim(millis(300), in, at -> { n.setScaleX(at); n.setScaleY(at); }).delay(millis(i*50)))
////            .toArray(Anim[]::new)
////        cp.getCells().stream().map(n->new Anim(millis(300), i, at -> { n.setScaleX(at); n.setScaleY(at); })).to
//        Transition t = new ParallelTransition(
//            forEachIndexedStream(cp.getCells(), (i,n) -> new Anim(millis(300), in, at -> { n.setScaleX(at); n.setScaleY(at); }).delay(millis(i*50)))
//            .toArray(Anim[]::new)
//        );
//        t.setDelay(millis(500));
//        t.play();
    }

    private boolean clickMode = true;

    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     * Default false.
     * @param val
     */
    public void setWeakMode(boolean val) {
        clickMode = val;

        // always hide on mouse exit, setParentRec
        if (root.getOnMouseExited()==null)
            root.setOnMouseExited(exitHider);
        // swap handlers
        if(val) {
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        } else {
            root.addEventHandler(MOUSE_CLICKED,clickShowHider);
            root.removeEventHandler(MOUSE_ENTERED,clickShowHider);
        }
    }

    public void toggleWeakMode() {
        clickMode = !clickMode;
    }
    public boolean isWeakMode() {
        return clickMode;
    }


    private void showWidgetArea() {
        WidgetPicker w = new WidgetPicker();
        w.onSelect = factory -> {
            closeAndDo(w.root, () -> {
                root.getChildren().remove(w.root);
                root.setOnMouseExited(null);
                // this is the crucial part
                container.addChild(index, factory.create());
                if(GUI.isLayoutMode()) container.show();
                APP.actionStream.push("New widget");
            });
        };
        w.onCancel = () -> closeAndDo(w.root, () -> {
            root.getChildren().remove(w.root);
            showControls(GUI.isLayoutMode());
        });
        w.buildContent();
        root.getChildren().add(w.root);
        setAnchors(w.root, 0d);
        openAndDo(w.root, null);
    }
    private void showSplitV() {
        container.addChild(index, new BiContainerPure(HORIZONTAL));
        APP.actionStream.push("Divide layout");
    }
    private void showSplitH() {
        container.addChild(index, new BiContainerPure(VERTICAL));
        APP.actionStream.push("Divide layout");
    }
    private void showTabs() {
        container.addChild(index, new PolyContainer());
    }
    private void showFreeform() {
        container.addChild(index, new FreeFormContainer());
    }

    @Override
    public Pane getRoot() {
        return root;
    }
}