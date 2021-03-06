/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;

import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 * @author Martin Polakovic
 */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Wikipedia" )
public class WikipediaQBuilder implements SearchUriBuilder {

    @ParsesFromString
    public WikipediaQBuilder() {}

    @Override
    public URI apply(String q) {
        return URI.create("https://en.wikipedia.org/wiki/" + q.replace(" ", "%20"));
    }

}