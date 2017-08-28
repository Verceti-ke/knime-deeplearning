/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   May 10, 2017 (marcel): created
 */
package org.knime.dl.keras.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.kernel.DLPythonCommands;
import org.knime.python2.kernel.PythonKernel;

/**
 * Reads in a {@link DLKerasDefaultNetwork Keras network} from a URL.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasDefaultNetworkReader implements DLKerasNetworkReader {

	@Override
	public DLKerasNetworkType getNetworkType() {
		return DLKerasNetworkType.INSTANCE;
	}

	@Override
	public DLKerasNetwork create(final URL source) throws DLInvalidSourceException, IOException {
		validateSource(source);
		final PythonKernel kernel;
		try {
			kernel = DLPythonCommands.createKernel();
		} catch (final IOException e) {
			throw new IOException("Connection to Keras could not be established. "
					+ "An exception occurred while setting up the Python kernel.", e);
		}
		try (DLKerasPythonCommands commands = new DLKerasPythonCommands(kernel)) {
			final DLPythonNetworkHandle networkHandle = load(source, commands);
			final DLKerasNetworkSpec networkSpec = commands.extractNetworkSpec(networkHandle, DLNumPyTypeMap.INSTANCE);
			return new DLKerasDefaultNetwork(source, networkSpec);
		} catch (final Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public DLKerasNetwork create(final URL source, final DLKerasNetworkSpec spec)
			throws DLInvalidSourceException, IllegalArgumentException, IOException {
		validateSource(source);
		return new DLKerasDefaultNetwork(source, checkNotNull(spec));
	}

	/**
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 */
	public static void validateSource(final Object source) throws DLInvalidSourceException {
		if (!(source instanceof URL)) {
			throw new DLInvalidSourceException("Source is not a valid Keras model file: '" + source.toString() + "'.");
		}
		final File sourceFile = FileUtil.getFileFromURL((URL) source);
		if (sourceFile == null || !sourceFile.exists()) {
			throw new DLInvalidSourceException("Cannot find Keras model file at location '" + source.toString() + "'.");
		}
	}

	/**
	 * @throws DLInvalidDestinationException if the destination is invalid
	 */
	public static void validateDestination(final Object destination) throws DLInvalidDestinationException {
		if (!(destination instanceof URL)) {
			throw new DLInvalidDestinationException("Invalid destination: '" + destination.toString() + "'.");
		}
		final File destinationFile = FileUtil.getFileFromURL((URL) destination);
		if (destinationFile == null || destinationFile.exists()) {
			throw new DLInvalidDestinationException(
					"Invalid destination or destination already exists: '" + destination.toString() + "'.");
		}
	}

	/**
	 * @throws DLInvalidSourceException if source is unavailable or invalid
	 * @throws IllegalArgumentException if commands is invalid
	 * @throws IOException if failed to load the network handle
	 */
	public static DLPythonNetworkHandle load(final URL source, final DLKerasPythonCommands commands)
			throws DLInvalidSourceException, IllegalArgumentException, IOException {
		validateSource(source);
		checkNotNull(commands);
		File sourceFile;
		try {
			sourceFile = FileUtil.getFileFromURL(source);
		} catch (final Exception e) {
			throw new DLInvalidSourceException(
					"Invalid network source URL '" + source.toString() + "'. URL could not be resolved.");
		}
		final String filePath = sourceFile.getAbsolutePath();
		try {
			final String fileExtension = FilenameUtils.getExtension(filePath);
			final DLPythonNetworkHandle networkHandle;
			if (fileExtension.equals("h5")) {
				networkHandle = commands.loadNetwork(filePath);
			} else if (fileExtension.equals("json")) {
				networkHandle = commands.loadNetworkFromJson(filePath);
			} else if (fileExtension.equals("yaml")) {
				networkHandle = commands.loadNetworkFromYaml(filePath);
			} else {
				throw new DLInvalidSourceException(
						"Keras network reader only supports files of type h5, json and yaml.");
			}
			return networkHandle;
		} catch (final Exception e) {
			throw new IOException("An exception occurred while reading in the Keras network.", e);
		}
	}
}
