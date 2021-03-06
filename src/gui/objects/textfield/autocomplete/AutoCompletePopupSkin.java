/**
 * Impl based on ControlsF:
 *
 * Copyright (c) 2014, 2015, ControlsFX
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

package gui.objects.textfield.autocomplete;

import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;

import gui.objects.textfield.autocomplete.AutoCompletePopup.SuggestionEvent;

public class AutoCompletePopupSkin<T> implements Skin<AutoCompletePopup<T>> {
    private final AutoCompletePopup<T> control;
    private final ListView<T> suggestionList;
    private final int LIST_CELL_HEIGHT = 24;
    private final int activationClickCount;

    public AutoCompletePopupSkin(AutoCompletePopup<T> control){
        this(control, 1);
    }

    public AutoCompletePopupSkin(AutoCompletePopup<T> control, int activationClickCount){
        this.control = control;
        this.activationClickCount = activationClickCount;
        suggestionList = new ListView<>(control.getSuggestions());

        /**
         * Here we bind the prefHeightProperty to the minimum height between the
         * max visible rows and the current items list. We also add an arbitrary
         * 5 number because when we have only one item we have the vertical
         * scrollBar showing for no reason.
         */
        suggestionList.prefHeightProperty().bind(
                Bindings.min(control.visibleRowCountProperty(), Bindings.size(suggestionList.getItems()))
                        .multiply(LIST_CELL_HEIGHT).add(5));
        suggestionList.setCellFactory(this::buildListViewCellFactory);

        suggestionList.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount()==activationClickCount)
                chooseSuggestion();
        });
        suggestionList.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER  : chooseSuggestion();
                              break;
                case ESCAPE : if (control.isHideOnEscape()) control.hide();
                              break;
                default     : break;
            }
            e.consume();
        });
    }

    @Override
    public Region getNode() {
        return suggestionList;
    }

    @Override
    public AutoCompletePopup<T> getSkinnable() {
        return control;
    }

    @Override
    public void dispose() {}

    private void chooseSuggestion(){
        onSuggestionChosen(suggestionList.getSelectionModel().getSelectedItem());
    }

    private void onSuggestionChosen(T suggestion){
        if(suggestion != null)
            Event.fireEvent(control, new SuggestionEvent<>(suggestion));
    }

    protected ListCell<T> buildListViewCellFactory(ListView<T> listview) {
        return TextFieldListCell.forListView(control.getConverter()).call(listview);
    }
}