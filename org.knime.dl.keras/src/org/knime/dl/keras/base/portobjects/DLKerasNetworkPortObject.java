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
 */
package org.knime.dl.keras.base.portobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Objects;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.dl.base.portobjects.DLAbstractNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.python.core.DLPythonNetworkPortObject;

/**
 * Keras implementation of a deep learning {@link DLNetworkPortObject network port object}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkPortObject
	extends DLAbstractNetworkPortObject<DLKerasNetwork, DLKerasNetworkPortObjectSpec>
		implements DLPythonNetworkPortObject<DLKerasNetwork> {

	/**
	 * The Keras deep learning network port type.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DLKerasNetworkPortObject.class);

	private static final String ZIP_ENTRY_NAME = "DLKerasNetworkPortObject";

	private URL m_networkReference;

	/**
	 * Creates a new Keras deep learning network port object. The given network is stored in the given file store.
	 *
	 * @param network the Keras deep learning network to store
	 * @param fileStore the file store in which to store the network
	 * @throws IOException if failed to store the network
	 */
	public DLKerasNetworkPortObject(final DLKerasNetwork network, final FileStore fileStore) throws IOException {
		super(network, new DLKerasNetworkPortObjectSpec(network.getSpec(), network.getClass()), fileStore);
	}

	/**
	 * Creates a new Keras deep learning network port object. The port object only stores the given network's source URL
	 * and uses it as a reference for later loading.
	 *
	 * @param network the Keras deep learning network which source URL is stored
	 */
	public DLKerasNetworkPortObject(final DLKerasNetwork network) {
		super(network, new DLKerasNetworkPortObjectSpec(network.getSpec(), network.getClass()));
		m_networkReference = network.getSource();
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLKerasNetworkPortObject() {
		super();
	}

	@Override
	public String getSummary() {
		return "Keras Deep Learning Network";
	}

	@Override
	protected DLKerasNetwork getNetworkInternal(final DLKerasNetworkPortObjectSpec spec)
			throws DLInvalidSourceException, IOException {
		return spec.getNetworkSpec()
				.create(m_networkReference == null ? getFileStore(0).getFile().toURI().toURL() : m_networkReference);
	}

	@Override
	protected void flushToFileStoreInternal(final DLKerasNetwork network, final FileStore fileStore)
			throws IOException {
		DLNetworkPortObject.copyFileToFileStore(network.getSource(), fileStore);
	}

	@Override
	protected void hashCodeInternal(final HashCodeBuilder b) {
		b.append(m_networkReference);
	}

	@Override
	protected boolean equalsInternal(final DLNetworkPortObject other) {
		return Objects.equals(((DLKerasNetworkPortObject) other).m_networkReference, m_networkReference);
	}

	/**
	 * Serializer of {@link DLKerasNetworkPortObject}.
	 */
	public static final class Serializer extends PortObjectSerializer<DLKerasNetworkPortObject> {

		@Override
		public void savePortObject(final DLKerasNetworkPortObject portObject, final PortObjectZipOutputStream out,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
			final ObjectOutputStream objOut = new ObjectOutputStream(out);
			final boolean storedInFileStore = portObject.m_networkReference != null;
			objOut.writeBoolean(storedInFileStore);
			if (storedInFileStore) {
				objOut.writeObject(portObject.m_networkReference);
			}
			objOut.flush();
		}

		@Override
		public DLKerasNetworkPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			final DLKerasNetworkPortObject portObject = new DLKerasNetworkPortObject();
			final ZipEntry entry = in.getNextEntry();
			if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
				throw new IOException("Failed to load Keras deep learning network port object. Invalid zip entry name '"
						+ entry.getName() + "', expected '" + ZIP_ENTRY_NAME + "'.");
			}
			final ObjectInputStream objIn = new ObjectInputStream(in);
			if (objIn.readBoolean()) {
				try {
					portObject.m_networkReference = (URL) objIn.readObject();
				} catch (final ClassNotFoundException e) {
					throw new IOException("Failed to load Keras deep learning network port object.", e);
				}
			} else {
				portObject.m_networkReference = null;
			}
			portObject.m_spec = (DLKerasNetworkPortObjectSpec) spec;
			return portObject;
		}
	}
}