
/**
 * Copyright (c) 2013, 2015 ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gui.objects.grid;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.sun.javafx.scene.control.VirtualScrollBar;

import gui.itemnode.FieldedPredicateChainItemNode;
import gui.itemnode.FieldedPredicateItemNode;
import gui.objects.grid.GridView.SelectionOn;
import main.App;
import util.access.fieldvalue.FileField;
import util.access.fieldvalue.ObjectField;
import util.collections.Tuple3;
import util.functional.Functors;
import util.functional.Functors.Ƒ0;
import util.type.Util;

import static javafx.application.Platform.runLater;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static util.Util.*;
import static util.collections.Tuples.tuple;
import static util.functional.Util.by;
import static util.functional.Util.stream;
import static util.graphics.Util.layHeaderTop;

public class GridViewSkin<T,F> implements Skin<GridView> {

    private final SkinDelegate skin;
    private final VBox root;
    private final StackPane filterPane = new StackPane();
    private final VirtualFlow<GridRow<T,F>> f; // accesses superclass' flow field, do not name "flow"!

    @SuppressWarnings("unchecked")
    public GridViewSkin(GridView<T,F> control) {
        skin = new SkinDelegate(control);

        f = Util.getFieldValue(skin,VirtualFlow.class,"flow"); // make flow field accessible
        f.setId("virtual-flow");
        f.setPannable(false);
        f.setVertical(true);
        f.setFocusTraversable(getSkinnable().isFocusTraversable());
        f.setCellFactory(f -> GridViewSkin.this.createCell());


        root = layHeaderTop(10, Pos.TOP_RIGHT, filterPane, f);
        filter = new Filter(control.type, control.itemsFiltered);

        control.parentProperty().addListener((o,ov,nv) -> {
            root.requestLayout();
        });

        updateGridViewItems();
        updateRowCount();

        // selection
        f.addEventHandler(KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            if(c.isNavigationKey()) {
                if(control.selectOn.contains(SelectionOn.KEY_PRESSED)) {
                    if(c==KeyCode.UP || c==KeyCode.KP_UP) selectUp();
                    if(c==KeyCode.DOWN || c==KeyCode.KP_DOWN) selectDown();
                    if(c==KeyCode.LEFT || c==KeyCode.KP_LEFT) selectLeft();
                    if(c==KeyCode.RIGHT || c==KeyCode.KP_RIGHT) selectRight();
                    if(c==KeyCode.PAGE_DOWN) selectDown();
                    if(c==KeyCode.PAGE_UP) selectUp();
                    if(c==KeyCode.HOME) selectFirst();
                    if(c==KeyCode.END) selectLast();
                    e.consume();
                }
            } else if(c==ESCAPE && !e.isConsumed()) {
                selectNone();
            }
        });
    }

    public VirtualFlow<GridRow<T,F>> getFlow() {
        return f;
    }

    @Override
    public GridView<T,F> getSkinnable() {
        return skin.getSkinnable();
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void dispose() {
        skin.dispose();
    }

    private void updateGridViewItems() {
        ListChangeListener<T> itemsListener = change -> {
            updateRowCount();
            getSkinnable().requestLayout();
            selectNone();
        };
        WeakListChangeListener<T> weakGridViewItemsListener = new WeakListChangeListener<>(itemsListener);
        getSkinnable().getItemsShown().addListener(itemsListener);
        getSkinnable().getItemsRaw().addListener(itemsListener);

        updateRowCount();
        flowRecreateCells();
        getSkinnable().requestLayout();
    }

    void updateRowCount() {
        if (f == null)
            return;

        int oldCount = f.getCellCount();
        int newCount = getItemCount();

        if (newCount != oldCount) {
            f.setCellCount(newCount);
            flowRebuildCells();
        } else {
            flowReconfigureCells();
        }
        updateRows(newCount);
    }

    public GridRow<T,F> createCell() {
        GridRow<T,F> row = new GridRow<>();
        row.setGridView(getSkinnable());
        return row;
    }

    /**
     *  Returns the number of row needed to display the whole set of cells
     *  @return GridView row count
     */
    public int getItemCount() {
        final ObservableList<?> items = getSkinnable().getItemsShown();
        return items == null ? 0 : (int)Math.ceil((double)items.size() / computeMaxCellsInRow());
    }

    /**
     *  Returns the max number of cell per row
     *  @return Max cell number per row
     */
    public int computeMaxCellsInRow() {
        double gap = getSkinnable().horizontalCellSpacingProperty().doubleValue();
        return Math.max((int) Math.floor((computeRowWidth()+gap) / computeCellWidth()), 1);
    }

    /**
     *  Returns the width of a row
     *  (should be GridView.width - GridView.Scrollbar.width)
     *  @return Computed width of a row
     */
    protected double computeRowWidth() {
        return getSkinnable().getWidth() - getVScrollbarWidth();
    }

    /**
     *  Returns the width of a cell
     *  @return Computed width of a cell
     */
    protected double computeCellWidth() {
        return getSkinnable().cellWidthProperty().doubleValue() + (getSkinnable().horizontalCellSpacingProperty().doubleValue() * 2);
    }

    protected double getVScrollbarWidth() {
        if(f!=null) {
            VirtualScrollBar vsb = Util.getFieldValue(f, VirtualScrollBar.class, "vbar");
            return vsb!=null && vsb.isVisible() ? vsb.getWidth() : 0;
        }
        return 0;
    }

    protected void updateRows(int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            GridRow<T,F> row = f.getVisibleCell(i);
            if (row != null) {
                // FIXME hacky - need to better understand what this is about
                // looks like its to force index change update, Q is: is it necessary?
                row.updateIndex(-1);
                row.updateIndex(i);
            }
        }
    }

    protected boolean areRowsVisible() {
        return f!=null && f.getFirstVisibleCell()!=null && f.getLastVisibleCell()!=null;
    }

    private void flowRecreateCells() {
        Util.invokeMethodP0(VirtualFlow.class,f,"recreateCells");
    }

    private void flowRebuildCells() {
        Util.invokeMethodP0(VirtualFlow.class,f,"rebuildCells");
    }

    private void flowReconfigureCells() {
        Util.invokeMethodP0(VirtualFlow.class,f,"reconfigureCells");
    }

    private class SkinDelegate extends CustomVirtualContainerBase<GridView<T,F>,GridRow<T,F>> {
        public SkinDelegate(GridView<T,F> control) {
            super(control);

            registerChangeListener(control.cellHeightProperty(), e -> flowRecreateCells());
            registerChangeListener(control.cellWidthProperty(), e -> { updateRowCount(); flowRecreateCells(); });
            registerChangeListener(control.horizontalCellSpacingProperty(), e -> { updateRowCount(); flowRecreateCells(); });
            registerChangeListener(control.verticalCellSpacingProperty(), e -> flowRecreateCells());
            registerChangeListener(control.widthProperty(), e -> updateRowCount());
            registerChangeListener(control.heightProperty(), e -> updateRowCount());
            registerChangeListener(control.cellFactoryProperty(), e -> flowRecreateCells());
            registerChangeListener(control.parentProperty(), e -> {
                if (getSkinnable().getParent() != null && getSkinnable().isVisible())
                    // getSkinnable().requestLayout();
                    GridViewSkin.this.getSkinnable().requestLayout();
            });
        }

        @Override
        int getItemCount() {
            return GridViewSkin.this.getItemCount();
        }

        @Override
        void updateRowCount() {
            GridViewSkin.this.updateRowCount();
        }
    }

/********************************************* FILTER *********************************************/

    /** Filter pane in the top of the table. */
    public final Filter filter;

    /**
     * Visibility of the filter pane.
     * Filter is displayed in the top of the table.
     * <p/>
     * Setting filter visible will
     * also make it focused (to allow writing filter query immediately). If you
     * wish for the filter to gain focus set this property to true (focus will
     * be set even if filter already was visible).
     * <p/>
     * Setting filter invisible will also clear any search query and effectively
     * disable filter, displaying all table items.
     */
    public BooleanProperty filterVisible = new SimpleBooleanProperty(false) {
        @Override
        public void set(boolean v) {
            if(v && get()) {
                runLater(filter::focus);
                return;
            }

            super.set(v);
            if(!v) filter.clear();

            Node sn = filter.getNode();
            if(v) {
                if(!filterPane.getChildren().contains(sn))
                    filterPane.getChildren().add(0,sn);
            } else {
                filterPane.getChildren().clear();
            }
            filterPane.setMaxHeight(v ? -1 : 0);
            filter.getNode().setVisible(v);

            // focus filter to allow user use filter asap
            if(v) runLater(filter::focus);
        }
    };

    /** Table's filter node. */
    public class Filter extends FieldedPredicateChainItemNode<F,ObjectField<F>> {

        public Filter(Class<F> filterType, FilteredList<T> filterList) {
            this(filterType, filterList, () -> attributes(filterType));
        }

        private Filter(Class<F> filterType, FilteredList<T> filterList, Ƒ0<List<Tuple3<String,Class,ObjectField<F>>>> attributes) {
            super(() -> {
                FieldedPredicateItemNode<F,ObjectField<F>> g = new FieldedPredicateItemNode<>(
                    in -> Functors.getIO(in, Boolean.class),
                    in -> Functors.getPrefIO(in, Boolean.class)
                );
                @SuppressWarnings("unchecked")
                ObjectField<F> prefField = (ObjectField<F>) FileField.NAME_FULL;
                g.setPrefTypeSupplier(() -> tuple(prefField.toString(), prefField.getType(), prefField));
//                g.setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
                g.setData(attributes.get());
                return g;
            });
            @SuppressWarnings("unchecked")
            ObjectField<F> prefField = (ObjectField<F>) FileField.NAME_FULL;
            setPrefTypeSupplier(() -> tuple(prefField.toString(), prefField.getType(), prefField));
//            setPrefTypeSupplier(() -> tuple(prefFilterType.toString(), prefFilterType.getType(), prefFilterType));
//            onItemChange = getSkinnable().itemsFiltered::setPredicate;
            onItemChange = predicate -> filterList.setPredicate(item -> predicate.test(getSkinnable().filterByMapper.apply(item)));
            setData(attributes.get());


            getNode().addEventFilter(KEY_PRESSED, e -> {
                // ESC -> close filter
                if(e.getCode()==ESCAPE) {
                    // clear & hide filter on single ESC
                    // clear();
                    // setFilterVisible(false);


                    // clear filter on 1st, hide on 2nd
                    if(filterVisible.get()) {
                        if(isEmpty()) filterVisible.set(false);
                        else clear();
                        e.consume();
                    }
                }
                // CTRL+F -> hide filter
                if(e.getCode()==F && e.isShortcutDown()) {
                    filterVisible.set(false);
                    GridViewSkin.this.root.requestFocus();
                }
            });

            // addEventFilter would cause ignoring first key stroke when setting filter visible
            GridViewSkin.this.f.addEventHandler(KEY_PRESSED, e -> {
                KeyCode k = e.getCode();
                // CTRL+F -> toggle filter
                if(k==F && e.isShortcutDown()) {
                    filterVisible.set(!filterVisible.get());
                    if(!filterVisible.get()) GridViewSkin.this.root.requestFocus();
                    return;
                }

                if(e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
                // ESC, filter not focused -> close filter
                if(k==ESCAPE) {
                    if(filterVisible.get()) {
                        if(isEmpty()) filterVisible.set(false);
                        else clear();
                        e.consume();
                    }
                }
            });

        }

    }

    private static <R> List<Tuple3<String,Class,ObjectField<R>>> attributes(Class<R> filterType) {
        return stream(App.APP.classFields.get(filterType))
                .filter(ObjectField::isTypeStringRepresentable)
                .map(mf -> tuple(mf.toString(),mf.getType(),mf))
                .sorted(by(e -> e._1))
                .toList();
    }

/******************************************** SELECTION *******************************************/

    private static final int NO_SELECT = Integer.MIN_VALUE;
    private static final PseudoClass SELECTED_PC = getPseudoClass("selected");
    private int selectedI = -1;
    private GridRow<T,F> selectedR = null;
    private GridCell<T,F> selectedC = null;

    void selectRight() {
        select(selectedI+1);
    }

    void selectLeft() {
        select(selectedI-1);
    }

    void selectUp() {
        select(selectedI-computeMaxCellsInRow());
    }

    void selectDown() {
        int sel = selectedI+computeMaxCellsInRow();
        int max = getSkinnable().getItemsShown().size()-1;
        if(sel>max) selectLast();
        else select(sel);
    }

    void selectFirst() {
        select(0);
    }

    void selectLast() {
        select(getSkinnable().getItemsShown().size()-1);
    }

    void selectNone() {
        if(selectedC!=null) selectedC.pseudoClassStateChanged(SELECTED_PC, false);
        if(selectedR!=null) selectedR.pseudoClassStateChanged(SELECTED_PC, false);
        if(selectedC!=null) selectedC.updateSelected(false);
        if(selectedR!=null) selectedR.updateSelected(false);
        getSkinnable().selectedRow.set(null);
        getSkinnable().selectedItem.set(null);
        selectedR = null;
        selectedC = null;
        selectedI = NO_SELECT;
    }

    void select(GridCell<T,F> c) {
        if(c==null || c.getItem()==null) selectNone();
        else select(c.getIndex());
    }

    /** Select cell (and row it is in) at index. No-op if out of range. */
    void select(int i) {
        if(f==null) return;
        if(i==NO_SELECT) throw new IllegalArgumentException("Illegal selection index " + NO_SELECT);

        int itemCount = getSkinnable().getItemsShown().size();
        int iMin = 0;
        int iMax = itemCount-1;
        if(itemCount==0 || i==selectedI || !isInRangeInc(i,iMin,iMax)) return;

        selectNone();

        // find index
        int rows = getItemCount();
        int cols = computeMaxCellsInRow();
        int row = i/cols;
        int col = i%cols;

        if(row<0 || row>rows) return;

        // show row & cell to select
        GridRow<T,F> fsc = f.getFirstVisibleCell();
        GridRow<T,F> lsc = f.getLastVisibleCell();
        boolean isUp   = row<=fsc.getIndex();
        boolean isDown = row>=lsc.getIndex();
        if(fsc.getIndex() >= row || row >= lsc.getIndex()) {
            if(isUp) f.scrollToTop(row);
            else f.scrollTo(row); // TODO: fix weird behavior
        }

        // find row & cell to select
        GridRow<T,F> r = f.getCell(row);
        if(r==null) return;
        GridCell<T,F> c = r.getSkinn().getCellAtIndex(col);
        if(c==null) return;

        selectedI = i;
        selectedR = r;
        selectedC = c;
        selectedR.pseudoClassStateChanged(SELECTED_PC, true);
        selectedC.pseudoClassStateChanged(SELECTED_PC, true);
        selectedC.requestFocus();
        selectedR.updateSelected(true);
        selectedC.updateSelected(true);
        getSkinnable().selectedRow.set(r.getItem());
        getSkinnable().selectedItem.set(c.getItem());
    }

}