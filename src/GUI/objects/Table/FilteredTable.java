/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.Table;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.InfoNode.InfoTable;
import gui.itemnode.FieldedPredicateChainItemNode;
import gui.itemnode.FieldedPredicateItemNode;
import gui.objects.icon.Icon;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.async.executor.FxTimer;
import util.collections.Tuple3;
import util.functional.Functors;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_MINUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static gui.objects.ContextMenu.SelectionMenuItem.buildSingleSelectionMenu;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.util.Duration.millis;
import static org.reactfx.EventStreams.changesOf;
import static util.Util.getEnumConstants;
import static util.Util.menuItem;
import static util.Util.zeroPad;
import static util.async.Async.runLater;
import static util.collections.Tuples.tuple;
import static util.dev.TODO.Purpose.ILL_DEPENDENCY;
import static util.functional.Util.*;
import static util.reactive.Util.sizeOf;

/**
 *
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Plutonium_
 */
@IsConfigurable("Table")
public class FilteredTable<T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends FieldedTable<T,F> {

    private final ObservableList<T> allitems;
    private final FilteredList<T> filtereditems;
    private final SortedList<T> sortedItems;
    final VBox root = new VBox(this);

    public FilteredTable(F main_field) {
        this(main_field, observableArrayList());
    }

    /**
     * @param main_field field that will denote main column. Must not be null.
     * Also initializes {@link #searchField}.
     *
     * @param main_field be chosen as main and default search field
     * @param backing_list
     */
    public FilteredTable(F main_field, ObservableList<T> backing_list) {
        super((Class<F>)main_field.getClass());
        searchField = main_field;

        allitems = backing_list;
        filtereditems = new FilteredList<>(allitems);
        sortedItems = new SortedList<>(filtereditems);
        itemsPredicate = filtereditems.predicateProperty();

        setItems(sortedItems);
        sortedItems.comparatorProperty().bind(comparatorProperty());
        VBox.setVgrow(this, ALWAYS);

        items_info.bind(this);

        // visually hint user menus are empty
        sizeOf(menuAdd.getItems(), size -> menuAdd.setDisable(size==0));
        sizeOf(menuRemove.getItems(), size -> menuRemove.setDisable(size==0));
        sizeOf(menuSelected.getItems(), size -> menuSelected.setDisable(size==0));
        sizeOf(menuOrder.getItems(), size -> menuOrder.setDisable(size==0));

        filterPane = new Filter(filtereditems, main_field);
        filterPane.getNode().setVisible(false);
        filterPane.getNode().addEventFilter(KEY_PRESSED, e -> {
            // ESC -> close filter
            if(e.getCode()==ESCAPE) {
                // clear & hide filter on single ESC
                // searchBox.clear();
                // setFilterVisible(false);


                // clear filter on 1st, hide on 2nd
                if(filterVisible.get()) {
                    if(filterPane.isEmpty()) filterVisible.set(false);
                    else filterPane.clear();
                    e.consume();
                }
            }
            // CTRL+F -> hide filter
            if(e.getCode()==F && e.isShortcutDown()) {
                filterVisible.set(false);
                requestFocus();
            }
        });

        // using EventFilter would cause ignoring first key stroke when setting
        // search box visible
        addEventHandler(KEY_PRESSED, e -> {
            KeyCode k = e.getCode();
            // CTRL+F -> toggle filter
            if(k==F && e.isShortcutDown()) {
                filterVisible.set(!filterVisible.get());
                if(!filterVisible.get()) requestFocus();
                return;
            }

            if(e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
            // ESC, filter not focused -> close filter
            if(k==ESCAPE) {
                if(filterVisible.get()) {
                    if(filterPane.isEmpty()) filterVisible.set(false);
                    else filterPane.clear();
                    e.consume();
                }
            }

            // typing -> scroll to
            if (k.isDigitKey() || k.isLetterKey()){
                String st = e.getText().toLowerCase();
                // update scroll text
                long now = System.currentTimeMillis();
                boolean append = scrolFTime==-1 || now-scrolFTime<scrolFTimeMax.toMillis();
                scrolFtext = append ? scrolFtext+st : st;
                scrolFTime = now;
                search(scrolFtext);
            }
        });
        addEventFilter(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE && !scrolFtext.isEmpty()) {
                searchEnd();
                e.consume(); // must cause all KEY_PRESSED handlers to be ignored
            }
        });
        addEventFilter(Event.ANY, e -> updateSearchStyles()); // isnt this overkill!?
        changesOf(getItems()).subscribe(c -> updateSearchStyles());
        changesOf(getItems()).subscribe(c -> resizeIndexColumn());

        footerVisible.set(true);
    }

    /** The root is a container for this table and the filter. Use the root instead
     * of this table when attaching it to the scene graph.
     * @return the root of this table */
    public VBox getRoot() {
        return root;
    }

    /** Return the items assigned to this this table. Includes the filtered out items.
     * <p>
     * This list can be modified, but it is recommended to use {@link #setItemsRaw(java.util.Collection)}
     * to change the items in the table.
     */
    public final ObservableList<T> getItemsRaw() {
        return allitems;
    }

    /**
     * Sets items to the table. If any filter is in effect, it will be applied.
     * <p>
     * Do not use {@link #setItems(javafx.collections.ObservableList)} or
     * {@code getItems().setAll(new_items)}. It will cause the filters to stop
     * working. The first replaces the table item list (instance of {@link FilteredList},
     * which must not happen. The second would throw an exception as FilteredList
     * is not directly modifiable.
     *
     * @param items
     */
    public void setItemsRaw(Collection<? extends T> items) {
        allitems.setAll(items);
    }

    /**
     * Maps the index of this list's filtered element to an index in the direct source list.
     *
     * @param index the index in filtered list of items visible in the table
     * @return index in the unfiltered list backing this table
     */
    public int getSourceIndex(int i) {
        return filtereditems.getSourceIndex(i);
    }

/******************************** TOP CONTROLS ********************************/

    /** Filter pane in the top of the table. */
    public final Filter filterPane;

    /*
     * Predicate that filters the table list. Null predicate will match all
     * items (same as always true predicate). The value reflects the filter
     * generated by the user through the {@link #searchBox}. Changing the
     * predicate programmatically is possible, however the searchBox will not
     * react on the change, its effect will merely be overriden and when
     * search box predicate changes, it will in turn override effect of a
     * custom predicate.
     */
    public final ObjectProperty<Predicate<? super T>> itemsPredicate;
    /**
     * Visibility of the filter pane.
     * Filter is displayed in the top of the table.
     * <p>
     * Setting filter visible will
     * also make it focused (to allow writing filter query immediatelly). If you
     * wish for the filter to gain focus set this proeprty to true (focus will
     * be set even if filter already was visible).
     * <p>
     * Setting filter invisible will also clear any search query and effectively
     * disable filter, displaying all table items.
     */
    public BooleanProperty filterVisible = new SimpleBooleanProperty(false) {
        @Override public void set(boolean v) {
            if(v && get()) {
                runLater(filterPane::focus);
                return;
            }

            super.set(v);
            if(!v) filterPane.clear();

            Node sn = filterPane.getNode();
            if(v) {
                if(!root.getChildren().contains(sn))
                    root.getChildren().add(0,sn);
            } else {
                root.getChildren().remove(sn);
            }
            filterPane.getNode().setVisible(v);

            // focus filter to allow user use filter asap
            if(v) runLater(filterPane::focus);
        }
    };

/******************************* BOTTOM CONTROLS ******************************/

    @util.dev.TODO(purpose = ILL_DEPENDENCY, note = "Hardcoded size for consistency, not good")
    public final Menu menuAdd = new Menu("", new Icon(PLAYLIST_PLUS,18).embedded());
    public final Menu menuRemove = new Menu("", new Icon(PLAYLIST_MINUS,18).embedded());
    public final Menu menuSelected = new Menu("", new Icon(FontAwesomeIcon.CROP).embedded(),
            menuItem("Select inverse", this::selectAll),
            menuItem("Select all", this::selectInverse),
            menuItem("Select none", this::selectNone)
    );
    public final Menu menuOrder = new Menu("", new Icon(FontAwesomeIcon.NAVICON).embedded());
    /** Table menubar in the bottom with menus. Feel free to modify. */
    public final MenuBar menus = new MenuBar(menuAdd,menuRemove,menuSelected,menuOrder);
    /**
     * Labeled in the bottom displaying information on table items and selection.
     * Feel free to provide custom implementation of {@link InfoTable#textFactory}
     * to display different information. You may want to reuse
     * {@link InfoTable#DEFAULT_TEXT_FACTORY}.
     */
    public final InfoTable<T> items_info = new InfoTable<>(new Label()); // can not bind here as table items list not ready
    private final HBox bottomLeftPane = new HBox(5,menus,items_info.node){{ setAlignment(CENTER_LEFT); }};
    /**
     * Pane for controls in the bottom of the table.
     * Feel free to modify its content. Menubar and item info label are on the
     * left {@link BorderPane#leftProperty()}.
     */
    public final BorderPane footerPane = new BorderPane(null, null, null, null, bottomLeftPane);

    /**
     * Visibility of the bottom controls and information panel. Displays
     * information about table items and menubar.
     */
    public BooleanProperty footerVisible = new SimpleBooleanProperty(true) {
        @Override public void set(boolean v) {
            super.set(v);
            if(v) {
                if(!root.getChildren().contains(footerPane))
                    root.getChildren().add(footerPane);
            } else {
                root.getChildren().remove(footerPane);
            }
        }
    };

    /** Table's filter node. */
    public class Filter extends FieldedPredicateChainItemNode<T,F> {

        public Filter(FilteredList<T> table_list, F prefFilterType) {
            super(() -> {
                FieldedPredicateItemNode<T,F> g = new FieldedPredicateItemNode<>(
                    in -> Functors.getIO(in, Boolean.class),
                    in -> Functors.getPrefIO(in, Boolean.class)
                );
                g.setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
                g.setData(d(prefFilterType));
                return g;
            });
            onItemChange = v -> filtereditems.setPredicate(v);
            setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
            if(prefFilterType instanceof Enum) {
                setData(d(prefFilterType));
            } else
                throw new IllegalArgumentException("Initial value - field type must be an enum");
        }

    }

    private static <F extends FieldEnum> List<Tuple3<String,Class,F>> d(F prefFilterType) {
        F[] es = (F[]) getEnumConstants(prefFilterType.getClass());
        return stream(es)
                .filter(FieldEnum::isTypeStringRepresentable)
                .map(mf -> tuple(mf.toString(),mf.getType(),mf))
                .sorted(by(e -> e._1))
                .collect(toList());
    }

/********************************** INDEX *************************************/

    /**
     * Shows original item's index in the unfiltered list when filter is on.
     * False will display index within filtered list. In other words false
     * will cause items to always be indexed from 1 to items.size. This has only
     * effect when filter is in effect.
     */
    public final BooleanProperty showOriginalIndex = new SimpleBooleanProperty(true){
        @Override public void set(boolean v) {
            super.set(v);
            refreshColumn(columnIndex);
        }
    };

    /**
     * Indexes range from 1 to n, where n can differ when filter is applied.
     * Equivalent to: {@code isShowOriginalIndex ? getItemsRaw().size() : getItems().size(); }
     * @return max index */
    @Override
    public final int getMaxIndex() {
        return showOriginalIndex.get() ? allitems.size() : getItems().size();
    }

/************************************ SCROLL **********************************/

    public void scrollToCenter(int i) {
        int items = getItems().size();
        if(i<0 || i>=items) return;

        double rows = getHeight()/getFixedCellSize();
        i -= rows/2;
        i = min(items-(int)rows+1,max(0,i));
        scrollTo(i);
    }

/************************************ SEARCH **********************************/

    /**
     * If the user types text to quick search content by scrolling table, the
     * text matching will be done by this field. Its column cell data must be
     * String (or search will be ignored) and column should be visible.
     */
    private F searchField;
    private String scrolFtext = "";
    private long scrolFTime = -1;
    private static final PseudoClass searchmatchPC = getPseudoClass("searchmatch");
    private static final PseudoClass searchmatchnotPC = getPseudoClass("searchmatchnot");
    private final FxTimer scrolFautocancelTimer = new FxTimer(3000,-1,this::searchEnd);

    @IsConfig(name = "Search delay", info = "Maximal time delay between key strokes. Search text is reset after the delay runs out.")
    private static Duration scrolFTimeMax = millis(500);
    @IsConfig(name = "Search auto-cancel", info = "Deactivates search after period of inactivity.")
    private static boolean scrolFautocancel = true;
    @IsConfig(name = "Search auto-cancel delay", info = "Period of inactivity after which search is automatically deactivated.")
    private static Duration scrolFautocancelTime = millis(3000);
    @IsConfig(name = "Search use contains", info = "Use 'contains' instead of 'starts with' for string matching.")
    private static boolean scrolFTimeMatchContain = true;

    /** Sets fields to be used in search. Default is main field. */
    public void searchSetColumn(F field) {
        searchField = field;
    }

    /**
     * Returns whether search is active. Every search must be ended, either
     * automatically {@link #scrolFautocancel}, or manually {@link #searchEnd()}.
     */
    public boolean searchIsActive() {
        return !scrolFtext.isEmpty();
    }

    /**
     * Starts search, searching for the specified string in the designated
     * column for field {@link #searchField} (column can be invisible).
     */
    public void search(String s) {
        scrolFtext = s;
        // scroll to first item beginning with search string
        TableColumn c = getColumn(searchField).orElse(null);
        if(!getItems().isEmpty() && c!=null && c.getCellData(0) instanceof String) {
            for(int i=0;i<getItems().size();i++) {
                String item = (String)getItems().get(i).getField(searchField);
                if(matches(item,scrolFtext)) {
                    scrollToCenter(i);
                    updateSearchStyles();
                    break;
                }
            }
        }
    }

    /**
     * Ends search manually.
     */
    public void searchEnd() {
        scrolFtext = "";
        updateSearchStyleRowsNoReset();
    }

    private void updateSearchStyles() {
        if(scrolFautocancel) scrolFautocancelTimer.start(scrolFautocancelTime);
        updateSearchStyleRowsNoReset();
    }

    private void updateSearchStyleRowsNoReset() {
        boolean searchOn = searchIsActive();
        for (TableRow<T> row : getRows()) {
            T t = row.getItem();
            Object o = t==null ? null : t.getField(searchField);
            boolean isMatch = o instanceof String && matches((String)o,scrolFtext);
            row.pseudoClassStateChanged(searchmatchPC, searchOn && isMatch);
            row.getChildrenUnmodifiable().forEach(c->c.pseudoClassStateChanged(searchmatchPC, searchOn && isMatch));
            row.pseudoClassStateChanged(searchmatchnotPC, searchOn && !isMatch);
            row.getChildrenUnmodifiable().forEach(c->c.pseudoClassStateChanged(searchmatchnotPC, searchOn && !isMatch));
       }
    }

    private boolean matches(String text, String s) {
        String x = s.toLowerCase();
        String in = text.toLowerCase();
        return scrolFTimeMatchContain ? in.contains(s) : in.startsWith(x);
    }


/************************************* SORT ***********************************/

    /** {@inheritDoc} */
    @Override
    public void sortBy(F field) {
        getSortOrder().clear();
        allitems.sort(by(p -> (Comparable) p.getField(field)));
    }

    /***
     * Sorts items using provided comparator. Any sort order is cleared.
     * @param comparator
     */
    public void sort(Comparator<T> comparator) {
        getSortOrder().clear();
        allitems.sort(comparator);
    }

/*********************************** HELPER ***********************************/

    @Override
    protected Callback<TableColumn<T, Void>, TableCell<T, Void>> buildIndexColumnCellFactory() {
        return ( column -> new TableCell<T,Void>() {
            {
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setText(null);
                else {
                    int j = getIndex();
                    String txt;
                    if(zeropadIndex.get()) {
                        int i = showOriginalIndex.get() ? filtereditems.getSourceIndex(j) : j;      // BUG HERE
                        txt = zeroPad(i+1, getMaxIndex(), '0');
                    } else
                        txt = String.valueOf(j+1);

                    setText( txt + ".");
                }
            }
        });
    }

    @Override
    public TableColumnInfo getDefaultColumnInfo() {
        boolean needs_creating = columnVisibleMenu==null;
        TableColumnInfo tci = super.getDefaultColumnInfo();
        if(needs_creating) {
            columnVisibleMenu.getItems().add(
                buildSingleSelectionMenu(
                    "Search column",
                    filter(getFields(),f -> isIn(f.getType(),String.class,Object.class)), // objects too, they can be strings
                    searchField,
                    field -> field.name(),
                    field -> searchField=field
                )
            );
        }
        return tci;
    }

}