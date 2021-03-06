/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.keras.tensorflow;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.keras.core.DLKerasPythonContext;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkLoader;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.dl.util.DLUtils;
import org.knime.python2.testing.PreferencesSetup;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasTensorFlowNetworkReaderTest {

	private static final String BUNDLE_ID = "org.knime.dl.keras.tests";

    @ClassRule
    public static final TestRule preferencesSetup = new PreferencesSetup("org.knime.dl.keras.tests");

    private DLPythonContext m_context;

    @Before
    public void createContext() {
        m_context = new DLKerasPythonContext(DLPythonPreferences.getPythonKerasCommandPreference());
    }

    @After
    public void closeContext() {
        m_context.close();
    }

	@Test
    public void test() throws Exception {
		final URL source = FileUtil
				.toURL(DLUtils.Files.getFileFromBundle(BUNDLE_ID, "data/simple_test_model.h5").getAbsolutePath());
		final DLPythonDefaultNetworkReader<DLKerasTensorFlowNetwork> reader = new DLPythonDefaultNetworkReader<>(
				new DLKerasTensorFlowNetworkLoader());
        reader.read(new DLNetworkReferenceLocation(source.toURI()), true, m_context, DLNotCancelable.INSTANCE);
		// TODO: test against known specs
	}
}
