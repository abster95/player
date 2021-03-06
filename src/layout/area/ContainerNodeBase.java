/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layout.area;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;

import layout.*;
import layout.container.Container;
import layout.container.bicontainer.BiContainer;
import layout.widget.Widget;
import gui.objects.window.stage.UiContext;
import gui.objects.window.stage.Window;
import gui.objects.icon.Icon;
import util.animation.Anim;
import util.graphics.drag.DragUtil;

import static layout.area.Area.CONTAINER_AREA_CONTROLS_STYLECLASS;
import static layout.area.Area.DRAGGED_PSEUDOCLASS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static gui.Gui.closeAndDo;
import static gui.objects.icon.Icon.createInfoIcon;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static main.App.APP;
import static util.functional.Util.mapB;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 *
 * @author Martin Polakovic
 */
public abstract class ContainerNodeBase<C extends Container> implements ContainerNode {

    private static final String actbTEXT = "Actions\n\n"
        + "Opens action chooser for this container. Browse and run additional non-layout actions "
        + "for this container.";

    protected final C container;
    protected final AnchorPane root = new AnchorPane();
    protected final TilePane icons = new TilePane(8, 8);
    protected final AnchorPane ctrls = new AnchorPane(icons);
    protected boolean isAltCon = false;
    protected boolean isAlt = false;

    Icon absB;

    public ContainerNodeBase(C container) {
        this.container = container;

        root.getChildren().add(ctrls);
        setAnchors(ctrls, 0d);
        ctrls.getStyleClass().addAll(CONTAINER_AREA_CONTROLS_STYLECLASS);

	// build header buttons
	Icon infoB = createInfoIcon("Container settings. See icon tooltips."
                + "\nActions:"
                + "\n\tLeft click: visit children"
                + "\n\tRight click: visit parent container"
        );
	Icon detachB = new Icon(CLONE, 12, "Detach widget to own window", this::detach);
	Icon changeB = new Icon(TH_LARGE, 12, "Change widget", ()->{});
        Icon actB = new Icon(GAVEL, 12, actbTEXT, () ->
            APP.actionPane.show(Container.class, container)
        );
	Icon lockB = new Icon(null, 12, "Lock widget layout", () -> {
	    container.locked.set(!container.locked.get());
	    APP.actionStream.push("Widget layout lock");
	});
        maintain(container.locked, mapB(LOCK,UNLOCK),lockB::icon);
	absB = new Icon(LINK, 12, "Resize widget proportionally", () -> {
	    toggleAbsSize();
	    updateAbsB();
	});
	Icon closeB = new Icon(TIMES, 12, "Close widget", () -> {
	    container.close();
	    APP.actionStream.push("Close widget");
	});
        Icon dragB = new Icon(MAIL_REPLY, 12, "Move widget by dragging");

        // drag
        DragUtil.installDrag(
            ctrls, EXCHANGE, "Switch components",
            DragUtil::hasComponent,
            e -> DragUtil.getComponent(e) == container,
            e -> DragUtil.getComponent(e).swapWith(container.getParent(),container.indexInParent())
        );

        // not that dragging children will drag those, dragging container
        // will drag whole container with all its children
        EventHandler<MouseEvent> dh = e -> {
            if (e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(container,db);
                // signal dragging graphically with css
                ctrls.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, true);
                e.consume();
            }
        };
        dragB.setOnDragDetected(dh);
        ctrls.setOnDragDetected(dh);
        // return graphics to normal
        root.setOnDragDone(e -> ctrls.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, false));

	icons.setNodeOrientation(LEFT_TO_RIGHT);
	icons.setAlignment(Pos.CENTER_RIGHT);
        icons.setPrefColumns(10);
        icons.setPrefHeight(25);
        AnchorPane.setTopAnchor(icons,0d);
        AnchorPane.setRightAnchor(icons,0d);
        AnchorPane.setLeftAnchor(icons,0d);
        icons.getChildren().addAll(infoB, dragB, absB, lockB, actB, detachB, changeB, closeB);

        ctrls.setOpacity(0);
        ctrls.mouseTransparentProperty().bind(ctrls.opacityProperty().isEqualTo(0));

        // switch container/normal layout mode using right/left click
        root.setOnMouseClicked(e -> {
            // close on right click
            if(isAlt && !isAltCon && e.getButton()==SECONDARY && container.getChildren().isEmpty()){
                closeAndDo(root, container::close);
                e.consume();
                return;
            }
            if(isAlt && !isAltCon && e.getButton()==SECONDARY) {
                setAltCon(true);
                e.consume();
            }
        });
        ctrls.setOnMouseClicked(e -> {
            if(isAltCon && e.getButton()==PRIMARY) {
                setAltCon(false);
                e.consume();
            }
        });
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() {
        // Interesting idea, but turns out it is not intuitive. Most of the time, user
        // simply wants to add child, so this gets in the way.
        // go to container if children empty
        // if(container.getChildren().isEmpty())
        //     setAltCon(true);

        isAlt = true;

        container.getChildren().values().forEach(c -> {
            if(c instanceof Container) ((Container)c).show();
            if(c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if(ct!=null) ct.show();
            }
        });
    }

    @Override
    public void hide() {
        if(isAltCon) setAltCon(false);
        isAlt = false;

        container.getChildren().values().forEach(c -> {
            if(c instanceof Container) ((Container)c).hide();
            if(c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if(ct!=null) ct.hide();
            }
        });
    }

    List<Node> getC() {
        List<Node> o = new ArrayList<>(root.getChildren());
        o.remove(ctrls);
        return o;
    }


    void setAltCon(boolean b) {
        if(isAltCon==b) return;
        isAltCon = b;
        new Anim(this::applyanim).dur(250).intpl(b ? x->x : x->1-x).play();
        ctrls.toFront();
    }

    void applyanim(double at) {
        getC().forEach(c->c.setOpacity(1-0.8*at));
        ctrls.setOpacity(at);
    }











    private void toggleAbsSize() {
	Container c = container;
	if (c != null && c instanceof BiContainer) {
	    Splitter s = BiContainer.class.cast(c).ui;
	    s.toggleAbsoluteSizeFor(container.indexInParent());
	}
    }

    void updateAbsB() {
	Container c = container;
	if (c != null && c instanceof BiContainer) {
	    boolean l = c.properties.getI("abs_size") != 0;
            absB.icon(l ? UNLINK : LINK);
	    if (!icons.getChildren().contains(absB))
		icons.getChildren().add(5, absB);
	} else
	    icons.getChildren().remove(absB);
    }

    public void detach() {
        if(!container.hasParent()) return;
        // get first active component
        Component c = container;
        c.getParent().addChild(c.indexInParent(),null);
        // detach into new window
        Window w = UiContext.showWindow(c);
        // set size to that of a source (also add header & border space)
        w.setSize(root.getWidth()+10, root.getHeight()+30);
    }

}
