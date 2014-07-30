/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.style.json;

import com.google.common.base.Function;

import com.google.common.base.Optional;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.Constants;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.map.style.ParserPluginUtils;
import org.mapfish.print.map.style.StyleParserPlugin;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.http.client.ClientHttpRequestFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Supports a JSON based style format.
 * <p>
 *     This style parser support two versions of JSON formatting.  Both versions use the same parameter names for configuring
 *     the values of the various properties of the style but the layout differs between the two and version 2 is more flexible
 *     and powerful than version 1.
 * </p>
 * <h2>Mapfish JSON Style Version 1</h2>
 * <p>
 *     Version 1 is compatible with mapfish print <= v2 and is based on the OpenLayers v2 styling.  The layout is as follows:
 * </p>
 * <pre><code>
 * {
 *   "version" : "1",
 *   "styleProperty":"_gx_style",
 *   "1": {
 *     "fillColor":"#FF0000",
 *     "fillOpacity":0,
 *     "rotation" : "30",
 * <p/>
 *     "externalGraphic" : "mark.png"
 *     "graphicName": "circle",
 *     "graphicOpacity": 0.4,
 *     "pointRadius": 5,
 * <p/>
 *     "strokeColor":"#FFA829",
 *     "strokeOpacity":1,
 *     "strokeWidth":5,
 *     "strokeLinecap":"round",
 *     "strokeDashstyle":"dot",
 * <p/>
 *     "fontColor":"#000000",
 *     "fontFamily": "sans-serif",
 *     "fontSize": "12px",
 *     "fontStyle": "normal",
 *     "fontWeight": "bold",
 *     "haloColor": "#123456",
 *     "haloOpacity": "0.7",
 *     "haloRadius": "3.0",
 *     "label": "${name}",
 *     "labelAlign": "cm",
 *     "labelRotation": "45",
 *     "labelXOffset": "-25.0",
 *     "labelYOffset": "-35.0"
 *    }
 * }
 * </code></pre>
 * <p/>
 * <h2>Mapfish JSON Style Version 2</h2>
 * <p>
 *     Version 2 uses the same property names as version 1 but has a different structure.  The layout is as follows:
 * </p>
 * <pre><code>
 * {
 *   "version" : "2",
 *   // shared values can be declared here (at top level)
 *   // and used in form ${constName} later in json
 *   "val1" : "#FFA829",
 *   // default values for properties can be defined here
 *   " strokeDashstyle" : "dot"
 *   "[population > 300]" : {
 *     // default values for current rule can be defined here
 *     // they will override default values defined at
 *     // higher level
 *     "rotation" : "30",
 * <p/>
 *     //min and max scale denominator are optional
 *     "maxScale" : 1000000,
 *     "minScale" : 100000,
 *     "symbolizers" : [{
 *       // values defined in symbolizer will override defaults
 *       "type" : "point",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *       "rotation" : "30",
 * <p/>
 *       "externalGraphic" : "mark.png"
 *       "graphicName": "circle",
 *       "graphicOpacity": 0.4,
 *       "pointRadius": 5,
 * <p/>
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "line",
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "polygon",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 * <p/>
 *       "strokeColor":"${val1}",
 *       "strokeOpacity":1,
 *       "strokeWidth":5,
 *       "strokeLinecap":"round",
 *       "strokeDashstyle":"dot"
 *     },{
 *       "type" : "text",
 *       "fontColor":"#000000",
 *       "fontFamily": "sans-serif",
 *       "fontSize": "12px",
 *       "fontStyle": "normal",
 *       "fontWeight": "bold",
 *       "haloColor": "#123456",
 *       "haloOpacity": "0.7",
 *       "haloRadius": "3.0",
 *       "label": "[name]",
 *       "fillColor":"#FF0000",
 *       "fillOpacity":0,
 *       "labelAlign": "cm",
 *       "labelRotation": "45",
 *       "labelXOffset": "-25.0",
 *       "labelYOffset": "-35.0"
 *     }
 *   ]}
 * }
 * </code></pre>
 * <p>
 *     As illustrated above the style consists of:
 *     <ul>
 *         <li>The version number (2) (required)</li>
 *         <li>
 *             Common values which can be referenced in symbolizer property values.(optional)
 *             <p>Values can be referenced in the value of a property with the pattern: ${valName}</p>
 *             <p>Value names can only contain numbers, characters, _ or -</p>
 *             <p>
 *                 Values do not have to be the full property they will be interpolated.  For example:
 *                 <code>The value is ${val}</code>
 *             </p>
 *         </li>
 *         <li>
 *             Defaults property definitions(optional):
 *             <p>
 *                 In order to reduce duplication and keep the style definitions small, default values can be specified.  The
 *                 default values in the root (style level) will be used in all symbolizers if the value is not defined.  The
 *                 style level default will apply to all symbolizers defined in the system.
 *             </p>
 *             <p>
 *                 The only difference between a value and a default is that the default has a well known name, therefore defaults
 *                 can also be used as values.
 *             </p>
 *         </li>
 *         <li>
 *             All the styling rules (At least one is required)
 *             <p>
 *                 A styling rule has a key which is the filter which selects the features that the rule will be used to draw and the
 *                 rule definition object.
 *             <p>The filter is either <code>*</code> or an
 *                 <a href="http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#filter-ecql-reference">
 *                 ECQL Expression</a>) surrounded by square brackets.  For example: [att < 23].</p>
 *                 The rule definition is as follows:
 *                 <ul>
 *                     <li>
 *                         Default property values (optional):
 *                         <p>
 *                             Each rule can also have defaults.  If the style and the rule have a default for the same property
 *                             the rule will override the style default.  All defaults can be (of course) overridden by a value
 *                             in a symbolizer.
 *                         </p>
 *                     </li>
 *                     <li>
 *                         minScale (optional)
 *                         <p>
 *                             The minimum scale that the rule should evaluate to true
 *                         </p>
 *                     </li>
 *                     <li>
 *                         maxScale (optional)
 *                         <p>
 *                             The maximum scale that the rule should evaluate to true
 *                         </p>
 *                     </li>
 *                     <li>
 *                         An array of symbolizers. (at least one required).
 *                         <p>
 *                             A symbolizer must have a type property (point, line, polygon, text) which indicates the type of
 *                             symbolizer and it has the attributes for that type of symbolizer.  All values have defaults
 *                             so it is possible to define a symbolizer as with only the type property. The only exception is
 *                             that the "text" symbolizer needs a label property.
 *                         </p>
 *                     </li>
 *                 </ul>
 *             </p>
 *         </li>
 *     </ul>
 * </p>
 * <p/>
 * <h2>Configuration Elements</h2>
 * <ul>
 *     <li><strong>fillColor</strong> - (polygon, point, text) The color used to fill the point graphic, polygon or text.</li>
 *     <li><strong>fillOpacity</strong> - (polygon,  point, text) The opacity used when fill the point graphic, polygon or text.</li>
 *     <li><strong>rotation</strong> - (point) The rotation of the point graphic</li>
 *     <li>
 *         <strong>externalGraphic</strong> - (point) one of the two options for declaring the point graphic to use.  This can
 *         be a URL to the icon to use or, if just a string it will be assumed to refer to a file in the
 *         configuration directory (or subdirectory).  Only files in the configuration directory (or subdirectory) will be allowed.
 *     </li>
 *     <li>
 *         <strong>graphicName</strong> - (point) one of the two options for declaring the point graphic to use.  This is the
 *         default and will be a square if not specified. The option are any of the Geotools Marks.
 *         <p>Geotools has by default 3 types of marks:</p>
 *         <ul>
 *             <li>WellKnownMarks: cross, star, triangle, arrow, X, hatch, square</li>
 *             <li>ShapeMarks: shape://vertline, shape://horline, shape://slash, shape://backslash, shape://dot, shape://plus,
 *                 shape://times, shape://oarrow, shape://carrow, shape://coarrow, shape://ccarrow</li>
 *             <li>TTFMarkFactory: ttf://fontName#code (where fontName is a TrueType font and the code is the code number of the
 *                 character to render for the point.</li>
 *         </ul>
 *     </li>
 *     <li><strong>graphicOpacity</strong> - (point) the opacity to use when drawing the point graphic</li>
 *     <li><strong>pointRadius</strong> - (point) the size at which to draw the point graphic</li>
 *     <li>
 *         <strong>strokeColor</strong> - (line, point, polygon) the color to use when drawing a line or the outline of a
 *         polygon or point graphic
 *     </li>
 *     <li><strong>strokeOpacity</strong> - (line, point, polygon) the opacity to use when drawing the line/stroke</li>
 *     <li><strong>strokeWidth</strong> - (line, point, polygon) the widh of the line/stroke</li>
 *     <li>
 *         <strong>strokeLinecap</strong> - (line, point, polygon) the style used when drawing the end of a line.
 *         <p>
 *             Options:  butt (sharp square edge), round (rounded edge), and square (slightly elongated square edge). Default is butt
 *         </p>
 *     </li>
 *     <li>
 *         <strong>strokeDashstyle</strong> - (line, point, polygon) A string describing how to draw the line or an array of
 *         floats describing the line lengths and space lengths:
 *         <ul>
 *             <li>dot - translates to dash array: [0.1, 2 * strokeWidth]</li>
 *             <li>dash - translates to dash array: [2 * strokeWidth, 2 * strokeWidth]</li>
 *             <li>dashdot - translates to dash array: [3 * strokeWidth, 2 * strokeWidth, 0.1, 2 * strokeWidth]</li>
 *             <li>longdash - translates to dash array: [4 * strokeWidth, 2 * strokeWidth]</li>
 *             <li>longdashdot - translates to dash array: [5 * strokeWidth, 2 * strokeWidth, 0.1, 2 * strokeWidth]</li>
 *             <li>{string containing spaces to delimit array elements} - Example: [1 2 3 1 2]</li>
 *         </ul>
 *     </li>
 *     <li><strong>fontColor</strong> - (text) the color of the text drawn</li>
 *     <li><strong>fontFamily</strong> - (text) the font of the text drawn</li>
 *     <li><strong>fontSize</strong> - (text) the font size of the text drawn</li>
 *     <li><strong>fontStyle</strong> - (text) the font style of the text drawn</li>
 *     <li><strong>fontWeight</strong> - (text) the font weight of the text drawn</li>
 *     <li><strong>haloColor</strong> - (text) the color of the halo around the text</li>
 *     <li><strong>haloOpacity</strong> - (text) the opacity of the halo around the text</li>
 *     <li><strong>haloRadius</strong> - (text) the radius of the halo around the text</li>
 *     <li>
 *         <strong>label</strong> - (text) the expression used to create the label e.  The value is either a string which will
 *         be the hardcoded label or a string surrounded by [] which indicates that it is an ECQL Expression.  Examples:
 *         <ul>
 *             <li>Static label</li>
 *             <li>[attributeName]</li>
 *             <li>['Static Label Again']</li>
 *             <li>[5]</li>
 *             <li>5</li>
 *             <li>env('java.home')</li>
 *             <li>centroid(geomAtt)</li>
 *         </ul>
 *     </li>
 *     <li>
 *         <strong>labelAlign</strong> - the indicator of how to align the text with respect to the geometry.  This property
 *         must have 2 characters, the x-align and the y-align.
 *         <p>
 *             X-Align options:
 *             <ul>
 *                 <li>l - align to the left of the geometric center</li>
 *                 <li>c - align on the center of the geometric center</li>
 *                 <li>r - align to the right of the geometric center</li>
 *             </ul>
 *         </p>
 *         <p>
 *             Y-Align options:
 *             <ul>
 *                 <li>b - align to the bottom of the geometric center</li>
 *                 <li>m - align on the middle of the geometric center</li>
 *                 <li>t - align to the top of the geometric center</li>
 *             </ul>
 *         </p>
 *     </li>
 *     <p/>
 *     <li><strong>labelRotation</strong> - the rotation of the label</li>
 *     <li><strong>labelXOffset</strong> - the amount to offset the label along the x axis.  negative number offset to the left</li>
 *     <li><strong>labelYOffset</strong> - the amount to offset the label along the y axis.  negative number offset to the top
 *         of the printing</li>
 * </ul>
 * <p/>
 * <h2>ECQL references:</h2>
 * <ul>
 *     <li><a href="http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#ecql-expr">
 *         http://docs.geoserver.org/stable/en/user/filter/ecql_reference.html#ecql-expr</a></li>
 *     <li><a href="http://docs.geoserver.org/stable/en/user/filter/function_reference.html#filter-function-reference">
 *         http://docs.geoserver.org/stable/en/user/filter/function_reference.html#filter-function-reference</a></li>
 *     <li><a href="http://docs.geotools.org/stable/userguide/library/cql/ecql.html">
 *         http://docs.geotools.org/stable/userguide/library/cql/ecql.html</a></li>
 *     <li><a href="http://docs.geoserver.org/latest/en/user/tutorials/cql/cql_tutorial.html">
 *         http://docs.geoserver.org/latest/en/user/tutorials/cql/cql_tutorial.html</a></li>
 * </ul>
 */
public final class MapfishJsonStyleParserPlugin implements StyleParserPlugin {
    enum Versions {
        ONE("1") {
            @Override
            Style parseStyle(final PJsonObject json,
                             final StyleBuilder styleBuilder,
                             final Configuration configuration) {
                return new MapfishJsonStyleVersion1(json, styleBuilder, configuration).parseStyle();
            }
        }, TWO("2") {
            @Override
            Style parseStyle(final PJsonObject json,
                             final StyleBuilder styleBuilder,
                             final Configuration configuration) {
                return new MapfishJsonStyleVersion2(json, styleBuilder, configuration).parseStyle();
            }
        };
        private final String versionNumber;

        Versions(final String versionNumber) {
            this.versionNumber = versionNumber;
        }

        abstract Style parseStyle(PJsonObject json, StyleBuilder styleBuilder, Configuration configuration);
    }

    static final String JSON_VERSION = "version";

    private StyleBuilder sldStyleBuilder = new StyleBuilder();


    @Override
    public Optional<Style> parseStyle(@Nullable final Configuration configuration,
                                      @Nonnull final ClientHttpRequestFactory clientHttpRequestFactory,
                                      @Nonnull final String styleString,
                                      @Nonnull final MapfishMapContext mapContext) throws Throwable {
        final Optional<Style> styleOptional = tryLoadJson(configuration, styleString);

        if (styleOptional.isPresent()) {
            return styleOptional;
        }
        return ParserPluginUtils.loadStyleAsURI(configuration, clientHttpRequestFactory, styleString, new Function<byte[],
                Optional<Style>>() {
            @Override
            public Optional<Style> apply(final byte[] input) {
                try {
                    return tryLoadJson(configuration, new String(input, Constants.DEFAULT_CHARSET));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private Optional<Style> tryLoadJson(final Configuration configuration, final String styleString) throws JSONException {
        String trimmed = styleString.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            final PJsonObject json = new PJsonObject(new JSONObject(styleString), "style");

            final String jsonVersion = json.optString(JSON_VERSION, "1");
            for (Versions versions : Versions.values()) {
                if (versions.versionNumber.equals(jsonVersion)) {
                    return Optional.of(versions.parseStyle(json, this.sldStyleBuilder, configuration));
                }
            }
        }

        return Optional.absent();
    }
}