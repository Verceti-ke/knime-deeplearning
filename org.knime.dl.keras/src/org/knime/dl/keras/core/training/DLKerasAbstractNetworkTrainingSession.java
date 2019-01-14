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
package org.knime.dl.keras.core.training;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLExecutionMonitorCancelable;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLNetworkFixedSizeInputPreparer;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.execution.DLKerasAbstractNetworkExecutionSession;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.training.DLPythonAbstractNetworkTrainingSession;

/**
 * Abstract base class for implementations of {@link DLKerasNetworkTrainingSession}.
 *
 * @param <N> the type of the {@link DLKerasNetwork network} to train
 * @param <C> the type of the {@link DLKerasAbstractCommands} that are used to control the training process on Python
 *            side
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractNetworkTrainingSession<N extends DLKerasNetwork, C extends DLKerasAbstractCommands>
	extends DLPythonAbstractNetworkTrainingSession<DLKerasTrainingStatus, N, DLKerasTrainingConfig, C>
		implements DLKerasNetworkTrainingSession {

	/**
	 * @param network the network to train
	 * @param trainingConfig the training configuration that specifies how the network will be trained
	 * @param executionInputSpecs a set of fully defined tensor specs. The set of tensor specs must exactly match the
	 *            network's input tensor specs with respect to the identifiers of the contained specs. A tensor spec is
	 *            fully defined if it features a non-empty batch size and a {@link DLFixedTensorShape fixed tensor
	 *            shape}.
	 * @param trainingInputPreparer the training data preparer
	 * @param validationInputPreparer the validation data preparer, may be null in which case no validation will be
	 *            performed during training
	 * @param tensorFactory the tensor factory that is used to create the network's input and target tensors
	 */
    protected DLKerasAbstractNetworkTrainingSession(final N network, final DLKerasTrainingConfig trainingConfig,
        final Set<DLTensorSpec> executionInputSpecs, final DLNetworkFixedSizeInputPreparer trainingInputPreparer,
        final DLNetworkFixedSizeInputPreparer validationInputPreparer, final DLTensorFactory tensorFactory) {
		super(network, trainingConfig, executionInputSpecs, trainingInputPreparer, validationInputPreparer,
				tensorFactory);
		boolean hasFixedBatchSizes = false;
		boolean hasVariableBatchSizes = false;
		for (final DLTensorSpec inputSpec : network.getSpec().getInputSpecs()) {
			if (inputSpec.getBatchSize().isPresent()) {
				hasFixedBatchSizes = true;
			} else {
				hasVariableBatchSizes = true;
			}
		}
		if (hasFixedBatchSizes && hasVariableBatchSizes) {
			NodeLogger.getLogger(DLKerasAbstractNetworkExecutionSession.class)
					.warn("Input network has both inputs with pre-defined batch size and variable batch size. "
							+ "This may not be supported by Keras and could lead to a runtime error.");
		}
	}

	@Override
	protected void setNetworkTrainingConfig(final DLPythonNetworkHandle handle, final DLKerasTrainingConfig config, final DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		m_commands.setNetworkTrainingConfig(handle, config, cancelable);
	}

	@Override
	public DLKerasNetworkPortObjectBase getTrainedNetwork(final ExecutionContext exec) throws Exception {
		if (m_commands == null) {
			throw new IllegalStateException("Network was not trained, yet.");
		}
		final DLCancelable cancelable = new DLExecutionMonitorCancelable(exec);
		final DLPythonNetworkLoader<? extends DLKerasNetwork> loader = DLPythonNetworkLoaderRegistry.getInstance()
				.getNetworkLoader(m_network.getClass()).get();
		final FileStore fileStore = DLNetworkPortObject.createFileStoreForSaving(loader.getSaveModelURLExtension(),
				exec);
        final URI fileStoreURI = fileStore.getFile().toURI();
        loader.save(m_handle, fileStoreURI, m_commands.getContext(cancelable), cancelable);
		if (!fileStore.getFile().exists()) {
			throw new IllegalStateException("Failed to save trained Keras deep learning network.");
		}
        return new DLKerasNetworkPortObject(
            loader.fetch(m_handle, new DLNetworkFileStoreLocation(fileStore), m_commands.getContext(cancelable), cancelable));
	}

    @Override
    public void stopEarly() throws DLCanceledExecutionException, Exception {
        if (m_commands == null) {
            throw new IllegalStateException("Network was not trained, yet.");
        }
        m_commands.stopTrainNetworkEarly(m_handle);
    }
}
