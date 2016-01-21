/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import AudioPlayer.plugin.IsPlugin;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;

import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 <p>
 @author Plutonium_
 */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "DuckDuckGo" )
public class DuckDuckGoImageQBuilder implements HttpSearchQueryBuilder {

    @ParsesFromString
    public DuckDuckGoImageQBuilder() {}

    @Override
    public URI apply(String term) {
        String s =  "https://duckduckgo.com/?q=" + term.replace(" ", "%20") + "&iax=1&ia=images";
        return URI.create(s);
    }

}