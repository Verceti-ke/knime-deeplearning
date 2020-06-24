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
 */
package org.knime.dl.python.core;

import java.io.IOException;

import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonDefaultNetworkReader<N extends DLPythonNetwork> {

	private final DLPythonNetworkLoader<N> m_loader;

	public DLPythonDefaultNetworkReader(final DLPythonNetworkLoader<N> loader) {
		m_loader = loader;
	}

	/**
	 * @deprecated Use {@link #read(DLNetworkLocation, boolean, DLPythonContext, DLCancelable)}.
	 */
	@Deprecated
    @SuppressWarnings("javadoc")
    public N read(final DLNetworkLocation source, final boolean loadTrainingConfig, final DLCancelable cancelable)
			throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        m_loader.validateSource(source.getURI()); // fail fast - spares us creating the Python kernel
        try (final DLPythonContext context = new DLPythonDefaultContext()) {
            final DLPythonNetworkHandle handle =
                m_loader.load(source.getURI(), context, loadTrainingConfig, cancelable);
			return m_loader.fetch(handle, source, context, cancelable);
		}
	}

	/**
	 * @deprecated Use {@link #read(DLPythonNetwork, boolean, DLPythonContext, DLCancelable)}.
	 */
    @Deprecated
    @SuppressWarnings("javadoc")
    public N read(final N network, final boolean loadTrainingConfig, final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        m_loader.validateSource(network.getSource().getURI()); // fail fast - spares us creating the Python kernel
        try (final DLPythonContext context = new DLPythonDefaultContext()) {
            final DLPythonNetworkHandle handle = m_loader.load(network, context, loadTrainingConfig, cancelable);
            return m_loader.fetch(handle, network.getSource(), context, cancelable);
        }
    }

    /**
     * Read the network from the given source.
     *
     * @param source the network source
     * @param loadTrainingConfig if a training configuration should be read
     * @param context the Python context to use
     * @param cancelable to check if the execution was canceled
     * @return the network
     * @throws DLInvalidSourceException if the source is invalid
     * @throws DLInvalidEnvironmentException if the context is invalid
     * @throws IOException if reading the network fails
     * @throws DLCanceledExecutionException if the execution is canceled
     */
    public N read(final DLNetworkLocation source, final boolean loadTrainingConfig, final DLPythonContext context,
        final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        m_loader.validateSource(source.getURI()); // fail fast - spares us creating the Python kernel
        final DLPythonNetworkHandle handle = m_loader.load(source.getURI(), context, loadTrainingConfig, cancelable);
        return m_loader.fetch(handle, source, context, cancelable);
    }

    /**
     * Read the network from the given network object.
     *
     * @param network the network
     * @param loadTrainingConfig if a training configuration should be read
     * @param context the Python context to use
     * @param cancelable to check if the execution was canceled
     * @return the network
     * @throws DLInvalidSourceException if the source is invalid
     * @throws DLInvalidEnvironmentException if the context is invalid
     * @throws IOException if reading the network fails
     * @throws DLCanceledExecutionException if the execution is canceled
     */
    public N read(final N network, final boolean loadTrainingConfig, final DLPythonContext context, final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        m_loader.validateSource(network.getSource().getURI()); // fail fast - spares us creating the Python kernel
        final DLPythonNetworkHandle handle = m_loader.load(network, context, loadTrainingConfig, cancelable);
        return m_loader.fetch(handle, network.getSource(), context, cancelable);
    }
}
