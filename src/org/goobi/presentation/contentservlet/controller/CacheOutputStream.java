/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 *         - http://gdz.sub.uni-goettingen.de 
 *         - http://www.intranda.com 
 *         - http://www.digiverso.com
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software
 *
 * This is the extended version updated by intranda
 * Copyright 2012, intranda GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goobi.presentation.contentservlet.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/************************************************************************************
 * CacheOutputStream enhanced the {@link FileOutputStream} to write to a second {@link OutputStream}
 * 
 * @version 15.01.2009
 * @author Steffen Hankiewicz
 * @author Igor Toker
 * @author Christian Mahnke
 ************************************************************************************/
public class CacheOutputStream extends FileOutputStream {
    private OutputStream os;

    /*************************************************************************************
     * Constructor for {@link CacheOutputStream}
     * 
     * @param file the File object
     * @param out the second OutputStream where to write (eg. OutputStream of servlet)
     * @throws FileNotFoundException
     ************************************************************************************/
    public CacheOutputStream(File file, OutputStream out) throws FileNotFoundException {
        super(file.getAbsolutePath());
        this.os = out;
    }

    /*************************************************************************************
     * Constructor for {@link CacheOutputStream}
     * 
     * @param name the system-dependent filename
     * @param out the second OutputStream where to write (eg. OutputStream of servlet)
     * @throws FileNotFoundException
     ************************************************************************************/
    public CacheOutputStream(String name, OutputStream out) throws FileNotFoundException {
        super(name);
        this.os = out;
    }

    /*************************************************************************************
     * overridden write method to split the stream to a second OutputStream
     * 
     * @param name the system-dependent filename
     * @param out the second OutputStream where to write (eg. OutputStream of servlet)
     ************************************************************************************/
    @Override
    public void write(byte[] b) throws IOException {

        this.os.write(b);
        super.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        this.os.write(b);
        super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.os.write(b, off, len);
        super.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.os.flush();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        this.os.close();
        super.close();
    }
}
