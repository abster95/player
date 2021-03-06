/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.*;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import org.reactfx.EventSource;
import org.reactfx.Subscription;

import util.dev.TODO;
import util.functional.Functors.Ƒ1;

import static java.time.Duration.ofMillis;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.stage.Modality.APPLICATION_MODAL;
import static javafx.stage.StageStyle.UNDECORATED;
import static javafx.stage.StageStyle.UTILITY;
import static util.async.Async.runFX;
import static util.dev.TODO.Purpose.BUG;
import static util.dev.Util.log;

/**
 * Graphic utility methods.
 *
 * @author Martin Polakovic
 */
public interface Util {

    /** Constructs ordinary {@link javafx.scene.layout.HBox}. Convenience constructor for more fluent style. */
    static HBox layHorizontally(double gap, Pos align, Node... nodes) {
        HBox l = new HBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.HBox}. Convenience constructor for more fluent style. */
    static HBox layHorizontally(double gap, Pos align, List<? extends Node> nodes) {
        HBox l = new HBox(gap);
             l.setAlignment(align);
             l.getChildren().addAll(nodes);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style. */
    static VBox layVertically(double gap, Pos align, Node... nodes) {
        VBox l = new VBox(gap, nodes);
             l.setAlignment(align);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style. */
    static VBox layVertically(double gap, Pos align, List<? extends Node> nodes) {
        VBox l = new VBox(gap);
             l.setAlignment(align);
             l.getChildren().addAll(nodes);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Header at the top
     * shrinks to its content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p/>
     * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
     */
    static VBox layHeaderTop(double gap, Pos align, Node header, Node content) {
        VBox l = layVertically(gap, align, header,content);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Header at the bottom
     * shrinks to its content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p/>
     * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
     */
    static VBox layHeaderBottom(double gap, Pos align, Node content, Node header) {
        VBox l = layVertically(gap, align, content, header);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Vertical layout where content takes as much vertical space as possible. Headers at the top
     * and bottom shrink to their content.
     * Sets {@link VBox#setVgrow(javafx.scene.Node, javafx.scene.layout.Priority)} for content
     * to {@link javafx.scene.layout.Priority#ALWAYS}.
     * <p/>
     * Constructs ordinary {@link javafx.scene.layout.VBox}. Convenience constructor for more fluent style.
     */
    static VBox layHeaderTopBottom(double gap, Pos align, Node headerTop, Node content, Node headerBottom) {
        VBox l = layVertically(gap, align, headerTop, content, headerBottom);
        VBox.setVgrow(content,ALWAYS);
        return l;
    }

    /**
     * Constructs ordinary {@link javafx.scene.layout.StackPane} with children aligned to {@link Pos#CENTER}.
     * Convenience constructor for more fluent style.
     */
    static StackPane layStack(Node... ns) {
        return new StackPane(ns);
    }

    /** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
    static StackPane layStack(Node n,Pos a) {
        StackPane l = new StackPane(n);
        StackPane.setAlignment(n, a);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
    static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2) {
        StackPane l = new StackPane(n1,n2);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
    static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2, Node n3,Pos a3) {
        StackPane l = new StackPane(n1,n2,n3);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        StackPane.setAlignment(n3, a3);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.StackPane}. Convenience constructor for more fluent style. */
    static StackPane layStack(Node n1,Pos a1, Node n2,Pos a2, Node n3,Pos a3, Node n4,Pos a4) {
        StackPane l = new StackPane(n1,n2,n3,n4);
        StackPane.setAlignment(n1, a1);
        StackPane.setAlignment(n2, a2);
        StackPane.setAlignment(n3, a3);
        StackPane.setAlignment(n4, a4);
        return l;
    }

    /** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
    static AnchorPane layAnchor(Node n, Double a) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n, a);
        return p;
    }

    /** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
    static AnchorPane layAnchor(Node n, Double top, Double right, Double bottom, Double left) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n, top, right, bottom, left);
        return p;
    }

    /** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
    static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2);
        return p;
    }

    /** Constructs ordinary {@link javafx.scene.layout.AnchorPane}. Convenience constructor for more fluent style. */
    static AnchorPane layAnchor(Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2,
                                                  Node n3, Double top3, Double right3, Double bottom3, Double left3) {
        AnchorPane p = new AnchorPane();
        setAnchor(p, n1, top1, right1, bottom1, left1, n2, top2, right2, bottom2, left2, n3, top3, right3, bottom3, left3);
        return p;
    }

    /** Sets {@link AnchorPane} anchors to the same value. Null clears all anchors. */
    static void setAnchors(Node n, Double a) {
        if (a == null) {
            AnchorPane.clearConstraints(n);
        } else {
            AnchorPane.setTopAnchor(n, a);
            AnchorPane.setRightAnchor(n, a);
            AnchorPane.setBottomAnchor(n, a);
            AnchorPane.setLeftAnchor(n, a);
        }
    }

    /** Sets {@link AnchorPane} anchors. Null clears the respective anchor. */
    static void setAnchors(Node n, Double top, Double right, Double bottom, Double left) {
        AnchorPane.clearConstraints(n);
        if (top != null) AnchorPane.setTopAnchor(n, top);
        if (right != null) AnchorPane.setRightAnchor(n, right);
        if (bottom != null) AnchorPane.setBottomAnchor(n, bottom);
        if (left != null) AnchorPane.setLeftAnchor(n, left);
    }

    /** Sets {@link javafx.scene.layout.AnchorPane} anchors for node. Convenience method for more fluent style. */
    static void setAnchor(AnchorPane pane, Node n, Double a) {
        pane.getChildren().add(n);
        setAnchors(n, a);
    }

    /** Sets {@link javafx.scene.layout.AnchorPane} anchors for node. Convenience method for more fluent style. */
    static void setAnchor(AnchorPane pane, Node n, Double top, Double right, Double bottom, Double left) {
        pane.getChildren().add(n);
        setAnchors(n, top, right, bottom, left);
    }

    /** Sets {@link javafx.scene.layout.AnchorPane} anchors for nodes. Convenience method for more fluent style. */
    static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2) {
        setAnchor(pane, n1, top1, right1, bottom1, left1);
        setAnchor(pane, n2, top2, right2, bottom2, left2);
    }

    /** Sets {@link javafx.scene.layout.AnchorPane} anchors for nodes. Convenience method for more fluent style. */
    static void setAnchor(AnchorPane pane, Node n1, Double top1, Double right1, Double bottom1, Double left1,
                                                  Node n2, Double top2, Double right2, Double bottom2, Double left2,
                                                  Node n3, Double top3, Double right3, Double bottom3, Double left3) {
        setAnchor(pane, n1, top1, right1, bottom1, left1);
        setAnchor(pane, n2, top2, right2, bottom2, left2);
        setAnchor(pane, n3, top3, right3, bottom3, left3);
    }

    /** Gives the text shape wrapping to its width and scrollable functionalities. */
    @Deprecated
    @TODO(purpose = BUG)
    static ScrollPane layScrollVText(Text t) {
        // This is how it should be done, but there is a bug.
        // Unfortunately the pane resizes with the text so we cant bind
        // t.wrappingWidthProperty().bind(sa.widthProperty());
        // The only (to me) known solution is to make the text t not manageable, but that
        // causes the height calculation of the pane sa fail and consequently breaks the
        // scrolling behavior
        // I do not know what to do anymore, believe me I have tried...
//        Pane sa = new StackPane(t);
//        ScrollPane s = new ScrollPane(sa);
//                   s.setPannable(false);
//                   s.setFitToWidth(true);
//                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
//                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
//        t.wrappingWidthProperty().bind(sa.widthProperty());

        // Scrollbar width hardcoded!
        double reserve = 5;
        ScrollPane s = new ScrollPane(t);
                   s.setOnScroll(Event::consume);
                   s.setPannable(false);
                   s.setFitToWidth(false);
                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
        t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
        return s;
    }

    @Deprecated // the behavior may be unpredictable/change in the future
    @TODO(purpose = BUG)
    static ScrollPane layScrollVTextCenter(Text t) {
        double reserve = 5;
        ScrollPane s = new ScrollPane(new StackPane(t));
                   s.setOnScroll(Event::consume);
                   s.setPannable(false);
                   s.setFitToWidth(false);
                   s.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                   s.setHbarPolicy(ScrollBarPolicy.NEVER);
        t.wrappingWidthProperty().bind(s.widthProperty().subtract(15 + reserve));
        return s;
    }

    /** @return simple background with specified solid fill color and no radius or insets.*/
    static Background bgr(Color c) {
        return new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY));
    }

    /** @return simple border with specified color, solid style, no radius and default width.*/
    static Border border(Color c) {
        return new Border(new BorderStroke(c, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT));
    }

    static void setScaleXY(Node n, double s) {
        n.setScaleX(s);
        n.setScaleY(s);
    }

    static void setScaleXY(Node n, double x, double y) {
        n.setScaleX(x);
        n.setScaleY(y);
    }

    static <E extends Event> void add1timeEventHandler(Stage eTarget, EventType<E> eType, Consumer<E> eHandler) {
        eTarget.addEventHandler(eType, new EventHandler<>() {
            @Override
            public void handle(E event) {
                eHandler.accept(event);
                eTarget.removeEventHandler(eType, this);
            }
        });
    }

    /**
     * Sets minimal, preferred and maximal width and height of the node to provided value.
     * Any bound property will be ignored. Null value will be ignored.
     * If node is not a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    static void setMinPrefMaxSize(Node n, Double widthHeight) {
        setMinPrefMaxSize(n, widthHeight, widthHeight);
    }

    /**
     * Sets minimal, preferred and maximal width and height of the node to provided values.
     * Any bound property will be ignored. Null value will be ignored.
     *
     * If node is not a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    static void setMinPrefMaxSize(Node n, Double width, Double height) {
        if(n instanceof Region) {
            Region r = (Region) n;
            boolean wmin  = width!=null  && !r.minWidthProperty().isBound();
            boolean wpref = width!=null  && !r.prefWidthProperty().isBound();
            boolean wmax  = width!=null  && !r.maxWidthProperty().isBound();
            boolean hmin  = height!=null && !r.minHeightProperty().isBound();
            boolean hpref = height!=null && !r.prefHeightProperty().isBound();
            boolean hmax  = height!=null && !r.maxHeightProperty().isBound();

            if(hmin && wmin)    r.setMinSize(width,height);
            else if (hmin)      r.setMinHeight(height);
            else if (wmin)      r.setMinWidth(height);

            if(hpref && wpref)  r.setPrefSize(width,height);
            else if (hpref)     r.setPrefHeight(height);
            else if (wpref)     r.setPrefWidth(height);

            if(hmax && wmax)    r.setMaxSize(width,height);
            else if (hmax)      r.setMaxHeight(height);
            else if (wmax)      r.setMaxWidth(height);
        }
    }

    /**
     * Sets minimal, preferred and maximal width of the node to provided value.
     * Any bound property will be ignored. Null value will be ignored.
     * If node is not a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    static void setMinPrefMaxWidth(Node n, Double width) {
        if(width!=null && n instanceof Region) {
            Region r = (Region) n;
            if(!r.minWidthProperty().isBound()) r.setMinWidth(width);
            if(!r.prefWidthProperty().isBound()) r.setPrefWidth(width);
            if(!r.maxWidthProperty().isBound()) r.setMaxWidth(width);
        }
    }

    /**
     * Sets minimal, preferred and maximal height of the node to provided value.
     * If property bound, value null or node not a {@link javafx.scene.layout.Region}, this method is a no op.
     */
    static void setMinPrefMaxHeight(Node n, Double height) {
        if(height!=null && n instanceof Region) {
            Region r = (Region) n;
            if(!r.minHeightProperty().isBound()) r.setMinHeight(height);
            if(!r.prefHeightProperty().isBound()) r.setPrefHeight(height);
            if(!r.maxHeightProperty().isBound()) r.setMaxHeight(height);
        }
    }

    static void removeFromParent(Node parent, Node child) {
        if(parent==null || child==null) return;
        if(parent instanceof Pane) {
            ((Pane)parent).getChildren().remove(child);
        }
    }

    static void removeFromParent(Node child) {
        if(child==null) return;
        removeFromParent(child.getParent(),child);
    }

/**************************************************************************************************/

    @Deprecated
    static Subscription hovered(Node n, Consumer<? super Boolean> handler) {
        EventSource<Boolean> events = new EventSource<>();
        EventHandler<MouseEvent> eTrue = e -> events.push(true);
        EventHandler<MouseEvent> eFalse = e -> events.push(false);
        ChangeListener<Boolean> eVal = (o, ov, nv) -> events.push(nv);

        n.hoverProperty().addListener(eVal);
        //        n.addEventFilter(MOUSE_MOVED, eTrue);
        //        n.addEventFilter(MOUSE_ENTERED_TARGET, eTrue);
        //        n.addEventFilter(MOUSE_EXITED_TARGET, eFalse);
        Subscription s = events.successionEnds(ofMillis(50)).subscribe(handler);

        return Subscription.multi(
                s,
                () -> n.removeEventFilter(MOUSE_MOVED, eTrue),
                () -> n.removeEventFilter(MOUSE_ENTERED_TARGET, eTrue),
                () -> n.removeEventFilter(MOUSE_EXITED_TARGET, eFalse),
                () -> n.hoverProperty().removeListener(eVal)
        );
    }



    //http://www.coderanch.com/t/622070/JavaFX/java/control-Tooltip-visible-time-duration
    /**
     * Tooltip behavior is controlled by a private class javafx.scene.control.Tooltip$TooltipBehavior.
     * All Tooltips share the same TooltipBehavior instance via a static private member BEHAVIOR, which
     * has default values of 1sec for opening, 5secs visible, and 200 ms close delay (if mouse exits from node before 5secs).
     *
     * The hack below constructs a custom instance of TooltipBehavior and replaces private member BEHAVIOR with
     * this custom instance.
     */
    @Deprecated
    static void setupCustomTooltipBehavior(int openDelayInMillis, int visibleDurationInMillis, int closeDelayInMillis) {
        try {

            Class TTBehaviourClass = null;
            Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
            for (Class c:declaredClasses) {
                if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
                    TTBehaviourClass = c;
                    break;
                }
            }
            if (TTBehaviourClass == null) return; // abort;

            @SuppressWarnings("unchecked")
            Constructor constructor = TTBehaviourClass.getDeclaredConstructor(Duration.class, Duration.class, Duration.class, boolean.class);
            if (constructor == null) return; // abort;

            constructor.setAccessible(true);
            Object behaviour = constructor.newInstance(
                    new Duration(openDelayInMillis), new Duration(visibleDurationInMillis),
                    new Duration(closeDelayInMillis), false);
            Field behaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
            if (behaviourField == null) return; // abort;

            behaviourField.setAccessible(true);
            // Object defaultBehavior = behaviourField.get(Tooltip.class); // store default behavior
            behaviourField.set(Tooltip.class, behaviour);

        } catch (Exception e) {
            log(Util.class).warn("Aborted customizing tooltip behavior",e);
        }
    }

    /**
     * Creates column that indexes rows from 1 and is right aligned. The column
     * is of type Void - table data type agnostic.
     *
     * @param name name of the column. For example "#"
     * @return the column
     */
    static <T> TableColumn<T,Void> createIndexColumn(String name) {
        TableColumn<T,Void> c = new TableColumn<>(name);
        c.setSortable(false);
        c.setCellFactory(column -> new TableCell<>(){
            {
                setAlignment(CENTER_RIGHT);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setText(null);
                else setText(String.valueOf(getIndex()+1)+ ".");
            }
        });
        return c;
    }

    /**
     * Creates default cell factory, which sets cell text to provided text when
     * cells text equals "". This is to differentiate between empty cell and nonempty
     * cell with 'empty' value.
     * For example: '<empty cell>'
     *
     * @param empty_value empty cell string value
     * @return cell factory
     */
    static Callback<TableColumn<?,?>, TableCell<?,?>> EMPTY_TEXT_DEFAULT_CELL_FACTORY(String empty_value) {
        return new Callback<>() {
            @Override
            public TableCell<?,?> call(TableColumn<?, ?> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        if (item == getItem()) return;

                        super.updateItem(item, empty);

                        if (item == null) {
                            super.setText(null);
                            super.setGraphic(null);
                        } else if ("".equals(item)) {
                            super.setText(empty_value);
                            super.setGraphic(null);
                        } else if (item instanceof Node) {
                            super.setText(null);
                            super.setGraphic((Node) item);
                        } else {
                            super.setText(item.toString());
                            super.setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    /**
     * Same as {@link #cellFactoryAligned(javafx.geometry.Pos, String)}, but
     * the alignment is inferred from the type of element in the cell (not table
     * or column, because we are aligning cell content) in the following way:
     * <br>
     * String content is aligned to CENTER_LEFT and the rest CENTER_RIGHT.
     * <p/>
     * The factory will need to be cast if it its generic types are declared.
     *
     * @param type for cell content.
     */
    static <T,O> Ƒ1<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Class<O> type, String no_val_text) {
        Pos a = type.equals(String.class) ? CENTER_LEFT : CENTER_RIGHT;
        return cellFactoryAligned(a, no_val_text);
    }

    /**
     * Returns {@link TableColumn#DEFAULT_CELL_FACTORY} (the default factory used
     * when no factory is specified), aligning the cell content to specified value.
     * <p/>
     * The factory will need to be cast if it its generic types are declared.
     *
     * @param a cell alignment
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T,O> Ƒ1<TableColumn<T,O>,TableCell<T,O>> cellFactoryAligned(Pos a, String no_val_text) {
        Ƒ1<TableColumn<?,?>, TableCell<?,?>> f = EMPTY_TEXT_DEFAULT_CELL_FACTORY(no_val_text)::call;
        return (Ƒ1) f.andApply(cell -> cell.setAlignment(a));
    }

    /**
     * Convenience method to make it easier to select given rows of the
     * TableView via its SelectionModel.
     * This methods provides alternative to TableViewSelectionModel.selectIndices()
     * that requires array parameter. This method makes the appropriate conversions
     * and selects the items using List parameter
     * <p/>
     * After the method is invoked only the provided rows will be selected - it
     * clears any previously selected rows.
     *
     * @param selectedIndexes
     * @param selectionModel
     */
    static void selectRows(List<Integer> selectedIndexes, TableViewSelectionModel<?> selectionModel) {
        selectionModel.clearSelection();
        int[] newSelected = new int[selectedIndexes.size()];
        for (int i = 0; i < selectedIndexes.size(); i++) {
            newSelected[i] = selectedIndexes.get(i);
        }
        if (newSelected.length != 0) {
            selectionModel.selectIndices(newSelected[0], newSelected);
        }
    }

    EventHandler<MouseEvent> consumeOnSecondaryButton = e-> {
        if (e.getButton()== MouseButton.SECONDARY) e.consume();
    };

    static MenuItem menuItem(String text, EventHandler<ActionEvent> action) {
        MenuItem i = new MenuItem(text);
        if(action!=null) i.setOnAction(action);
        return i;
    }

    static MenuItem menuItem(String text, Runnable action) {
        return menuItem(text, action==null ? null : a -> action.run());
    }

    /**
     * Generates menu items from list of services or actions. Use to populate
     * context menu dynamically.
     * <p/>
     * For example context menu that provides menu items for searching given
     * text on the web, using different search engines. What we want is to
     * generate menu items each executing the same type action.
     *
     * @param <A> service or action that can facilitates the action
     * @param from list of service
     * @param toStr service to string converter for menu item text
     * @param action item click action, uses service A possibly on some parameter
     * like selected table item
     * @return menu item
     */
    static <A> MenuItem[] menuItems(List<A> from, Function<A,String> toStr, Consumer<A> action) {
        return from.stream()
                .map(t -> menuItem(toStr.apply(t), e -> action.accept(t)))
                .toArray(MenuItem[]::new);
    }    

/* ---------- FONT -------------------------------------------------------------------------------------------------- */

    static double computeFontWidth(javafx.scene.text.Font font, String text) {
        return com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(text, font);
    }

    static double computeFontHeight(javafx.scene.text.Font font) {
        return com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().getFontMetrics(font).getLineHeight();
    }

/* ---------- WINDOW ------------------------------------------------------------------------------------------------ */

    /**
     * Create fullscreen modal no taskbar stage on given screen. The stage will have its owner,
     * style and modality initialized and be prepared to be shown.
     * <p/>
     * Use: just set your scene on it and call show().
     * <p/>
     * Easy way to get popup like behavior that:
     * <ul>
     * <li> is always fullscreen
     * <li> is modal - does not lose focus and is always on top of other application windows
     * <li> has no taskbar icon (for your information, javafx normally disallows this, but it is
     * doable using owner stage with UTILITY style).
     * </ul>
     */
    static Stage createFMNTStage(Screen screen) {
        // Using owner stage of UTILITY style is the only way to get a 'top level'
        // window with no taskbar.
        Stage owner = new Stage(UTILITY);
              owner.setOpacity(0); // make sure it will never be visible
              owner.setWidth(5); // stay small to leave small footprint, just in case
              owner.setHeight(5);
              owner.show(); // must be 'visible' for the hack to work

        Stage s = new Stage(UNDECORATED); // no OS header & buttons, we want full space
        s.initOwner(owner);
        s.initModality(APPLICATION_MODAL); // eliminates focus stealing form other apps
        s.setX(screen.getVisualBounds().getMinX()); // screen does not necessarily start at [0,0] !!
        s.setY(screen.getVisualBounds().getMinY());
        s.setWidth(screen.getVisualBounds().getWidth()); // fullscreen...
        s.setHeight(screen.getVisualBounds().getHeight());
        s.setAlwaysOnTop(true); // maybe not needed, but just in case
        // Going fullscreen actually breaks things.
        // We do not need fullscreen, we use UNDECORATED stage of maximum size. Fullscreen
        // was supposed to be more of a final nail to prevent possible focus stealing.
        //
        // In reality, using fullscreen actually causes this issue! - focus stealing
        // and the consequent disappearance of the window (nearly impossible to bring it
        // back). This is even when using modality on the window or even its owner stage.
        //
        // Fortunately things work as they should using things like we do. Pray the moment
        // they do not wont occur though.
        //
        // s.setFullScreen(true); // just in case
        // s.setFullScreenExitHint(""); // completely annoying, remove
        // // not always desired! and if we do not use fullscreen it wont work or we could just
        // // introduce inconsistent behavior. Let the dev implement his own hide if he needs.
        // s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // The owner must not escape garbage collection or remain visible forever
//        s.addEventFilter(WindowEvent.WINDOW_HIDDEN, e -> owner.hide());

        return s;
    }

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

    /** Returns screen containing the given coordinates. Never null. */
    static Screen getScreen(Point2D p) {
        return getScreen(p.getX(), p.getY());
    }

    /** Returns screen containing the given coordinates. Never null. */
    static Screen getScreen(double x, double y) {
        // rely on official util (someone hid it..., good work genius)
        return com.sun.javafx.util.Utils.getScreenForPoint(x, y);

//        for (Screen scr : Screen.getScreens())
//            if (scr.getBounds().intersects(x,y,1,1)) {
//                return scr;
//                // unknown whether this affects functionality
////                break;
//            }
//        // avoid null
//        return Screen.getPrimary();
    }

    /** Captures screenshot of the entire screen and runs custom action on fx thread. */
    static void screenCaptureAndDo(Screen screen, Consumer<Image> action) {
        Rectangle2D r = screen.getBounds();
        screenCaptureAndDo((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight(), action);
    }

    /**
     * Captures screenshot of the screen of given size and position and runs custom
     * action on fx thread.
     */
    static void screenCaptureAndDo(int x, int y, int w, int h, Consumer<Image> action) {
        screenCaptureRawAndDo(
            x, y, w, h,
            img -> {
                Image i = img==null ? null : SwingFXUtils.toFXImage(img, new WritableImage(img.getWidth(), img.getHeight()));
                runFX(() -> action.accept(i));
            }
        );
    }

    /** Captures screenshot of the entire screen and runs custom action on non fx thread. */
    static void screenCaptureRawAndDo(Screen screen, Consumer<BufferedImage> action) {
        Rectangle2D r = screen.getBounds();
        screenCaptureRawAndDo((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight(), action);
    }

    /**
     * Captures screenshot of the screen of given size and position and runs custom
     * action on non fx thread.
     * <p/>
     * Based on:
     * <a href="http://www.aljoscha-rittner.de/blog/archive/2011/03/09/javafxdev-screen-capture-tool-with-200-lines-and-500ms-startup-time/">javafx-dev-screen-capture-tool</a>
     */
    static void screenCaptureRawAndDo(int x, int y, int w, int h, Consumer<BufferedImage> action) {
        EventQueue.invokeLater(() -> {
            try {
                Robot robot = new Robot();
                BufferedImage img = robot.createScreenCapture(new Rectangle(x,y,w,h));
                action.accept(img);
            } catch (Exception ex) {
                log(Util.class).error("Failed to screenshot the screen {}");
                action.accept(null);
            }
        });
    }

}