/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import util.access.FieldValue.FieldEnum;
import util.access.FieldValue.FieldedValue;
import util.collections.Tuple2;
import util.collections.Tuple3;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class FilterGeneratorChain<T extends FieldedValue,F extends FieldEnum<T>> extends ChainConfigField<Tuple2<Predicate<Object>,F>,FilterGenerator<F>> {

    private final List<Tuple3<String,Class,F>> data = new ArrayList();
    private BiFunction<F,Predicate<Object>,Predicate<T>> converter;
    private Predicate<T> conjuction;
    private boolean inconsistent_state = false;
    private Consumer<Predicate<T>> onFilterChange;
    
    public FilterGeneratorChain(Supplier<FilterGenerator<F>> chainedFactory) {
        this(1,chainedFactory);
    }

    public FilterGeneratorChain(int i, Supplier<FilterGenerator<F>> chainedFactory) {
        super(i, chainedFactory);
        conjuction = isTRUE;
    }
    

    protected List<Tuple3<String,Class,F>> getData() {
        return data;
    } 

    public void setData(List<Tuple3<String,Class,F>> classes) {
//        data.clear(); // causes serious problems, unknown
        data.addAll(classes);
        generators.forEach(g->g.chained.setData(classes));
    }

    public void setMapper(BiFunction<F,Predicate<Object>,Predicate<T>> mapper) {
        this.converter = mapper;
    }

    public void setOnFilterChange(Consumer<Predicate<T>> filter_acceptor) {
        onFilterChange = filter_acceptor;
    }
    
    
    public void setPrefTypeSupplier(Supplier<Tuple3<String,Class,F>> supplier) {
        generators.forEach(g->g.chained.setPrefTypeSupplier(supplier));
    }

    public boolean isEmpty() {
        return generators.stream().allMatch(c->c.chained.isEmpty());
    }

    public void clear() {
        inconsistent_state = true;
        generators.setAll(generators.get(0));
        generators.forEach(c->c.chained.clear());
        inconsistent_state = false;
        generateValue();
    }

    @Override
    protected void generateValue() {
        if(inconsistent_state) return;
        conjuction = generators.stream().filter(g->g.on.selected.get())
                                    .map(g->g.chained.getValue()).filter(isNotNULL)
                                    .map(g->converter.apply(g._2,g._1))
                                    .reduce(Predicate::and).orElse(isTRUE);
        if(onFilterChange!=null) onFilterChange.accept(conjuction);
    }
    
}