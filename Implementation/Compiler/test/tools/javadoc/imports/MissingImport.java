/* 
 * Copyright 2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 5012972
 * @summary ClassDoc.getImportedClasses should return a class even if
 *	    it's not in the classpath.
 */

import com.sun.javadoc.*;


public class MissingImport extends Doclet {

    public static void main(String[] args) {
	String thisFile = "" +
	    new java.io.File(System.getProperty("test.src", "."),
			     "I.java");

	if (com.sun.tools.javadoc.Main.execute(
		"javadoc",
		"MissingImport",
		new String[] {thisFile}) != 0)
	    throw new Error("Javadoc encountered warnings or errors.");
    }

    /*
     * The world's simplest doclet.
     */
    public static boolean start(RootDoc root) {
	ClassDoc c = root.classNamed("I");
	ClassDoc[] imps = c.importedClasses();
	if (imps.length == 0 ||
	    !imps[0].qualifiedName().equals("bo.o.o.o.Gus")) {
	    throw new Error("Import bo.o.o.o.Gus not found");
	}
	return true;
    }
}
