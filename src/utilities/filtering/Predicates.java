/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities.filtering;

import java.util.function.BiPredicate;
import utilities.SteroidObject;

/**
 *
 * @author Plutonium_
 */
public interface Predicates<T> extends SteroidObject {
    public BiPredicate<T,T> predicate(Class<T> type);
}
