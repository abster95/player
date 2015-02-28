
package GUI.ItemHolders.ItemTextFields;

import AudioPlayer.tagging.MoodManager;
import GUI.objects.Pickers.MoodPicker;
import GUI.objects.PopOver.PopOver;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.controlsfx.control.textfield.TextFields;
import util.parsing.ParserImpl.StringStringParser;
import util.parsing.StringParser;

/**
 * Text field intended for mood tagging specifically. It provides two additional
 * functionalities - auto-completion from list moods registered in application 
 * and button to open mood picker popup. The position of the picker popup can be
 * customized. 
 *
 * @author Plutonium_
 */
public class MoodTextField extends ItemTextField<String> {
    
    private final Consumer<String> pickMood = this::setValue;
    private PopOver.NodeCentricPos pos = PopOver.NodeCentricPos.RightCenter;

    public MoodTextField() {
        this(StringStringParser.class);
    }
    public MoodTextField(Class<? extends StringParser<String>> parser_type) {
        super(parser_type);
        
        // set autocompletion
        TextFields.bindAutoCompletion(this, p -> MoodManager.moods.stream()
                    .filter( t -> t.startsWith(p.getUserText()) )
                    .collect(Collectors.toList()));
        
        setEditable(true);
    }
    
    /** 
     * Adds word to moods. New moods are registered by application automatically
     * during tagging, so this method doesn't need to be invoked, it is possible
     * though to extend this field's functionality by it.
     */
    private void autoCompletionLearnWord(String newWord){
        MoodManager.moods.add(newWord);
    }
    
    /** @return the position for the picker to show on */
    public PopOver.NodeCentricPos getPos() {
        return pos;
    }

    /** @param pos the position for the picker to show on */
    public void setPos(PopOver.NodeCentricPos pos) {
        this.pos = pos;
    }

    @Override
    void onDialogAction() {
        MoodPicker mood_picker = getCM();
        PopOver p = new PopOver(mood_picker.getNode());
        p.detachable.set(false);
        p.setArrowSize(0);
        p.setArrowIndent(0);
        p.setCornerRadius(0);
        p.setAutoHide(true);
        p.setAutoFix(true);
        mood_picker.onSelect = mood -> {
            setValue(mood);
            p.hide();
        };
        p.show(this, pos);
    }

    @Override
    String itemToString(String item) {
        return item;
    }
    
    
/******************************* CONTEXT MENU *********************************/
    
    private static MoodPicker cm;
    
    private static MoodPicker getCM() {
        if(cm==null) cm = new MoodPicker();
        return cm;
    }
}
