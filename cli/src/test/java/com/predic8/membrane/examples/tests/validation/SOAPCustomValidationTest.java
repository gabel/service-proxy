/* Copyright 2012 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package com.predic8.membrane.examples.tests.validation;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.predic8.membrane.examples.AbstractConsoleWatcher;
import com.predic8.membrane.examples.DistributionExtractingTestcase;
import com.predic8.membrane.examples.Process2;
import com.predic8.membrane.examples.util.ConsoleLogger;

public class SOAPCustomValidationTest extends DistributionExtractingTestcase {
	
	@Test
	public void test() throws IOException, InterruptedException {
		File baseDir = getExampleDir("validation" + File.separator + "soap-custom");
		Process2 sl = new Process2.Builder().in(baseDir).script("router").waitForMembrane().start();
		try {
			File buildXML = new File(baseDir, "build.xml");

			// remove <exec...</exec> from build.xml
			String s = Pattern.compile("<exec.*</exec>", Pattern.DOTALL).matcher(FileUtils.readFileToString(buildXML)).replaceAll("");
			FileUtils.writeStringToFile(buildXML, s);

			File classes = new File(baseDir, "build" + File.separator + "classes");
			classes.mkdirs();
			File sources = new File(baseDir, "src");
			sources.mkdirs();

			// run "wsimport" generating java sources
			Assert.assertTrue(new com.sun.tools.ws.wscompile.WsimportTool(System.out).run(new String[] {
					"-quiet",
					"-Xnocompile",
					"http://www.predic8.com:8080/material/ArticleService?wsdl",
					"-s",
					sources.getAbsolutePath()
			}));
			
			assertAntRunProducesException(baseDir, true);
			
			File source = new File(baseDir, "src" + File.separator + "ArticleClient.java");
			FileUtils.writeStringToFile(source, readFileToString(source).replace("//aType", "aType"));
			
			assertAntRunProducesException(baseDir, false);
		} finally {
			sl.killScript();
		}
	}

	private void assertAntRunProducesException(File baseDir, boolean expectException) throws IOException, InterruptedException {
		final boolean[] exception = new boolean[1];
		Process2 ant = new Process2.Builder().
				in(baseDir).
				executable("ant run").
				withWatcher(new ConsoleLogger()).
				withWatcher(new AbstractConsoleWatcher() {
					@Override
					public void outputLine(boolean error, String line) {
						if (line.contains("ClientTransportException"))
							exception[0] = true;
					}
				}).
				start();
		try {
			ant.waitFor(30000);
		} finally {
			ant.killScript();
		}
		Assert.assertEquals(expectException, exception[0]);
	}

}