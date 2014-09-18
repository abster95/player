/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import GUI.objects.FilterGenerator.TableFilterGenerator;
import java.util.Collection;
import java.util.function.Predicate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import utilities.access.FieldValue.FieldEnum;
import utilities.access.FieldValue.FieldedValue;

/**
 * 
 * Table with a search filter header that supports filtering with provided gui.
 *
 * @author Plutonium_
 */
public class FilterableTable<T extends FieldedValue<T,F>, F extends FieldEnum<T>> extends ImprovedTable<T> {
    
    private final ObservableList<T> allitems = FXCollections.observableArrayList();
    private final FilteredList<T> filtereditems = new FilteredList(allitems);
    private final SortedList<T> sortedItems = new SortedList(filtereditems);
    public final TableFilterGenerator<T,F> searchBox;
    final VBox root = new VBox(this);
    
    public FilterableTable(F initialVal) {
        searchBox = new TableFilterGenerator(filtereditems, initialVal);
        
        setItems(sortedItems);
        sortedItems.comparatorProperty().bind(comparatorProperty());
        VBox.setVgrow(this, ALWAYS);
        
        searchBox.setVisible(false);
        
        searchBox.addEventFilter(KEY_PRESSED, e -> {
            if(e.getCode()==ESCAPE) {
                searchBox.clear();
                setFilterVisible(false);
                e.consume();
            }
        });
        
        addEventFilter(KEY_PRESSED, e -> {
            KeyCode k = e.getCode();
            if(k==ESCAPE) {
                if(searchBox.isEmpty()) setFilterVisible(false);
                else searchBox.clear();
                e.consume();
            } else if (k.isDigitKey() || k.isLetterKey()){
                if(searchBox.isEmpty()) setFilterVisible(true);
                // e.consume(); // must not consume
            }
        });
    }
    
    /**
     * 
     * @return 
     */
    public TableFilterGenerator<T,F> getSearchBox() {
        return searchBox;
    }
    
    /**
     * 
     * @return 
     */
    public VBox getRoot() {
        return root;
    }
    
    public final ObservableList<T> getItemsRaw() {
        return allitems;
    }
    
    public final SortedList<T> getItemsSorted() {
        return sortedItems;
    }
    
    public final FilteredList<T> getItemsFiltered() {
        return filtereditems;
    }
    
    /**
     * Equivalent to {@code (Predicate<T>) filtereditems.getPredicate();}
     * @return 
     */
    public Predicate<T> getFilterPredicate() {
        return (Predicate<T>) filtereditems.getPredicate();
    }
    
    /**
     * Sets items to the table. If any filter is in effect, it will be aplied.
     * <p>
     * Do not use {@link #setItems(javafx.collections.ObservableList)} or 
     * {@code getItems().setAll(new_items)} . It will cause the filters stop
     * working. The first replaces the table item list (instance of {@link FilteredList}
     * which must not happen. The second would throw an exception as FilteredList
     * is not directly modifiable.
     * 
     * @param items 
     */
    public void setItemsRaw(Collection<T> items) {
        allitems.setAll(items);
    }
    
    public final void setFilterVisible(boolean v) {
        if(searchBox.isVisible()==v) return;
        if(v) root.getChildren().setAll(searchBox,this);
        else root.getChildren().setAll(this);
        searchBox.setVisible(v);
        VBox.setVgrow(this, ALWAYS);
        
        // after gui changes, focus on filter so we type the search criteria
        if(v) searchBox.focus();
    }
    
}