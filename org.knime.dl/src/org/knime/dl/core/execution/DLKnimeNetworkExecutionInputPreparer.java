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
package org.knime.dl.core.execution;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.Queue;

import org.knime.core.data.DataRow;
import org.knime.dl.core.DLAbstractKnimeNetworkInputPreparer;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKnimeNetworkExecutionInputPreparer extends DLAbstractKnimeNetworkInputPreparer {

	private final boolean m_isPredefinedBatchSize;

	private final Queue<DataRow> m_baseRows;

	/**
	 * @param iterator provides the input data rows that are used by this instance to prepare (fill) the network tensors
	 *            fed to {@link #prepare(Map, long)}. The iterator must know its size. It must be in a proper initial
	 *            state (i.e. reset).
	 * @param batchSize the batch size of the tensors that will be prepared by this instance
	 * @param isPredefinedBatchSize true if the batch size is defined by the network specification (rather than by the
	 *            user). In this case, incomplete last batches will be zero-padded to match the expected batch size.
	 *            Otherwise false, in which case incomplete batches are not handled.
	 * @param converters the converters that are used to write the data rows into the tensors. The given tensor ids
	 *            determine the set of tensors supported by {@link #prepare(Map, long)}.
	 */
	public DLKnimeNetworkExecutionInputPreparer(final DLRowIterator iterator, final int batchSize,
			final boolean isPredefinedBatchSize,
			final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
		super(iterator, batchSize, converters);
		m_isPredefinedBatchSize = isPredefinedBatchSize;
		m_baseRows = new ArrayDeque<>(batchSize);
	}

    public OptionalLong getNumBatches() {
	    try {
	        return OptionalLong.of((long) Math.ceil(m_iterator.size() / (double) m_batchSize));
        } catch (final UnsupportedOperationException e) {
            // The iterator doens't know how long the table is
            return OptionalLong.empty();
	    }
	}

	public Queue<DataRow> getBaseRows() {
		return m_baseRows;
	}

    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    @Override
    public void prepareNext(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input)
        throws DLCanceledExecutionException {
		long i;
		for (i = 0; i < m_batchSize; i++) {
			if (!m_iterator.hasNext()) {
				// last batch will be incomplete, handled below
				break;
			}
			final DataRow row = m_iterator.next();
			m_baseRows.add(row);
			try {
				writeDataValuesInTensors(m_iterator.groupByTensor(row), input);
			} catch (final DLBufferOverflowExceptionForTensor e) {
				final DLTensor<?> tensor = e.getTensor();
				// must be present
				final long exampleSize = DLUtils.Shapes.getFixedSize(tensor.getSpec().getShape()).getAsLong();
				// must be present
				final long batchSize = tensor.getSpec().getBatchSize().getAsLong();
				throw new DLInvalidNetworkInputException(
						"Node input data size exceeds the expected size of network input '" + tensor.getSpec().getName()
								+ "'. Neuron count is " + exampleSize + ", batch size is " + batchSize
								+ ". Thus, expected input data size is " + exampleSize * batchSize
								+ ". Please check the column selection for this input "
								+ "and validate the node's input data.",
						e);
			}
		}
		// check if tensors were filled correctly
		for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
			final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
			final DLWritableBuffer buffer = tensor.getBuffer();
			final long expectedSize = tensor.getExampleSize() * m_batchSize;
			if (buffer.size() != expectedSize) {
				if (i < m_batchSize && buffer.size() / tensor.getExampleSize() == i) {
					// Last batch is incomplete but was correctly filled: if the batch size is pre-defined in the
					// network, we have to pad the input batch in order to adhere to the network's input specification.
					// Else, we ignore it - downstream code will have to make sure the incomplete batch is processed
					// properly.
					if (m_isPredefinedBatchSize) {
						buffer.zeroPad(expectedSize - buffer.size());
					}
				} else {
					// Must be present. Note that exampleSize == tensor.getExampleSize() does not necessarily hold
					// as the latter is expressed in terms of buffer elements, not input elements ("neurons").
					final long exampleSize = DLUtils.Shapes.getFixedSize(tensor.getSpec().getShape()).getAsLong();
					final long bufferSizeInNeurons = buffer.size() * (exampleSize / tensor.getExampleSize());
					throw new DLInvalidNetworkInputException(
							"Node input data size does not match the expected size of network input '"
									+ tensor.getSpec().getName() + "'. Neuron count is " + exampleSize
									+ ", batch size is " + m_batchSize + ". Thus, expected input size is "
									+ exampleSize * m_batchSize + ". However, node input data size is "
									+ bufferSizeInNeurons + ". Please check the column selection for this input "
									+ "and validate the node's input data.");
				}
			}
		}
	}

	@Override
	public void close() throws Exception {
		super.close();
		m_baseRows.clear();
	}
}
