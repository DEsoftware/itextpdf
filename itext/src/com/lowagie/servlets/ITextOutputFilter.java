/*
 * $Name$
 * $Id$
 *
 * Copyright 2002 by Marcelo Vanzin.
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */

package com.lowagie.servlets;


//Import General
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

// Import iText
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.PageSize;

import com.lowagie.text.xml.SAXiTextHandler;

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.rtf.RtfWriter;
import com.lowagie.text.xml.XmlWriter;
import com.lowagie.text.html.HtmlWriter;

/**
 *  <p>Extends OutputfilterBase to pass the output from the filter chain through
 *  the iText library.</p>
 *
 *  @author     Marcelo Vanzin
 *  @created    18/Abr/2002
 */
public class ITextOutputFilter extends OutputFilterBase {

    // Tipos de sa�da
    public final static int PDF_OUTPUT  = 0;
    public final static int RTF_OUTPUT  = 1;
    public final static int XML_OUTPUT  = 2;
    public final static int HTML_OUTPUT = 3;
    
    private static final String[] CONTENT_TYPES =  {
        "application/pdf",
        "text/rtf",
        "text/xml",
        "text/html"
    };
    
    // Constantes que definem os atributos que podem ser mudados no PDF
    
    /** Request key where to store the desired page size. */
    public final static String PAGE_SIZE_KEY   = "ITEXT_PAGESIZE";

    /** Request key where to store the desired output type. */
    public final static String OUTPUT_TYPE_KEY = "ITEXT_OUTPUT_TYPE";

    // Atributos internos

    private Rectangle pageSize = PageSize.A4;
    private int outputType     = PDF_OUTPUT;
    
    private final SAXParserFactory spf = SAXParserFactory.newInstance();
    
    private static final String PAGE_SIZE   = "pageSize";
    private static final String OUTPUT_TYPE = "defaultOutput";
    
    /**
     *  <p>Initializes the filter. Possible configuration parameters are:</p>
     *
     *  <ul>
     *      <li>pageSize: the desired default page size. This must be the name
     *      of a constant in the PageSize class. Default: A4.</li>
     *
     *      <li>defaultOutput: The default output desired. Currently, only the
     *      writer bundled with iText are supported. Possible values are: PDF,
     *      RTF, XML, HTML. Default: PDF.</li>
     *  </ul>
     *
     *  @see com.lowagie.text.PageSize
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        
        String tmp;
        
        // Tamanho da p�gina
        tmp = filterConfig.getInitParameter(PAGE_SIZE);
        if (tmp != null) {
            try {
                pageSize = (Rectangle) PageSize.class.getField(tmp).get(null);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Tipo de sa�da
        tmp = filterConfig.getInitParameter(OUTPUT_TYPE);
        if (tmp != null) {
            if (tmp.equalsIgnoreCase("PDF")) {
                outputType = PDF_OUTPUT;
            } else if (tmp.equalsIgnoreCase("RTF")) {
                outputType = RTF_OUTPUT;
            } else if (tmp.equalsIgnoreCase("XML")) {
                outputType = XML_OUTPUT;
            } else if (tmp.equalsIgnoreCase("HTML")) {
                outputType = HTML_OUTPUT;
            }
        }
    }

    /**
     *  <p>Calls iText to turn the passed input stream into a document in
     *  the desired format (either the default, or a format passed as a
     *  request attribute). The input stream must contain a XML document in
     *  the format understood by iText. Currently, tag maps are not supported.</p>
     *
     *  <p>The following atributes may be provided in the request to modify
     *  the target document:</p>
     *
     *  <ul>
     *      <li>PAGE_SIZE_KEY: the page size. Must be an instance of
     *      {@link com.lowagie.text.Rectangle Rectangle}.</li>
     *
     *      <li>OUTPUT_TYPE_KEY: the type of the output document. Must be an
     *      {@link java,lang.Integer Integer} containing one of the constant
     *       values defined in this class. Defaults to PDF.</li>
     *  </ul>
     *
     *  @param  request     The original request from the filter chain.
     *  @param  response    The original response from the filter chain.
     *  @param  data        The data collected from the called resources.
     */
    public void perform(ServletRequest request,
                        ServletResponse response,
                        InputStream data) throws Exception {

        // Iniciando...            
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();

        // Page Size
        if (request.getAttribute(PAGE_SIZE_KEY) != null) {
            try {
                Rectangle ps = (Rectangle) request.getAttribute(PAGE_SIZE_KEY);
                doc.setPageSize(ps);
            } catch (Exception e) {
               doc.setPageSize(pageSize);
            }
        } else {
            doc.setPageSize(pageSize);
        }
        
        // Definindo a sa�da
        int output = outputType; 
        try {
            Integer i = (Integer) request.getAttribute(OUTPUT_TYPE_KEY);
            if (i != null && (i.intValue() >= PDF_OUTPUT && i.intValue() <= HTML_OUTPUT)) {
                output = i.intValue();
            }
        } catch (Exception e) { /* ignore */ }
        
        // Criando o PDF
        
        switch (output) {
            case RTF_OUTPUT:
                RtfWriter.getInstance(doc, out);
                break;
                
            case XML_OUTPUT:
                XmlWriter.getInstance(doc, out);
                break;
                
            case HTML_OUTPUT:
                HtmlWriter.getInstance(doc, out);
                break;
                
            default:
                PdfWriter.getInstance(doc, out);
                break;
        }
                
        SAXParser parser = spf.newSAXParser();
        parser.parse(data, new SAXiTextHandler(doc));
         
        byte[] pdf = out.toByteArray();
        response.setContentType(CONTENT_TYPES[output]);
        response.setContentLength(pdf.length);
        dump(new ByteArrayInputStream(pdf), response);
                  
    }

}