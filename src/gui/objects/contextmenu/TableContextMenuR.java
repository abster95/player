/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.contextmenu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.scene.control.TableView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

/**
 *
 * @author Martin Polakovic
 */
public class TableContextMenuR<E> extends TableContextMenuMR<E,TableView<E>> {
    
    public TableContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder) {
        super(builder);
    }
    public TableContextMenuR(Supplier<ImprovedContextMenu<List<E>>> builder, BiConsumer<ImprovedContextMenu<List<E>>, TableView<E>> mutator) {
        super(builder, mutator);
    }
    
    /**
     * Equivalent to: {@code get(table).show(table, e)} . But called only if the
     * table selection is not empty.
     * 
     * @param table
     * @param e 
     */
    public void show(TableView<E> table, MouseEvent e) {
        show(table, table, e);
    }
    
    public void show(TableView<E> table, ContextMenuEvent e) {
        show(table, table, e);
    }
    
}
